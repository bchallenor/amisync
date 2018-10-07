package amisync.lambda

import amisync._
import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.lambda.runtime.events._

import scala.collection.JavaConverters._

class S3Handler extends RequestHandler[S3Event, Unit] {
  override def handleRequest(event: S3Event, context: Context): Unit = {
    print(s"Event: $event")

    val config = Config.default

    val tasks: Set[Task] = event.getRecords.iterator.asScala.map { record =>
      record.getEventName.split(":")(0) match {
        case "ObjectCreated" =>
          val bucket = Bucket(record.getS3.getBucket.getName)
          val key = Key(record.getS3.getObject.getKey)
          val amiName = AmiName.deriveFrom(key)
          ImportAmiFromS3Task(amiName, bucket, key)
        case eventType =>
          sys.error(s"Cannot handle event type: $eventType")
      }
    }.toSet

    tasks foreach { task =>
      TaskHandler.invokeAsync(config, task)
    }
  }
}
