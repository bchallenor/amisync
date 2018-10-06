package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.DescribeImportSnapshotTasksRequest

import scala.collection.immutable.Queue

case class WaitForImportSnapshotTask(importTaskId: ImportTaskId) extends Task {
  override def run(config: Config): Queue[Task] = {
    import config._
    val res = ec2.describeImportSnapshotTasks({
      val req = new DescribeImportSnapshotTasksRequest()
      req.setImportTaskIds(Collections.singleton(importTaskId.id))
      req
    })
    val detail = res.getImportSnapshotTasks.get(0).getSnapshotTaskDetail
    detail.getStatus match {
      case "active" =>
        println(s"Waiting for snapshot ${detail.getSnapshotId} to be imported (${detail.getProgress}%)")
        Queue(this)

      case "completed" =>
        Queue.empty

      case status =>
        throw new IllegalStateException(s"Unknown import snapshot task status: $status")
    }
  }
}
