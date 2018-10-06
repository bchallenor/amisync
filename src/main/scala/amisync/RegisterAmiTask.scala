package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.{BlockDeviceMapping, EbsBlockDevice, RegisterImageRequest}

case class RegisterAmiTask(amiName: AmiName, snapshotId: SnapshotId) extends LeafTask {
  override def run(ctx: Context): Nil.type = {
    import ctx._
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
    Nil
  }
}
