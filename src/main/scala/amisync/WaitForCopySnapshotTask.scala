package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest

case class WaitForCopySnapshotTask(snapshotId: SnapshotId) extends Task {
  override def run(ctx: Context): List[Task] = {
    import ctx._
    val res = ec2.describeSnapshots({
      val req = new DescribeSnapshotsRequest()
      req.setSnapshotIds(Collections.singleton(snapshotId.id))
      req
    })
    val detail = res.getSnapshots.get(0)
    detail.getState match {
      case "pending" =>
        println(s"Waiting for snapshot ${detail.getSnapshotId} to be copied (${detail.getProgress})")
        List(this)

      case "completed" =>
        Nil

      case state =>
        throw new IllegalStateException(s"Unknown snapshot state: $state")
    }
  }
}
