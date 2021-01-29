---
typora-root-url: ..
layout: post
title: "Slimming down Apache Hop"
summary: A quick walkthrough on how to make Hop as small as possible
date: 2021-01-28
categories: [Hop]
tags: 
published: true
---

Pentaho Data Integration (PDI), the predecessor of Hop,  had grown in size substantially over years, nearly amounting to 2GB in size. This is of course undesirable for a lightweight deployments, like using it for a AWS Lamba function or creating a super small container.

Luckily for us, things have quite changed over the last few years. About 2 years ago Matt Casters, Bart Maertens and a few others founded **Project Hop** based on the open source code of PDI and then started cleaning up and improving the code base. A big focus has been set on making each step/transform an actual **plugin**, so that in case you don't really require a certain step/transform, you could just remove this plugin from the package. The great news is that recently **Hop** was accepted into the **Apache Incubator** programme.

Our first task will be to create a simple **Hop pipeline** and then to slim down the Hop package so that it only has the plugins and core functionality that we require to run this very pipeline.

Our simple **pipeline** will:

- Read Excel file from S3
- Convert it to CSV and write it back to S3

# How to slim down Hop

Download Apache Hop from [here](https://artifactory.project-hop.org/artifactory/hop-snapshots-local/org/hop/hop-assemblies-client).

## Remove non required plugins

I will explain how to remove folders and files based on the example pipeline given above. You will have to adjust these steps for your setup (which should be fairly straight forward after reading this article).

From within the **Hop** install directory run the command shown below. This will provide you a good starting point to decide which plugins to remove:

```
% tree -L 2 plugins
plugins
├── actions
│   ├── abort
│   ├── addresultfilenames
│   ├── checkdbconnection
│   ├── checkfilelocked
│   ├── columnsexist
...
├── databases
│   ├── as400
│   ├── cache
│   ├── db2
│   ├── derby
│   ├── exasol4
...
├── engines
│   └── beam
├── misc
│   ├── debug
│   ├── git
│   ├── projects
│   └── testing
├── transforms
│   ├── abort
│   ├── addsequence
│   ├── analyticquery
...
└── vfs
    ├── azure
    ├── googledrive
    └── s3

225 directories, 0 files
```

**Plugins**:

- `actions`: remove entire folder as we are not using any workflow actions
- `databases`: remove all folders apart from postgresql
- `engines`: remove entire folder
- `misc`: remove everything apart from projects
- `transforms`: delete everything apart from tableouput, textfile, writetolog
- `vfs`: remove everything apart from S3

## Remove not required core compenents

We don't need the GUI, the server, ...


```shell
rm -rf demo
rm -rf docs
rm hop-conf*
rm hop-encrypt*
rm hop-gui*
rm hop-run.bat
rm hop-server*
rm hop-translator*
rm -rf libswt
rm LICENSE.txt
rm -rf pwd
rm README.md
rm -rf static
rm translator.xml
rm -rf ui
```

`lib` folder: Again, here it might vary what you need depending on the workflows and pipelines you created. A primitive way of finding out what is needed is just to remove the libs one by one and making sure the code runs each time. In our case we can remove these libraries:

```shell
rm asm*
rm batik * #svg library
rm blueprints-core*
rm dom4j*
rm encoder*
rm enunciate-core-annotations-*
rm enunciate-jersey-rt-*
rm flexjson-*
rm hibernate*
rm http*
rm jackson-jaxrs-base-*
rm jackson-jaxrs-json-provider-*
rm janino-*
rm javax.servlet-api-*
rm jaxen*
rm jcommon*
rm jersey*
rm jetty*
rm jfreechart*
rm jsch*
rm json-simple*
rm jsr311-api-*
rm jug-lgpl-*
rm jzlib-*
rm mimepull-*
rm ognl-*
rm org.eclipse.*
rm rhino-*
rm slf4j-log4j*
rm snappy-java-*
rm spark-avro*
rm tyrus-standalone-client-*
rm webservices*
rm xercesImpl-2.12.0
rm xml*
```

So what we are left with is this:

```
apache-log4j-extras-1.2.17.jar
commons-beanutils-1.9.4.jar
commons-cli-1.2.jar
commons-codec-1.10.jar
commons-collections-3.2.2.jar
commons-compiler-3.0.8.jar
commons-compress-1.19.jar
commons-configuration-1.9.jar
commons-dbcp-1.4.jar
commons-io-2.2.jar
commons-lang-2.6.jar
commons-logging-1.1.3.jar
commons-math3-3.6.1.jar
commons-pool-1.5.7.jar
commons-validator-1.3.1.jar
commons-vfs2-2.4.1.jar
geronimo-servlet_3.0_spec-1.0.jar
gson-2.8.5.jar
guava-27.0-jre.jar
hop-core-0.40-SNAPSHOT.jar
hop-engine-0.40-SNAPSHOT.jar
hop-ui-swt-0.40-SNAPSHOT.jar
jackson-annotations-2.10.2.jar
jackson-core-2.10.2.jar
jackson-databind-2.10.2.jar
javassist-3.20.0-GA.jar
log4j-1.2.17.jar
picocli-3.7.0.jar
postgresql-42.2.13.jar
scannotation-1.0.2.jar
slf4j-api-1.7.7.jar
```

In my case, the **Hop package** is 23.8 MB in size now.

I created [this Jira request](https://issues.apache.org/jira/browse/HOP-2475) suggesting for a new option to be added to the GUI which would check the dependencies of your project and build the smallest possible Hop package for you that you can then use for deployment.