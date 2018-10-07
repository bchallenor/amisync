package amisync

import com.amazonaws.services.ec2.model.{ImportSnapshotRequest, SnapshotDiskContainer, UserBucket}

case class ImportSnapshotTask(bucket: Bucket, key: Key, k: SnapshotId => Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.importSnapshot({
      val req = new ImportSnapshotRequest()
      req.setRoleName(vmImportRoleName.name)
      req.setDiskContainer({
        val diskContainer = new SnapshotDiskContainer
        diskContainer.setFormat("RAW")
        diskContainer.setUserBucket({
          val userBucket = new UserBucket()
          userBucket.setS3Bucket(bucket.name)
          userBucket.setS3Key(key.name)
          userBucket
        })
        diskContainer
      })
      req
    })
    Set(
      WaitForImportSnapshotTask(ImportTaskId(res.getImportTaskId), k)
    )
  }
}
