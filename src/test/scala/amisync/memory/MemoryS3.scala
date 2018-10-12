package amisync.memory

import amisync._
import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing, S3ObjectSummary}

case class MemoryS3(
  bucket: Bucket,
  var keys: Set[Key] = Set.empty
) extends AbstractAmazonS3 {
  override def listObjects(req: ListObjectsRequest): ObjectListing = {
    require(req.getBucketName == bucket.name)
    require(req.getDelimiter eq null)

    val listing = new ObjectListing
    keys.iterator.filter(_.name.startsWith(req.getPrefix)).foreach { key =>
      val summary = new S3ObjectSummary
      summary.setKey(key.name)
      listing.getObjectSummaries.add(summary)
    }
    listing
  }
}
