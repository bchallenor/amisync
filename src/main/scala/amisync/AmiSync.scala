package amisync

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.ec2.model.{DescribeImagesRequest, Filter, Image}
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.immutable.Queue
import scala.concurrent.duration._

object AmiSync {
  def main(args: Array[String]): Unit = {
    val (vmImportRoleName, bucket, keyPrefix) = args match {
      case Array(p1, p2, p3) => (RoleName(p1), Bucket(p2), KeyPrefix(p3))
      case _                 => sys.exit(1)
    }
    val ctx = buildContext(vmImportRoleName)
    val taskQueues = buildSyncImageTaskQueues(ctx, bucket, keyPrefix)
    taskQueues.par.foreach { taskQueue =>
      println(s"Task queue: $taskQueue")
      runTasks(taskQueue, ctx)
    }
  }

  private def buildContext(vmImportRoleName1: RoleName): Context = {
    new Context {
      override lazy val regionName: RegionName = {
        val chain = new DefaultAwsRegionProviderChain
        RegionName(chain.getRegion)
      }

      override lazy val vmImportRoleName: RoleName = vmImportRoleName1

      override lazy val s3: AmazonS3 = {
        val builder = AmazonS3ClientBuilder.standard()
        builder.setRegion(regionName.name)
        builder.build()
      }

      override lazy val ec2: AmazonEC2 = {
        val builder = AmazonEC2ClientBuilder.standard()
        builder.setRegion(regionName.name)
        builder.build()
      }
    }
  }

  private def buildSyncImageTaskQueues(ctx: Context, bucket: Bucket, keyPrefix: KeyPrefix): Set[Queue[Task]] = {
    val importable = findImportableImages(ctx.s3, bucket, keyPrefix)
    val imported = findImportedImages(ctx.ec2)

    val extra = (imported -- importable.keySet).iterator.map { case (ami, image) =>
      val rootDeviceMapping = image
        .getBlockDeviceMappings
        .asScala
        .find(_.getDeviceName == image.getRootDeviceName)
        .getOrElse(sys.error(s"Missing root device mapping: $image"))

      Queue(
        DeregisterAmiTask(AmiId(image.getImageId)),
        DeleteSnapshotTask(SnapshotId(rootDeviceMapping.getEbs.getSnapshotId))
      )
    }.toSet

    val missing = (importable -- imported.keySet).iterator.map { case (ami, obj) =>
      Queue(
        ImportAmiFromS3Task(ami, bucket, Key(obj.getKey))
      )
    }.toSet

    extra ++ missing
  }

  private def findImportableImages(s3: AmazonS3, bucket: Bucket, keyPrefix: KeyPrefix): Map[AmiName, S3ObjectSummary] = {
    val res = s3.listObjects(bucket.name, keyPrefix.name)
    assert(!res.isTruncated, s"Truncated: $res")
    res.getObjectSummaries.iterator.asScala.map(obj => {
      deriveAmiName(obj) -> obj
    }).toMap
  }

  private def findImportedImages(ec2: AmazonEC2): Map[AmiName, Image] = {
    val res = ec2.describeImages({
      val req = new DescribeImagesRequest
      req.setOwners(Set("self").asJava)
      req.setFilters(Set({
        val filter = new Filter
        filter.setName("state")
        filter.setValues(Set("available", "pending").asJava)
        filter
      }).asJava)
      req
    })
    res.getImages.iterator.asScala.map(image => {
      AmiName(image.getName) -> image
    }).toMap
  }

  private def deriveAmiName(obj: S3ObjectSummary): AmiName = {
    val key = obj.getKey
    val rootName = key.split("/").last.split("\\.").head
    val lastModified = obj.getLastModified.toInstant.atOffset(ZoneOffset.UTC)
    val datePattern = DateTimeFormatter.ofPattern("yyyyMMdd")
    val timePattern = DateTimeFormatter.ofPattern("HHmmss")
    val name = s"$rootName-${lastModified.format(datePattern)}-${lastModified.format(timePattern)}"
    AmiName(name)
  }

  @tailrec
  private def runTasks(tasks: Queue[Task], ctx: Context): Unit = {
    tasks.dequeueOption match {
      case Some((head, tail)) =>
        println(s"Running task: $head")
        val next = head.run(ctx)
        val maybeDelayTask = if (next == List(head)) {
          Queue(new LeafTask {
            override def run(ctx: Context): Nil.type = {
              Thread.sleep(1.seconds.toMillis)
              Nil
            }
          })
        } else Queue()
        runTasks(maybeDelayTask ++ next ++ tail, ctx)
      case None =>
    }
  }
}
