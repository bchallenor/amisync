package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.DescribeImportSnapshotTasksRequest

import scala.concurrent.duration._

case class WaitForImportSnapshotTask(importTaskId: ImportTaskId, k: SnapshotId => Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.describeImportSnapshotTasks({
      val req = new DescribeImportSnapshotTasksRequest()
      req.setImportTaskIds(Collections.singleton(importTaskId.id))
      req
    })
    val detail = res.getImportSnapshotTasks.get(0).getSnapshotTaskDetail
    detail.getStatus match {
      case "active" =>
        println(s"Waiting for $importTaskId to complete (${detail.getProgress}%)")
        Set(DelayTask(5.seconds, Set(this)))

      case "completed" =>
        k(SnapshotId(detail.getSnapshotId))

      case status =>
        throw new IllegalStateException(s"Unknown import snapshot task status: $status")
    }
  }
}
