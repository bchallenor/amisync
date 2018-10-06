package object amisync {
  case class RegionName(name: String) extends AnyVal
  case class RoleName(name: String) extends AnyVal
  case class Bucket(name: String) extends AnyVal
  case class Key(name: String) extends AnyVal
  case class KeyPrefix(name: String) extends AnyVal
  case class AmiId(id: String) extends AnyVal
  case class AmiName(name: String) extends AnyVal
  case class ImportTaskId(id: String) extends AnyVal
  case class SnapshotId(id: String) extends AnyVal
}
