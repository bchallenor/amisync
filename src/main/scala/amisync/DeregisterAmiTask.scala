package amisync

import com.amazonaws.services.ec2.model.DeregisterImageRequest

case class DeregisterAmiTask(amiId: AmiId) extends LeafTask {
  override def run(config: Config): Nil.type = {
    import config._
    ec2.deregisterImage({
      val req = new DeregisterImageRequest
      req.setImageId(amiId.id)
      req
    })
    Nil
  }
}
