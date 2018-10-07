package amisync

import com.amazonaws.services.ec2.model._

import scala.collection.JavaConverters._

case class FindAmiRootSnapshotTask(amiId: AmiId, k: SnapshotId => Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.describeImages({
      val req = new DescribeImagesRequest
      req.setImageIds(Set(amiId.id).asJava)
      req
    })
    res.getImages.asScala.headOption.map { image =>
      val rootSnapshotId = image
        .getBlockDeviceMappings
        .iterator
        .asScala
        .find(device => device.getDeviceName == image.getRootDeviceName)
        .map(device => SnapshotId(device.getEbs.getSnapshotId))
        .getOrElse(sys.error(s"Could not find root device: $image"))

      k(rootSnapshotId)
    }.getOrElse(Set.empty[Task])
  }
}
