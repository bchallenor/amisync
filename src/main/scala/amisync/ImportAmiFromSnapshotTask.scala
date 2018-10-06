package amisync

import com.amazonaws.services.ec2.model.CopySnapshotRequest

import scala.collection.immutable.Queue

case class ImportAmiFromSnapshotTask(amiName: AmiName, snapshotId: SnapshotId) extends Task {
  override def run(config: Config): Queue[Task] = {
    import config._
    val res = ec2.copySnapshot({
      val req = new CopySnapshotRequest
      req.setDescription(amiName.name)
      req.setEncrypted(true)
      req.setSourceRegion(regionName.name)
      req.setSourceSnapshotId(snapshotId.id)
      req
    })
    val encryptedSnapshotId = SnapshotId(res.getSnapshotId)
    Queue(
      WaitForCopySnapshotTask(encryptedSnapshotId),
      DeleteSnapshotTask(snapshotId),
      RegisterAmiTask(amiName, encryptedSnapshotId)
    )
  }
}
