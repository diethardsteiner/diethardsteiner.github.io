---
typora-root-url: ..
typora-copy-images-to: ../images/aws-hop-part3
layout: post
title: "Getting started with Apache Hop on AWS: Lambda Functions"
summary: We will set up an Apache Hop Pipeline as a Lambda Functions
date: 2021-01-03
categories: [Hop]
tags: 
published: false
---

About 4 years ago **Dan Keeley** published two blog posts ([#Serverless #AWS PDI](https://dankeeley.wordpress.com/2017/04/25/serverless-aws-pdi/) and [Serverless PDI in AWS – Building the jar](https://dankeeley.wordpress.com/2017/05/10/serverless-pdi-in-aws-building-the-jar)) on using **Pentaho Data Integration** (PDI) with **AWS Lambda**. He also provided some more details in a the Pentaho Community Meeting of the same year. It was a very intriguing idea and still is: Back then it was a lot more complicated to slim down the fat PDI package. 



Matt:

AWS Lambda basics:

https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html

So in that handleRequest() method we want to either:

\- Run a pipeline or workflow and return some result

\- Stream data through an existing pipeline and get the results (probably a single threaded one)

For the first pipeline case I started to write a bit of documentation to give you an idea:

http://hop.apache.org/dev-manual/latest/sdk/hop-sdk.html



## Writing the Function Handler

An basic example of a Java function handler can be found [here](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html). We use this as a basis for our function.

It's best to use an **IDE** for this: Instructions to set up a Hop dev environment can be found [here](http://hop.apache.org/dev-manual/latest/setup-dev-environment.html).

The artifacts are currently not available on [Maven Central](https://mvnrepository.com). We will have to see to add this **external repository**:

```
https://repository.apache.org/content/repositories/snapshots
```

We require the following:

- `org.apache.hop.core`
- `org.apache.hop.engine`
- `hop-assemblies-libs`
- and probably `hop-assemblies-plugins-dist`

The last two are zip: first contains the lib folder next contains the plugins folder. (Thanks to Hans for explaining this!)




Let's open the project now in **IntelliJ IDEA CE**: Choose to open an existing project.
Our project looks like this so far:

IMAGE

Right click on the `lib` folder and choose **Add as library**. Make sure that the name in the upcoming dialog matches `lib` and then confirm the default settings.





Class `PipelineMeta` defines the constructor `PipelineMeta` (in line 1997, search for `public PipelineMeta`)



-----

So I'll briefly describe my approach. A warning upfront: I am not a developer (as in Java, Scala develper etc). I only do have a very basic understanding of Scala and this is what I used. And my project setup is very primitive - I am sure there are better ways to do it. With this warning out of the way, here we go:

Create your project folder:

Inside the project folder create a `build.sbt` file and add following content:

```sbt
name := "hop-lambda"

version := "0.1-SNAPSHOT"

organization := "example"

scalaVersion := "2.13.5"

scalacOptions += "-deprecation"
```

Copy all the **lib jar files** from a **Hop binary download** to:

```
<project>/lib
```

Copy a **Hop config file** inside:

```
<project>/config
```

Copy a **Hop run definition** inside:

```
<project>/config/metadata/pipeline-run-configuration
```

Copy any **plugins** you require from a **Hop binary download** to:

```
<project>/plugins
```

Create a **Scala file** called `Run.scala` in:

```
<project>/src/main/scala/example
```

with following content:

```scala
package example;

import org.apache.hop.core.variables.{IVariables, Variables}
import org.apache.hop.core.{HopEnvironment, Result}
import org.apache.hop.pipeline.PipelineMeta
import org.apache.hop.pipeline.engine.{IPipelineEngine, PipelineEngineFactory}
import org.apache.hop.metadata.util.HopMetadataUtil


object Run {

  def main(args: Array[String]):Unit = {
    // Initialize the Hop environment: load plugins and more
    HopEnvironment.init
    println("=> Done initialising Hop Environment")

    // to work with variables we create a top level IVariables object
    val variables = Variables.getADefaultVariableSpace
    println("=> Done creating Variable Space")

    // Set up the metadata to use
    val metadataProvider = HopMetadataUtil.getStandardHopMetadataProvider(variables)
    println("=> Done setting up Metadata Provider")

    // get pipeline metadata
    val pipelineMeta = new PipelineMeta(
      "file:///Users/diethardsteiner/git/apache-hop-minimal-project/main.hpl",   // The filename
      metadataProvider,             // See above
      true,                        // set internal variables
      variables                   // see above
    );
    println("=> Done sourcing Hop pipeline")

    // execute the pipeline
    // IPipelineEngine
    val pipelineEngine = PipelineEngineFactory.createPipelineEngine(
      variables,           // IVariables
      "local",            // The name of the run configuration defined in the metadata
      metadataProvider,  // The metadata provider to resolve the run configuration details
      pipelineMeta      // The pipeline metadata
    );
    println("=> Done creating Pipeline Engine")

    // We can now simply execute this engine...
    pipelineEngine.execute
    println("=> Started executing Hop Pipeline")

    // This execution runs in the background but we can wait for it to finish:
    pipelineEngine.waitUntilFinished
    println("=> Waiting for Hop Pipeline to finish")

    // When it's done we can evaluate the results:
    val result:Result = pipelineEngine.getResult
    println("=> Retrieving result")

    println("Hurrah it's all done!")
  }

}
```

In the project root folder run following command (in your Terminal):

```
sbt
```

Within the sbt prompt:

```
clean
compile
run
```

