package amisync

import java.util.UUID

import spray.json._

import scala.collection.immutable.{ListMap, Queue}
import scala.concurrent.duration._
import scala.reflect.ClassTag

package object json {
  import DefaultJsonProtocol._

  private def jsonFormatVia[A: ClassTag, V: ClassTag: JsonFormat](apply: V => A, unapply: A => Option[V]): JsonFormat[A] = new JsonFormat[A] {
    private lazy val aClassName = implicitly[ClassTag[A]].runtimeClass.getSimpleName
    private lazy val vClassName = implicitly[ClassTag[V]].runtimeClass.getSimpleName
    override def read(json: JsValue): A = apply(json.convertTo[V])
    override def write(a: A): JsValue = {
      val maybeV = unapply(a)
      val v = maybeV.getOrElse(serializationError(s"Could not convert $aClassName to $vClassName: $a"))
      v.toJson
    }
  }

  implicit lazy val unitJsonFormat: JsonFormat[Unit] = jsonFormatVia[Unit, JsObject](_ => (), _ => Some(JsObject.empty))

  implicit lazy val finiteDurationFormat: JsonFormat[FiniteDuration] = jsonFormatVia[FiniteDuration, String](
    s => Duration(s).asInstanceOf[FiniteDuration],
    d => Some(d.toString)
  )

  implicit lazy val regionNameFormat: JsonFormat[RegionName] = jsonFormatVia(RegionName.apply, RegionName.unapply)
  implicit lazy val roleNameFormat: JsonFormat[RoleName] = jsonFormatVia(RoleName.apply, RoleName.unapply)
  implicit lazy val bucketFormat: JsonFormat[Bucket] = jsonFormatVia(Bucket.apply, Bucket.unapply)
  implicit lazy val keyFormat: JsonFormat[Key] = jsonFormatVia(Key.apply, Key.unapply)
  implicit lazy val keyPrefixFormat: JsonFormat[KeyPrefix] = jsonFormatVia(KeyPrefix.apply, KeyPrefix.unapply)
  implicit lazy val amiIdFormat: JsonFormat[AmiId] = jsonFormatVia(AmiId.apply, AmiId.unapply)
  implicit lazy val amiNameFormat: JsonFormat[AmiName] = jsonFormatVia(AmiName.apply, AmiName.unapply)
  implicit lazy val importTaskIdFormat: JsonFormat[ImportTaskId] = jsonFormatVia(ImportTaskId.apply, ImportTaskId.unapply)
  implicit lazy val snapshotIdFormat: JsonFormat[SnapshotId] = jsonFormatVia(SnapshotId.apply, SnapshotId.unapply)

  implicit lazy val taskJsonFormat: JsonFormat[Task] = new JsonFormat[Task] {
    private implicit lazy val delayTaskFormat: JsonFormat[DelayTask] = jsonFormat2(DelayTask)
    private implicit lazy val deleteSnapshotTaskFormat: JsonFormat[DeleteSnapshotTask] = jsonFormat1(DeleteSnapshotTask)
    private implicit lazy val deregisterAmiTaskFormat: JsonFormat[DeregisterAmiTask] = jsonFormat1(DeregisterAmiTask)
    private implicit lazy val importAmiFromS3TaskFormat: JsonFormat[ImportAmiFromS3Task] = jsonFormat3(ImportAmiFromS3Task)
    private implicit lazy val importAmiFromSnapshotTaskFormat: JsonFormat[ImportAmiFromSnapshotTask] = jsonFormat2(ImportAmiFromSnapshotTask)
    private implicit lazy val registerAmiTaskFormat: JsonFormat[RegisterAmiTask] = jsonFormat2(RegisterAmiTask)
    private implicit lazy val waitForCopySnapshotTaskFormat: JsonFormat[WaitForCopySnapshotTask] = jsonFormat2(WaitForCopySnapshotTask)
    private implicit lazy val waitForImportSnapshotTaskFormat: JsonFormat[WaitForImportSnapshotTask] = jsonFormat2(WaitForImportSnapshotTask)

    override def read(json: JsValue): Task = {
      json.asJsObject.fields("task").convertTo[String] match {
        case "Delay" => delayTaskFormat.read(json)
        case "DeleteSnapshot" => deleteSnapshotTaskFormat.read(json)
        case "DeregisterAmi" => deregisterAmiTaskFormat.read(json)
        case "ImportAmiFromS3" => importAmiFromS3TaskFormat.read(json)
        case "ImportAmiFromSnapshot" => importAmiFromSnapshotTaskFormat.read(json)
        case "RegisterAmi" => registerAmiTaskFormat.read(json)
        case "WaitForCopySnapshot" => waitForCopySnapshotTaskFormat.read(json)
        case "WaitForImportSnapshot" => waitForImportSnapshotTaskFormat.read(json)
      }
    }

    override def write(obj: Task): JsValue = obj match {
      case task: DelayTask => writeTask("Delay", task)
      case task: DeleteSnapshotTask => writeTask("DeleteSnapshot", task)
      case task: DeregisterAmiTask => writeTask("DeregisterAmi", task)
      case task: ImportAmiFromS3Task => writeTask("ImportAmiFromS3", task)
      case task: ImportAmiFromSnapshotTask => writeTask("ImportAmiFromSnapshot", task)
      case task: RegisterAmiTask => writeTask("RegisterAmi", task)
      case task: WaitForCopySnapshotTask => writeTask("WaitForCopySnapshot", task)
      case task: WaitForImportSnapshotTask => writeTask("WaitForImportSnapshot", task)
    }

    private def writeTask[T: JsonFormat](typeName: String, task: T): JsObject = {
      JsObject(ListMap("task" -> typeName.toJson) ++ task.toJson.asJsObject.fields.toList.sortBy(_._1))
    }
  }

  private trait Placeholder[A] {
    def placeholder(uuid: UUID): A
  }

  private implicit lazy val snapshotIdPlaceholder: Placeholder[SnapshotId] = uuid => SnapshotId(uuid.toString)

  implicit def continuationJsonFormat[A: Placeholder: JsonFormat, B: JsonFormat]: JsonFormat[A => B] = new JsonFormat[A => B] {
    override def write(k: A => B): JsValue = {
      val param = implicitly[Placeholder[A]].placeholder(UUID.randomUUID())
      JsObject(
        "param" -> param.toJson,
        "expr" -> k(param).toJson
      )
    }

    override def read(json: JsValue): A => B = {
      val obj = json.asJsObject
      val paramJson = obj.fields("param")
      val exprJson = obj.fields("expr")

      a => {
        val paramValJson = a.toJson

        def replace(json: JsValue): JsValue = json match {
          case `paramJson` => paramValJson
          case JsObject(fields) => JsObject(fields.map { case (k, v) => k -> replace(v) })
          case JsArray(elements) => JsArray(elements.map(replace))
          case x: JsString => x
          case x: JsNumber => x
          case x: JsBoolean => x
          case x: JsNull.type => x
        }

        replace(exprJson).convertTo[B]
      }
    }
  }

  implicit def setJsonFormat[A: JsonFormat]: JsonFormat[Set[A]] = jsonFormatVia[Set[A], List[A]](x => Set(x: _*), x => Some(x.toList))
  implicit def queueJsonFormat[A: JsonFormat]: JsonFormat[Queue[A]] = jsonFormatVia[Queue[A], List[A]](x => Queue(x: _*), x => Some(x.toList))
}
