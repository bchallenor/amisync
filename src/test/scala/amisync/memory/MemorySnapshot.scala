package amisync.memory

import amisync._
import com.amazonaws.services.ec2.model.Snapshot

case class MemorySnapshot(
  id: SnapshotId,
  description: String,
  encrypted: Boolean,
  var progress: Int
) {
  def toModel: Snapshot = {
    new Snapshot()
      .withSnapshotId(id.id)
      .withDescription(description)
      .withEncrypted(encrypted)
      .withProgress(s"${progress.toString}%")
      .withState(if (progress == 100) "completed" else "pending")
  }
}
