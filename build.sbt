name := """tasklist-webapi"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  "com.github.seratch" %% "awscala" % "0.5.+",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val dockerBuild = taskKey[Unit]("Build docker container with app")

dockerBuild := {
  clean.value
  stage.value
  "docker build -t xiongmaomaomao/tasklist-webapi ." !
}

lazy val ebBuild = taskKey[Unit]("Build artifact for uploading to eb")

ebBuild := {
  import java.io.File
  val inputs = Seq(new File("Dockerrun.aws.json") -> "Dockerrun.aws.json")
  IO.zip(inputs, new File("target/eb/artifact.zip"))
}
