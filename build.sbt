name := "amisync"

scalaVersion := "2.12.7"

libraryDependencies ++= List(
  "com.amazonaws" % "aws-java-sdk-ec2" % "1.11.422",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.422"
)

assembly/assemblyOutputPath := baseDirectory.value / "target" / "amisync.jar"