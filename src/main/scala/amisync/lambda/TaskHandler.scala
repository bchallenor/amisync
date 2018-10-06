package amisync.lambda

import amisync._
import amisync.json._
import com.amazonaws.services.lambda._
import com.amazonaws.services.lambda.model._
import com.amazonaws.services.lambda.runtime._
import spray.json._

class TaskHandler extends RequestJsonHandler[Task, Unit] {
  override def handleRequest(task: Task, context: Context): Unit = {
    println(s"Running task: ${task.toJson.compactPrint}")

    val config = Config.default

    val nextTasks = task.run(config)

    nextTasks foreach { nextTask =>
      TaskHandler.invokeAsync(config, nextTask)
    }
  }
}

object TaskHandler {
  def invokeAsync(config: Config, task: Task): Unit = {
    val lambda = AWSLambdaClientBuilder.defaultClient()
    lambda.invoke({
      val req = new InvokeRequest
      req.setFunctionName(config.taskFunctionName.name)
      req.setInvocationType(InvocationType.Event)
      req.setPayload(task.toJson.compactPrint)
      req
    })
  }
}
