package amisync

import com.amazonaws.services.ec2.model.{ImportSnapshotRequest, SnapshotDiskContainer, UserBucket}

case class ImportAmiFromS3Task(amiName: AmiName, bucket: Bucket, key: Key) extends Task {
  override def run(config: Config): List[Task] = {
    import config._
    val res = ec2.importSnapshot({
      val req = new ImportSnapshotRequest()
      req.setRoleName(vmImportRoleName.name)
      req.setDiskContainer({
        val diskContainer = new SnapshotDiskContainer
        diskContainer.setDescription(amiName.name)
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
    val detail = res.getSnapshotTaskDetail
    List(
      WaitForImportSnapshotTask(ImportTaskId(res.getImportTaskId)),
      ImportAmiFromSnapshotTask(amiName, SnapshotId(detail.getSnapshotId))
    )
  }
}
