package amisync

import com.amazonaws.services.ec2.model.CopySnapshotRequest

case class ImportAmiFromSnapshotTask(amiName: AmiName, snapshotId: SnapshotId) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.copySnapshot({
      val req = new CopySnapshotRequest
      req.setDescription(amiName.name)
      req.setEncrypted(true)
      req.setSourceRegion(regionName.name)
      req.setSourceSnapshotId(snapshotId.id)
      req
    })
    Set(
      WaitForCopySnapshotTask(
        SnapshotId(res.getSnapshotId),
        encryptedSnapshotId => Set(
          DeleteSnapshotTask(snapshotId),
          RegisterAmiTask(amiName, encryptedSnapshotId)
        )
      )
    )
  }
}
