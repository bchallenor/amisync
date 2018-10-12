package amisync.memory

import java.util.concurrent.atomic.AtomicInteger

import amisync._
import com.amazonaws.services.ec2.AbstractAmazonEC2
import com.amazonaws.services.ec2.model._

import scala.collection.JavaConverters._

case class MemoryEC2(
  var importTasks: Map[ImportTaskId, MemoryImportTask] = Map.empty,
  var snapshots: Map[SnapshotId, MemorySnapshot] = Map.empty,
  var amis: Map[AmiId, MemoryAmi] = Map.empty
) extends AbstractAmazonEC2 {
  private val nextImportTaskId = {
    val count = new AtomicInteger(1)
    () => ImportTaskId(f"import-snap-${count.getAndIncrement}%03d")
  }
  private var nextSnapshotId = {
    val count = new AtomicInteger(1)
    () => SnapshotId(f"snap-${count.getAndIncrement}%03d")
  }
  private var nextAmiId = {
    val count = new AtomicInteger(1)
    () => AmiId(f"ami-${count.getAndIncrement}%03d")
  }

  override def importSnapshot(req: ImportSnapshotRequest): ImportSnapshotResult = {
    val id = nextImportTaskId()
    val bucket = Bucket(req.getDiskContainer.getUserBucket.getS3Bucket)
    val key = Key(req.getDiskContainer.getUserBucket.getS3Key)

    importTasks += id -> MemoryImportTask(id, bucket, key, progress = 0, snapshotId = None)

    new ImportSnapshotResult()
      .withImportTaskId(id.id)
  }

  override def describeImportSnapshotTasks(req: DescribeImportSnapshotTasksRequest): DescribeImportSnapshotTasksResult = {
    require(req.getImportTaskIds.size == 1)
    require(req.getFilters.size == 0)

    val importTaskId = ImportTaskId(req.getImportTaskIds.get(0))
    val importTask = importTasks(importTaskId)

    if (importTask.progress < 100) {
      importTask.progress = Math.min(importTask.progress + 25, 100)
      if (importTask.progress == 100) {
        val snapshot = MemorySnapshot(
          nextSnapshotId(),
          description = "vmimport",
          encrypted = false,
          progress = 100
        )
        importTask.snapshotId = Some(snapshot.id)
        snapshots += snapshot.id -> snapshot
        importTasks -= importTaskId
      }
    }

    new DescribeImportSnapshotTasksResult()
      .withImportSnapshotTasks(
        List(importTask.toModel).asJava
      )
  }

  override def describeSnapshots(req: DescribeSnapshotsRequest): DescribeSnapshotsResult = {
    require(req.getSnapshotIds.size == 1)
    require(req.getFilters.size == 0)

    val snapshotId = SnapshotId(req.getSnapshotIds.get(0))
    val snapshot = snapshots(snapshotId)

    if (snapshot.progress < 100) {
      snapshot.progress = Math.min(snapshot.progress + 25, 100)
    }

    new DescribeSnapshotsResult()
      .withSnapshots(
        List(snapshot.toModel).asJava
      )
  }

  override def copySnapshot(req: CopySnapshotRequest): CopySnapshotResult = {
    require(req.getSourceRegion ne null)
    require(req.getSourceSnapshotId ne null)

    val id = nextSnapshotId()
    val description = req.getDescription
    val encrypted = req.getEncrypted

    snapshots += id -> MemorySnapshot(id, description, encrypted, progress = 0)

    new CopySnapshotResult()
      .withSnapshotId(id.id)
  }

  override def deleteSnapshot(req: DeleteSnapshotRequest): DeleteSnapshotResult = {
    val snapshotId = SnapshotId(req.getSnapshotId)
    require(snapshots.contains(snapshotId))
    require(!amis.values.exists(_.snapshotId == snapshotId))

    // TODO: should it instead stay in the map, but with a different state?
    snapshots -= snapshotId

    new DeleteSnapshotResult()
  }

  override def describeImages(req: DescribeImagesRequest): DescribeImagesResult = {
    val reqImageIds: Option[Set[AmiId]] = if (req.getImageIds.isEmpty) None else Some {
      req.getImageIds.iterator.asScala.map(AmiId).toSet
    }

    val reqOwners: Option[Set[String]] = if (req.getOwners.isEmpty) None else Some {
      req.getOwners.iterator.asScala.toSet
    }

    val reqFilters: List[MemoryAmi => Boolean] = req.getFilters.iterator().asScala
      .map(filter => filter.getName match {
        case "state" => (ami: MemoryAmi) => filter.getValues.contains(ami.state.toString)
        case "name" => (ami: MemoryAmi) => filter.getValues.contains(ami.name.name)
      })
      .toList

    val matchingImages: List[MemoryAmi] = amis.valuesIterator
      .filter(ami => reqImageIds.forall(_.contains(ami.id)))
      .filter(ami => reqOwners.forall(_.contains(ami.ownerId)))
      .filter(ami => reqFilters.forall(filter => filter(ami)))
      .toList

    new DescribeImagesResult()
      .withImages(matchingImages.map(_.toModel).asJava)
  }

  override def registerImage(req: RegisterImageRequest): RegisterImageResult = {
    val id = nextAmiId()
    val name = AmiName(req.getName)
    val description = req.getDescription
    val snapshotId = SnapshotId(req.getBlockDeviceMappings.get(0).getEbs.getSnapshotId)
    require(snapshots(snapshotId).progress == 100)

    amis += id -> MemoryAmi(id, name, description, snapshotId)

    new RegisterImageResult()
      .withImageId(id.id)
  }


  override def deregisterImage(req: DeregisterImageRequest): DeregisterImageResult = {
    val amiId = AmiId(req.getImageId)
    require(amis.contains(amiId))

    // TODO: should it instead stay in the map, but with a different state?
    amis -= amiId

    new DeregisterImageResult()
  }
}
