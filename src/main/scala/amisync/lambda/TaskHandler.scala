package amisync.lambda

import amisync._
import amisync.json._
import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.SendMessageRequest
import spray.json._

import scala.concurrent.duration._

class TaskHandler extends RequestHandler[SQSEvent, Unit] {
  override def handleRequest(event: SQSEvent, context: Context): Unit = {
    require(event.getRecords.size == 1)
    val taskStr = event.getRecords.get(0).getBody
    val task = try JsonParser(taskStr).convertTo[Task] catch {
      case e: Exception =>
        throw new IllegalArgumentException(s"Could not parse JSON: $taskStr", e)
    }

    val config = Config.default

    println(s"Running task: ${task.toJson.compactPrint}")
    val nextTasks = task.run(config)

    nextTasks foreach { nextTask =>
      TaskHandler.submit(config, nextTask, delay = Duration.Zero)
    }
  }
}

object TaskHandler {
  def submit(config: Config, task: Task, delay: FiniteDuration): Unit = {
    task match {
      case DelayTask(d, k) =>
        k foreach { delayedTask =>
          submit(config, delayedTask, delay = delay + d)
        }
      case _ =>
        println(s"Submitting task with delay of $delay: ${task.toJson.compactPrint}")
        val sqs = AmazonSQSClientBuilder.defaultClient()
        sqs.sendMessage({
          val req = new SendMessageRequest()
            .withQueueUrl(config.taskQueueUrl.toString)
            .withMessageBody(task.toJson.compactPrint)
            .withDelaySeconds(Math.toIntExact(delay.toSeconds))
          req
        })
    }
  }
}
