---
layout: post
title: "Pentaho Data Integration: The easy way to create Beam Pipelines"
summary: This article explains how to get started with creating Beam pipelines in PDI
date: 2018-12-01
categories: PDI
tags: PDI, Beam
published: false
---

Resources:

- [Github: kettle-beam repo](https://github.com/mattcasters/kettle-beam)
- [Github: kettle-beam-core repo](https://github.com/mattcasters/kettle-beam-core)
- [Video: Kettle on Beam : first results](https://www.youtube.com/watch?feature=youtu.be&v=DlTOVh55Rag&app=desktop#dialog)

# Introduction

**Pentaho Data Integration/Kettle** has for a long time offered a convenient graphical interface to create **data integration processes** without the need for coding (although if someone ever wanted to code something, there is the option available). About two years ago Pentaho's/Hitachi Vantara's own take on an **adaptive execution layer** (AEL) was added to PDI: The grand goal has been to design the DI process once and be able to run it via differenet engines. From the start **Spark** as well as PDI's own engine was supported and this is still the state today, although in some official slides you can see that the ambitions are to extend this Flink etc. It has also taken several iteration to improve their AEL/get it production ready. Whenever I ask around, there doesn't seem anyone using AEL in production (which is not to say that there aren't companies out there using AEL in production, but it is certainly not the adoption rate that I'd hoped for).

From the start I never quite understood why one company would try to create an AEL by itself, considering the open source **Apache Beam** project was already a top level Apache project at the time with wide support from various companies. It's seemed quite obvious that this project would move on in a more rapid speed than one company alone could ever achieve. Plus **Apache Beam** at the time already supported several execution engines. I supposed there must have been some good reasons on the Pentah/Hitachi Vantara side to go down their own route.

**Matt Casters**, the original creator of **Kettle/PDI** left **Hitachi Vantara** end of last year to start a new career with **Neo4j** and in the last few months he has been very active creating plugins for **PDI** that solve some longstanding shortcomings. It was on a **Slack** chat that I suggested to him to go for **Apache Beam**, because having Beam support available within **PDI** would finally catapult it back to being a leading data integration tool - a position that it kind of had lost over the last few years. Remember when PDI was the first drag and drop DI tool to support **MapReduce**? 

So here we are today looking at the rapidly evolving **Kettle Beam** project, that **Matt Casters** put a massive effort in (burning a lot of midngiht oil):

# Download Pentaho Data Integration (PDI)

...

# Building the plugin

Install Maven:

```
# ########## MAVEN ##########
# https://maven.apache.org/install.html
# 
cd $WORK_DIR
wget https://www-eu.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz
tar xzvf apache-maven-3.6.0-bin.tar.gz

# Add the bin directory to the PATH environment variable
cat >> ~/.bashrc <<EOL
# ======== MAVEN ======== #
export MAVEN_HOME=\$WORK_DIR/apache-maven-3.6.0
export PATH=\$PATH:\$MAVEN_HOME/bin
EOL

source ~/.bashrc

echo "--- Finished installing Maven ---"
# Confirm Maven is properly set up
mvn -v
```

Install the Pentaho settings.xml for maven in `~/.m2`:

```
cd ~/.m2
wget https://raw.githubusercontent.com/pentaho/maven-parent-poms/master/maven-support-files/settings.xml
```

## Phase 1: kettle-beam-core

```bash
cd ~/git
git clone https://github.com/mattcasters/kettle-beam-core.git
cd kettle-beam-core
mvn clean install
```

Copy the `target/kettle-beam-[VERSION].jar` your the PDI `lib/` folder, e.g.:

```bash
cp target/kettle-beam-core-0.0.4-SNAPSHOT.jar ~/apps/pdi-ce-8.1/lib
```

## Phase 2: kettle-beam

Clone the plugin and start the build:

```
cd ~/git
git clone git@github.com:mattcasters/kettle-beam.git
cd kettle-beam
mvn clean install
```

Next:

1. Create a **new directory** called `kettle-beam` in `[PDI-ROOT]/plugins/`
2. Copy `target/kettle-beam-[VERSION].jar` to `[PDI-ROOT]/plugins/kettle-beam/`.
3. All the files from the `target/lib` folder also have to go into `[PDI-ROOT]/lib` folder. However, there are already some existing `jackson*` jar files there (`lib/jackson-annotations-*.jar`, `jackson-core-*` and `databind-*`). Just keep the latest version of them there and not both versions, otherwise some weird things can happen.
4. Delete `[PDI-ROOT]/system/karaf/caches` as well for good measure.


Example:

```bash
mkdir ~/apps/pdi-ce-8.1/plugins/kettle-beam
cp target/kettle-beam-0.0.4-SNAPSHOT.jar ~/apps/pdi-ce-8.1/plugins/kettle-beam/
cp target/lib/* ~/apps/pdi-ce-8.1/lib
# remove jackson duplicates
rm -r ~/apps/pdi-ce-8.1/system/karaf/caches
```

## Rapid rebuild

Since this project is in rapid development, you will have to rebuild the plugin regulary. Here is a small (but not perfect) **utility script** to rebuild the plugin (adjust variables):

```bash
#!/bin/bash
export PDI_HOME=~/apps/pdi-ce-8.1
export GIT_HOME=~/git/
echo "***************** NOTE ****************"
echo "If KETTLE BEAM version number changes this will break!"
echo "***************************************"
cd ${GIT_HOME}/kettle-beam-core
git pull
mvn clean install
cp target/kettle-beam-core-0.0.4-SNAPSHOT.jar ${PDI_HOME}/lib
cd ${GIT_HOME}/kettle-beam
git pull
mvn clean install
cp target/kettle-beam-0.0.4-SNAPSHOT.jar ${PDI_HOME}/plugins/kettle-beam/
cp target/lib/* ${PDI_HOME}/lib
rm -r ${PDI_HOME}/system/karaf/caches
```

# Creating a Beam Pipeline in PDI

Start **Spoon**, the graphical user interface for PDI:

```bash
cd [PDI-ROOT]
sh ./spoon.sh
```

Create a new **transformation** via the main menu: File > New > Transformation.

## Create an Input and Output File Definition

If the plugin was installed correctly, you should see a **Beam** entry in the **menu bar**. 

![kettle-beam-2](/images/kettle-beam/kettle-beam-2.png)

The first thing we have to do is to **create an input file definition** for our **Beam Pipeline**. This basically depicts the structure of the data when it enters the pipeline:

So via the main menu choose **Beam > Create a file definition**. The goal here is to describe the structure of the input files, which could reside in a HDFS folder or other file stores. You define which **fields** the file contains, of which **type** they are etc and also what the **separator** and **enclosure** for these fields is: all pretty standard metadata/**schema**. Provide all this required metadata now:

- **Name**: `sales-input-schema`
- **Description**: `sample sales dataset`
- **Field Separator**: `${PARAM_FIELD_SEPARATOR}`
- **Field Enclosure**: `${PARAM_FIELD_ENCLOSURE}`
- **Field definitions**: Provide as shown below in the screenshot:

![kettle-beam-3](/images/kettle-beam/kettle-beam-3.png)

Click **Ok**.

In a similar vain, create the **file output definition**.

## Add the Beam Input Step

Next, in the **Design** tab on the left, expand the **Big Data** folder (or alternatively search for `Beam`): 
![kettle-beam-1](../../diethardsteiner.github.io/images/kettle-beam/kettle-beam-1.png)

Drag and drop the **Beam Input** step onto the **canvas** and double click on it:

- Input location: The directory where your input files are stored. This could be a directory on HDFS or any other file storage. Ideally parameterise this.
- File definitoin to use: Pick the schema we created earlier on.

![](/images/kettle-beam/kettle-beam-4.png)

> **Important**: The input dataset must not have a header!

This won't work:

```
date,country,city,department,sales_amount
2018-12-01,UK,London,Groceries,22909
```

This will work:

```
date,country,city,department,sales_amount
```

## Add the Beam Output Step

Again from the **Design** tab add the **Beam Output** step to the canvas and link it up to the **Beam Input** step. You can create a link/hop by hovering over the input step, which will bring up HuD dialog, then click on the icon shown below and drag the hop to the output step:

![](/images/kettle-beam/kettle-beam-5.png)

Double click on the **Beam Output** step. Let's configure it:

- **Output location**: `${PARAM_OUTPUT_DIR}`
- **File prefix**:
- **File suffix**:
- **Windowed** (unsupported):
- **File definition to use**: Pick the output schema/definition you created earlier on.

## Add Transfromation Steps

## Set up the Beam Job Configuration

Via the main menu choose **Beam > Create a Beam Job Config**. We will create a job configuration for the **Direct runner**:

- **Name**: `Direct`
- **Description**: anything you fancy
- **Runner**: The options here are `Direct`, `DataFlow`, `Flink` and `Spark`. Just pick `Direct` for now.
- **User Agent**: `Kettle` [OPEN] Is this a free-form field or is something specific expected here?
- **Temporary Location**: `/tmp` (in this case in can be a local directory)
- **Google Cloud Plaform - Dataflow**: We can leave this section empty
- **Variables/Parameters to set**: We list here any parmaters and values that we want to use as part of our Beam pipeline.

![](/images/kettle-beam/kettle-beam-6.png)

# How to test the PDI Beam Pipeline locally

Chances are you don't have a full Beam environment on your developer machine. Luckily, **Matt Casters** also create the **Pentaho Datasets Plugin**, which allows you to create **unit tests**.

... 

# Execution your PDI Beam Pipeline

You can execute the **PDI Beam Pipeline** via **Beam > Run this transformation on Beam**. Then you can pick your **Beam Job Configuration** before running the process.

## Engine: Direct

## Engine: Google Cloud Platform DataFlow

Before gettings started make sure you have GCP account set up. Then locally on your machine set following **environment variables** so that **PDI** can pick up the **credentials**:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-key.json
GOOGLE_CLOUD_PROJECT=yourproject # this one is no longer needed
# since now include in the PDI Beam job config
```


The first time you run it in **GCP DataFlow ** it will be quite slow:
In the **Beam Job Configuration** within the **DataFlow** settings you define under **Staging location** where to store the **binaries** (example: `gs://kettledataflow/binaries`). The first time your run your pipeline the Google DataFlow runner will upload the Kettle binaries from the lib folder to this **Staging folder**. This allows you to also include any specific **PDI plugins** that you require for your project.


Note that any other file paths you define should also follow this pattern, e.g. `gs://ketteldataflow/input/sales-data.csv`.

## Analysing the Stats

Via the GCP interface you can analyse the various stats around **Counters** (see **Custom Counters** section). Each **PDI step** gets its own counter here.