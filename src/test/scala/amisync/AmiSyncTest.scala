package amisync

import org.junit.Assert._
import org.junit._

class AmiSyncTest {
  @Test
  def test(): Unit = {
    val bucket = Bucket("bucket")
    val keyPrefix = KeyPrefix("ami/")
    val config = new MemoryConfig(bucket)

    config.s3.keys += Key("ami/machine.img")

    AmiSync.run(config, bucket, keyPrefix, skipDelays = true)

    assertEquals(
      MemoryEC2(
        importTasks = Map(),
        snapshots = Map(
          SnapshotId("snap-002") -> MemorySnapshot(
            SnapshotId("snap-002"),
            description = "machine",
            encrypted = true,
            progress = 100
          )
        ),
        amis = Map(
          AmiId("ami-001") -> MemoryAmi(
            AmiId("ami-001"),
            name = AmiName("machine"),
            description = "machine",
            snapshotId = SnapshotId("snap-002")
          )
        )
      ),
      config.ec2
    )

    config.s3.keys -= Key("ami/machine.img")

    AmiSync.run(config, bucket, keyPrefix, skipDelays = true)

    assertEquals(
      MemoryEC2(
        importTasks = Map(),
        snapshots = Map(),
        amis = Map()
      ),
      config.ec2
    )
  }
}
