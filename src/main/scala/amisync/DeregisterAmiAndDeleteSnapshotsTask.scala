package amisync

import com.amazonaws.services.ec2.model._

import scala.collection.JavaConverters._

case class DeregisterAmiAndDeleteSnapshotsTask(amiId: AmiId) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.describeImages({
      val req = new DescribeImagesRequest
      req.setImageIds(Set(amiId.id).asJava)
      req
    })
    res.getImages.asScala.headOption.map { image =>
      val deleteSnapshotTasks: Set[Task] = image
        .getBlockDeviceMappings
        .iterator
        .asScala
        .map(device => DeleteSnapshotTask(SnapshotId(device.getEbs.getSnapshotId)))
        .toSet

      Set[Task](DeregisterAmiTask(amiId, deleteSnapshotTasks))
    }.getOrElse(Set.empty[Task])
  }
}
