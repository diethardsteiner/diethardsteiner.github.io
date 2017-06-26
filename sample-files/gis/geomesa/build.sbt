name := "GeoMesaSparkExample"

version := "0.1"

scalaVersion := "2.11.8"

// Add necessary resolvers
resolvers ++= Seq(
  "locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "boundlessgeo" at "https://repo.boundlessgeo.com/main",
  "osgeo" at "http://download.osgeo.org/webdav/geotools",
  "conjars.org" at "http://conjars.org/repo",
  "media.javax" at "http://maven.geotoolkit.org"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.2" % "provided"
  , "org.apache.spark" %% "spark-catalyst" % "2.0.2" % "provided"
  , "org.apache.spark" %% "spark-sql" % "2.0.2" % "provided"
/**  , "org.apache.spark" %% "spark-yarn" % "2.0.2" % "provided" **/
  , "org.locationtech.geomesa" %% "geomesa-accumulo-datastore" % "1.3.1"
  , "org.locationtech.geomesa" %% "geomesa-accumulo-spark" % "1.3.1"
  , "org.locationtech.geomesa" %% "geomesa-spark-sql" % "1.3.1"
  , "ch.qos.logback" % "logback-classic" % "1.1.7"
  , "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  , "org.apache.accumulo" % "accumulo-core" % "1.7.3"
  , "org.geotools" % "gt-epsg-hsql" % "17.1"
)


assemblyMergeStrategy in assembly := {
  case path => {
    val strategy = (assemblyMergeStrategy in assembly).value(path)
    if (strategy == MergeStrategy.deduplicate) {
      MergeStrategy.first
    } else {
      strategy
    }
  }
}


/**
assemblyMergeStrategy in assembly := {

  case PathList("org","geotools", xs @ _*) => MergeStrategy.last
  case x => 
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
**/


  
