package amisync

import com.amazonaws.services.ec2.model.DeregisterImageRequest

case class DeregisterAmiTask(amiId: AmiId) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    ec2.deregisterImage({
      val req = new DeregisterImageRequest
      req.setImageId(amiId.id)
      req
    })
    Set.empty
  }
}
