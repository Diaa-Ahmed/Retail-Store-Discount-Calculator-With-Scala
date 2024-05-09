ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "Discounts",
    idePackagePrefix := Some("Discounts"),
    libraryDependencies += "com.oracle.database.jdbc" % "ojdbc8" % "19.3.0.0"
  )
