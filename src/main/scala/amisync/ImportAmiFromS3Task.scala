package amisync

case class ImportAmiFromS3Task(amiName: AmiName, bucket: Bucket, key: Key) extends Task {
  override def run(config: Config): Set[Task] = {
    val description = amiName.name
    val encrypt = true
    Set(
      ImportSnapshotTask(bucket, key, importedSnapshotId => Set(
        CopySnapshotTask(importedSnapshotId, description, encrypt, encryptedSnapshotId => Set(
          DeleteSnapshotTask(importedSnapshotId),
          RegisterAmiTask(amiName, encryptedSnapshotId)
        ))
      ))
    )
  }
}
