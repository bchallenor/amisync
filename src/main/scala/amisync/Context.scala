package amisync

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.s3.AmazonS3

trait Context {
  def regionName: RegionName
  def vmImportRoleName: RoleName
  def s3: AmazonS3
  def ec2: AmazonEC2
}
