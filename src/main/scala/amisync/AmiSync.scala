package amisync

import amisync.json._
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.{DescribeImagesRequest, Filter, Image}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3ObjectSummary
import spray.json._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.immutable.Queue

object AmiSync {
  def main(args: Array[String]): Unit = {
    val (bucket, keyPrefix) = args match {
      case Array(p1, p2) => (Bucket(p1), KeyPrefix(p2))
      case _             => sys.exit(1)
    }
    val config = Config.default
    val taskQueues = buildSyncImageTaskQueues(config, bucket, keyPrefix)
    taskQueues.par.foreach { taskQueue =>
      println(s"Task queue: ${taskQueue.toJson.prettyPrint}")
      runTasks(taskQueue, config)
    }
  }

  private def buildSyncImageTaskQueues(config: Config, bucket: Bucket, keyPrefix: KeyPrefix): Set[Queue[Task]] = {
    val importable = findImportableImages(config.s3, bucket, keyPrefix)
    val imported = findImportedImages(config.ec2)

    val extra = (imported -- importable.keySet).iterator.map { case (ami, image) =>
      Queue(
        DeregisterAmiAndDeleteSnapshotsTask(AmiId(image.getImageId))
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
      AmiName.deriveFrom(Key(obj.getKey)) -> obj
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

  @tailrec
  private def runTasks(tasks: Queue[Task], config: Config): Unit = {
    tasks.dequeueOption match {
      case Some((head, tail)) =>
        println(s"Running task: ${head.toJson.prettyPrint}")
        val next = head.run(config)
        runTasks(tail ++ next, config)
      case None =>
    }
  }
}
