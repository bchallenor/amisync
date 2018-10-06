package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest

import scala.collection.immutable.Queue

case class WaitForCopySnapshotTask(snapshotId: SnapshotId) extends Task {
  override def run(config: Config): Queue[Task] = {
    import config._
    val res = ec2.describeSnapshots({
      val req = new DescribeSnapshotsRequest()
      req.setSnapshotIds(Collections.singleton(snapshotId.id))
      req
    })
    val detail = res.getSnapshots.get(0)
    detail.getState match {
      case "pending" =>
        println(s"Waiting for $snapshotId to be copied (${detail.getProgress})")
        Queue(this)

      case "completed" =>
        Queue.empty

      case state =>
        throw new IllegalStateException(s"Unknown snapshot state: $state")
    }
  }
}
