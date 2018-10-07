package amisync

import com.amazonaws.services.ec2.model.{ImportSnapshotTask, SnapshotTaskDetail, UserBucketDetails}

case class MemoryImportTask(
  id: ImportTaskId,
  bucket: Bucket,
  key: Key,
  var progress: Int,
  var snapshotId: Option[SnapshotId]
) {
  def toModel: ImportSnapshotTask = {
    new ImportSnapshotTask()
      .withImportTaskId(id.id)
      .withSnapshotTaskDetail(
        new SnapshotTaskDetail()
          .withUserBucket(
            new UserBucketDetails()
              .withS3Bucket(bucket.name)
              .withS3Key(key.name)
          )
          .withProgress(progress.toString)
          .withStatus(if (progress == 100) "completed" else "active")
          .withSnapshotId(snapshotId.map(_.id).orNull)
      )
  }
}
