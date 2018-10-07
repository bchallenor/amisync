package amisync.memory

import amisync._

class MemoryConfig(bucket: Bucket) extends Config {
  override lazy val regionName: RegionName = RegionName("memory")
  override lazy val taskFunctionName: FunctionName = FunctionName("task")
  override lazy val vmImportRoleName: RoleName = RoleName("vmimport")
  override lazy val s3: MemoryS3 = MemoryS3(bucket)
  override lazy val ec2: MemoryEC2 = MemoryEC2()
}
