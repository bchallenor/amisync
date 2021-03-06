package amisync

import java.net.URI

import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

trait Config {
  def regionName: RegionName
  def taskQueueUrl: URI
  def vmImportRoleName: RoleName
  def s3: AmazonS3
  def ec2: AmazonEC2
}

object Config {
  def default: Config = {
    new Config {
      override lazy val regionName: RegionName = {
        val chain = new DefaultAwsRegionProviderChain
        RegionName(chain.getRegion)
      }

      override lazy val taskQueueUrl: URI = {
        new URI(sys.env("TASK_QUEUE_URL"))
      }

      override lazy val vmImportRoleName: RoleName = {
        RoleName(sys.env.getOrElse("VMIMPORT_ROLE_NAME", "vmimport"))
      }

      override lazy val s3: AmazonS3 = {
        val builder = AmazonS3ClientBuilder.standard()
        builder.setRegion(regionName.name)
        builder.build()
      }

      override lazy val ec2: AmazonEC2 = {
        val builder = AmazonEC2ClientBuilder.standard()
        builder.setRegion(regionName.name)
        builder.build()
      }
    }
  }
}
