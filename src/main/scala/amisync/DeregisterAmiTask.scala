package amisync

import com.amazonaws.services.ec2.model.DeregisterImageRequest

import scala.collection.immutable.Queue

case class DeregisterAmiTask(amiId: AmiId) extends Task {
  override def run(config: Config): Queue[Task] = {
    import config._
    ec2.deregisterImage({
      val req = new DeregisterImageRequest
      req.setImageId(amiId.id)
      req
    })
    Queue.empty
  }
}
