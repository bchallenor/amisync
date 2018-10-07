package amisync.memory

import amisync._
import com.amazonaws.services.ec2.model.{BlockDeviceMapping, EbsBlockDevice, Image, ImageState}

import scala.collection.JavaConverters._

case class MemoryAmi(
  id: AmiId,
  name: AmiName,
  description: String,
  snapshotId: SnapshotId
) {
  def ownerId: String = "self"
  def state: ImageState = ImageState.Available

  def toModel: Image = {
    new Image()
      .withImageId(id.id)
      .withName(name.name)
      .withDescription(description)
      .withOwnerId(ownerId)
      .withState(state.toString)
      .withRootDeviceName("/dev/xvda")
      .withBlockDeviceMappings(List(
        new BlockDeviceMapping()
          .withDeviceName("/dev/xvda")
          .withEbs(
            new EbsBlockDevice()
              .withSnapshotId(snapshotId.id)
          )
      ).asJava)
  }
}
