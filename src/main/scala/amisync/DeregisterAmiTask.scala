package amisync

import com.amazonaws.services.ec2.model.DeregisterImageRequest

case class DeregisterAmiTask(amiId: AmiId) extends LeafTask {
  override def run(ctx: Context): Nil.type = {
    import ctx._
    ec2.deregisterImage({
      val req = new DeregisterImageRequest
      req.setImageId(amiId.id)
      req
    })
    Nil
  }
}
