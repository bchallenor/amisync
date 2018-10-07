package amisync.lambda

import amisync._
import amisync.json._
import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.sqs._
import spray.json._

class TaskHandler extends RequestJsonHandler[Task, Unit] {
  override def handleRequest(task: Task, context: Context): Unit = {
    println(s"Running task: ${task.toJson.compactPrint}")

    val config = Config.default

    val nextTasks = task.run(config)

    nextTasks foreach { nextTask =>
      TaskHandler.submit(config, nextTask)
    }
  }
}

object TaskHandler {
  def submit(config: Config, task: Task): Unit = {
    val sqs = AmazonSQSClientBuilder.defaultClient()
    sqs.sendMessage(
      config.taskQueueUrl.toString,
      task.toJson.compactPrint
    )
  }
}
