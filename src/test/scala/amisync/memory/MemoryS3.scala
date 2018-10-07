package amisync.memory

import amisync._
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}

case class MemoryS3(
  bucket: Bucket,
  var keys: Set[Key] = Set.empty
) extends AbstractS3 {
  override def listObjects(bucketName: String, prefix: String): ObjectListing = {
    require(bucketName == bucket.name)
    val listing = new ObjectListing
    keys.iterator.filter(_.name.startsWith(prefix)).foreach { key =>
      val summary = new S3ObjectSummary
      summary.setKey(key.name)
      listing.getObjectSummaries.add(summary)
    }
    listing
  }
}
