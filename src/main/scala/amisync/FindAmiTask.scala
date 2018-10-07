package amisync

import com.amazonaws.services.ec2.model._

import scala.collection.JavaConverters._

case class FindAmiTask(amiName: AmiName, k: AmiId => Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    import config._
    val res = ec2.describeImages({
      val req = new DescribeImagesRequest
      req.setOwners(Set("self").asJava)
      req.setFilters(Set({
        val filter = new Filter
        filter.setName("name")
        filter.setValues(Set(amiName.name).asJava)
        filter
      }, {
        val filter = new Filter
        filter.setName("state")
        filter.setValues(Set("available", "pending").asJava)
        filter
      }).asJava)
      req
    })
    res.getImages.asScala.headOption.map { image =>
      k(AmiId(image.getImageId))
    }.getOrElse(Set.empty[Task])
  }
}
