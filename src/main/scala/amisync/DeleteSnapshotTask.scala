package amisync

import com.amazonaws.services.ec2.model.DeleteSnapshotRequest

case class DeleteSnapshotTask(snapshotId: SnapshotId) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    ec2.deleteSnapshot({
      val req = new DeleteSnapshotRequest()
      req.setSnapshotId(snapshotId.id)
      req
    })
    Set.empty
  }
}
