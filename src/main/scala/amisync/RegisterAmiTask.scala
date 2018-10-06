package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.{BlockDeviceMapping, EbsBlockDevice, RegisterImageRequest}

import scala.collection.immutable.Queue

case class RegisterAmiTask(amiName: AmiName, snapshotId: SnapshotId) extends Task {
  override def run(config: Config): Queue[Task] = {
    import config._
    ec2.registerImage({
      val req = new RegisterImageRequest
      req.setName(amiName.name)
      req.setDescription(amiName.name)
      req.setArchitecture("x86_64")
      req.setVirtualizationType("hvm")
      req.setRootDeviceName("/dev/xvda")
      req.setBlockDeviceMappings(Collections.singletonList({
        val mapping = new BlockDeviceMapping
        mapping.setDeviceName("/dev/xvda")
        mapping.setEbs({
          val ebs = new EbsBlockDevice
          ebs.setSnapshotId(snapshotId.id)
          ebs.setVolumeType("gp2")
          ebs
        })
        mapping
      }))
      req.setEnaSupport(true)
      req
    })
    Queue.empty
  }
}
