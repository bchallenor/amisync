package amisync

import java.util.Collections

import com.amazonaws.services.ec2.model.DescribeImportSnapshotTasksRequest

case class WaitForImportSnapshotTask(importTaskId: ImportTaskId) extends Task {
  override def run(ctx: Context): List[Task] = {
    import ctx._
    val res = ec2.describeImportSnapshotTasks({
      val req = new DescribeImportSnapshotTasksRequest()
      req.setImportTaskIds(Collections.singleton(importTaskId.id))
      req
    })
    val detail = res.getImportSnapshotTasks.get(0).getSnapshotTaskDetail
    detail.getStatus match {
      case "active" =>
        println(s"Waiting for snapshot ${detail.getSnapshotId} to be imported (${detail.getProgress}%)")
        List(this)

      case "completed" =>
        Nil

      case status =>
        throw new IllegalStateException(s"Unknown import snapshot task status: $status")
    }
  }
}
