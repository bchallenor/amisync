package amisync

import spray.json._

import scala.collection.immutable.{ListMap, Queue}
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

  implicit val unitJsonFormat: JsonFormat[Unit] = jsonFormatVia[Unit, JsObject](_ => (), _ => Some(JsObject.empty))

  implicit val regionNameFormat: JsonFormat[RegionName] = jsonFormatVia(RegionName.apply, RegionName.unapply)
  implicit val roleNameFormat: JsonFormat[RoleName] = jsonFormatVia(RoleName.apply, RoleName.unapply)
  implicit val bucketFormat: JsonFormat[Bucket] = jsonFormatVia(Bucket.apply, Bucket.unapply)
  implicit val keyFormat: JsonFormat[Key] = jsonFormatVia(Key.apply, Key.unapply)
  implicit val keyPrefixFormat: JsonFormat[KeyPrefix] = jsonFormatVia(KeyPrefix.apply, KeyPrefix.unapply)
  implicit val amiIdFormat: JsonFormat[AmiId] = jsonFormatVia(AmiId.apply, AmiId.unapply)
  implicit val amiNameFormat: JsonFormat[AmiName] = jsonFormatVia(AmiName.apply, AmiName.unapply)
  implicit val importTaskIdFormat: JsonFormat[ImportTaskId] = jsonFormatVia(ImportTaskId.apply, ImportTaskId.unapply)
  implicit val snapshotIdFormat: JsonFormat[SnapshotId] = jsonFormatVia(SnapshotId.apply, SnapshotId.unapply)

  implicit val taskJsonFormat: JsonFormat[Task] = new JsonFormat[Task] {
    private implicit val deleteSnapshotTaskFormat: JsonFormat[DeleteSnapshotTask] = jsonFormat1(DeleteSnapshotTask)
    private implicit val deregisterAmiTaskFormat: JsonFormat[DeregisterAmiTask] = jsonFormat1(DeregisterAmiTask)
    private implicit val importAmiFromS3TaskFormat: JsonFormat[ImportAmiFromS3Task] = jsonFormat3(ImportAmiFromS3Task)
    private implicit val importAmiFromSnapshotTaskFormat: JsonFormat[ImportAmiFromSnapshotTask] = jsonFormat2(ImportAmiFromSnapshotTask)
    private implicit val registerAmiTaskFormat: JsonFormat[RegisterAmiTask] = jsonFormat2(RegisterAmiTask)
    private implicit val waitForCopySnapshotTaskFormat: JsonFormat[WaitForCopySnapshotTask] = jsonFormat1(WaitForCopySnapshotTask)
    private implicit val waitForImportSnapshotTaskFormat: JsonFormat[WaitForImportSnapshotTask] = jsonFormat1(WaitForImportSnapshotTask)

    override def read(json: JsValue): Task = {
      json.asJsObject.fields("task").convertTo[String] match {
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

  implicit def queueJsonFormat[A: JsonFormat]: JsonFormat[Queue[A]] = jsonFormatVia[Queue[A], List[A]](x => Queue(x: _*), x => Some(x.toList))
}
