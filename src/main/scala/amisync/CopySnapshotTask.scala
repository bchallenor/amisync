package amisync

import com.amazonaws.services.ec2.model.CopySnapshotRequest

case class CopySnapshotTask(snapshotId: SnapshotId, description: String, encrypt: Boolean, k: SnapshotId => Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.copySnapshot({
      val req = new CopySnapshotRequest
      req.setDescription(description)
      req.setEncrypted(encrypt)
      req.setSourceRegion(regionName.name)
      req.setSourceSnapshotId(snapshotId.id)
      req
    })
    Set(
      WaitForCopySnapshotTask(SnapshotId(res.getSnapshotId), k)
    )
  }
}
