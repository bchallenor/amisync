package object amisync {
  case class RegionName(name: String) {
    require(name ne null)
  }

  case class RoleName(name: String) {
    require(name ne null)
  }

  case class FunctionName(name: String) {
    require(name ne null)
  }

  case class Bucket(name: String) {
    require(name ne null)
  }

  case class Key(name: String) {
    require(name ne null)
  }

  case class KeyPrefix(name: String) {
    require(name ne null)
  }

  case class AmiId(id: String) {
    require(id ne null)
  }

  case class AmiName(name: String) {
    require(name ne null)
  }

  object AmiName {
    def deriveFrom(key: Key): AmiName = {
      // Strip directory name and extension
      AmiName(key.name.split("/").last.split("\\.").head)
    }
  }

  case class ImportTaskId(id: String) {
    require(id ne null)
  }

  case class SnapshotId(id: String) {
    require(id ne null)
  }
}
