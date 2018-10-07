package amisync.lambda

import amisync._
import amisync.json._
import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.sqs._
import spray.json._

class TaskHandler extends RequestHandler[SQSEvent, Unit] {
  override def handleRequest(event: SQSEvent, context: Context): Unit = {
    require(event.getRecords.size == 1)
    val taskStr = event.getRecords.get(0).getBody
    val task = try JsonParser(taskStr).convertTo[Task] catch {
      case e: Exception =>
        throw new IllegalArgumentException(s"Could not parse JSON: $taskStr", e)
    }

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
