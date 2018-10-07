name := "amisync"

scalaVersion := "2.12.7"

libraryDependencies ++= List(
  "com.amazonaws" % "aws-java-sdk-ec2" % "1.11.422",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.422",
  "com.amazonaws" % "aws-java-sdk-lambda" % "1.11.422",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "io.spray" %%  "spray-json" % "1.3.4",
  "junit" % "junit" % "4.12" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)

assembly/assemblyOutputPath := baseDirectory.value / "target" / "amisync.jar"