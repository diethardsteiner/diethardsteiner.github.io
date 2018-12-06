---
layout: post
title: "Pentaho Data Integration/Kettle: The easy way to create Beam Pipelines"
summary: This article explains how to get started with creating Beam pipelines in PDI
date: 2018-12-01
categories: PDI
tags: PDI, Beam
published: true
---

Resources:

- [Github: kettle-beam repo](https://github.com/mattcasters/kettle-beam)
- [Github: kettle-beam-core repo](https://github.com/mattcasters/kettle-beam-core)
- [Video: Kettle on Beam : first results](https://www.youtube.com/watch?feature=youtu.be&v=DlTOVh55Rag&app=desktop#dialog)

# Introduction

**Pentaho Data Integration/Kettle** has for a long time offered a convenient graphical interface to create **data integration processes** without the need for coding (although if someone ever wanted to code something, there is the option available). About two years ago Pentaho's/Hitachi Vantara's own take on an **adaptive execution layer** (AEL) was added to PDI: The grand goal has been to design the DI process once and be able to run it via differenet engines. From the start **Spark** as well as PDI's own engine was supported and this is still the state today, although in some official slides you can see that the ambitions are to extend this Flink etc. It has also taken several iteration to improve their AEL/get it production ready. Whenever I ask around, there doesn't seem To be anyone using AEL in production (which is not to say that there aren't companies out there using AEL in production, but it is certainly not the adoption rate that I'd hoped for).  

From the start I have never quite understood why one company would try to create an AEL by themselves, considering the open source **Apache Beam** project was already a top level Apache project at the time with wide support from various companies. It seemed quite obvious that this project would move on in a more rapid speed than one company alone could ever achieve. Plus **Apache Beam** at the time already supported several execution engines. I suppose there must have been some good reasons on the Pentaho/Hitachi Vantara side to go down their own route.

**Matt Casters**, the original creator of **Kettle/PDI** left **Hitachi Vantara** end of last year to start a new career with **Neo4j** and in the last few months he has been very active creating plugins for **PDI** that solve some longstanding shortcomings. It was on a **Slack** chat that I suggested to him to go for **Apache Beam**, because having Beam support available within **PDI** would finally catapult it back to being a leading data integration tool - a position that it kind of had lost over the last few years. Remember when PDI was the first drag and drop DI tool to support **MapReduce**? 

So here we are today looking at the rapidly evolving **Kettle Beam** project, that **Matt Casters** put a massive effort in (burning a lot of midngiht oil):

> **Important**: Kettle Beam Plugin is **currently under heavy development** and it's only a few days old (2018-12-01). It goes without saying that **you must not use this plugin in production**.

**Special thanks** goes to **Matt Casters** for providing a lot of info for this article.

**THIS ARTICLE IS STILL DEVELOPING ...**

# Download Pentaho Data Integration (PDI)

You can find the **PDI package** on [Sourceforge](https://sourceforge.net/projects/pentaho/files/). Just pick the latest version 8.1 (at the time of this writing). Once downloaded, unzip it in a convenient folder and that's basically all there is to installation. Inside the PDI folder you find the `spoon.sh` file (or `spoon.bat` on Windows), which will start the GUI of the designer.

# Building the plugin

Install Maven, in example like so (adjust to your own setup):

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
mvn --offline clean install
cp target/kettle-beam-core-0.0.4-SNAPSHOT.jar ${PDI_HOME}/lib
cd ${GIT_HOME}/kettle-beam
git pull
mvn --offline clean install
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

In a similar vain, create the **file output definition**. This one is actually not required any more, Kettle will fetch the metadata automatically from the incoming stream.

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
2018-12-01,UK,London,Groceries,22909
```

## Add the Beam Output Step

Again from the **Design** tab add the **Beam Output** step to the canvas and link it up to the **Beam Input** step. You can create a link/hop by hovering over the input step, which will bring up HuD dialog, then click on the icon shown below and drag the hop to the output step:

![](/images/kettle-beam/kettle-beam-5.png)

Double click on the **Beam Output** step. Let's configure it:

- **Output location**: `${PARAM_OUTPUT_DIR}`
- **File prefix**:
- **File suffix**:
- **Windowed** (unsupported):
- **File definition to use**: Pick the output schema/definition you created earlier on. This one is actually not required any more, Kettle will fetch the metadata automatically from the incoming stream.

## Add Transfromation Steps

Some steps require a special implementation, like Sort etc.
As it is extremely early days with this project, these steps are supported:

- Memory Group by work with Sum and Count.

Beyond that all steps should work as long as you don't bring together data streams.
For bringing streams together Matt is planning to implement support for Merge Join.

## Set up the Beam Job Configuration

Via the main menu choose **Beam > Create a Beam Job Config**. We will create a job configuration for the **Direct runner**:

- **Name**: `Direct`
- **Description**: anything you fancy
- **Runner**: The options here are `Direct`, `DataFlow`, `Flink` and `Spark`. Just pick `Direct` for now.
- **User Agent**: `Kettle`. This a free-form field.
- **Temporary Location**: `/tmp` (in this case in can be a local directory)
- **Google Cloud Plaform - Dataflow**: We can leave this section empty
- **Variables/Parameters to set**: We list here any parmaters and values that we want to use as part of our Beam pipeline.

![](/images/kettle-beam/kettle-beam-6.png)
## Batch VS Stream

So how do I configure the pipeline to use streaming and not batch processing you might wonder? 

Currently the **PDI Beam plugin** doesn't have any unbound input data sources yet. Kafka will be the first probably ( see [ticket](https://github.com/mattcasters/kettle-beam-core/issues/3)). Once Beam sees it has an unbound (unending) input source, it will automatically stream the data.

# How to test the PDI Beam Pipeline locally

To create **unit tests** for your PDI Beam Pipeline, you can use **Matt Casters** [Pentaho PDI Datasets](https://github.com/mattcasters/pentaho-pdi-dataset) Plugin. See also my blog post [here](http://diethardsteiner.github.io/big/data/2016/01/30/PDI-Unit-Testing.html) for further info.

# Execution your PDI Beam Pipeline

You can execute the **PDI Beam Pipeline** via **Beam > Run this transformation on Beam**. Then you can pick your **Beam Job Configuration** before running the process.

## Engine: Direct

**Direct** uses the **Direct Beam engine**, which enables you to test your **PDI Beam Pipeline** locally. We have already created the Beam Job configuration previously for the local runner, so you are ready to go.

## Engine: Google Cloud Platform DataFlow

> **Note**: This works as well with **PDI CE** (Community Edition, the one you downloaded from SourceForge) - no **EE** subscription required.

### Setting up a Service Account

Background info:

- [Deploying a Pipeline](https://cloud.google.com/dataflow/docs/guides/deploying-a-pipeline)
- [Getting started with authentication](https://cloud.google.com/docs/authentication/getting-started)
- [Google Cloud Dataflow Security and Permissions](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions)

Before gettings started make sure you have GCP account set up and you have the required credentials file:

1. Pick the correct project or create a new one.
2. Create a new **service account** via the [GCP IAM - Service Accounts page]. Further info: [Getting started with authentication](https://cloud.google.com/docs/authentication/getting-started). Make sure to tick the **Create key (optional)** option at the end. This is the key (JSON file) that we require to authenticate from our local machine ( and also to set `GOOGLE_APPLICATION_CREDENTIALS`).
3. Define the `GOOGLE_APPLICATION_CREDENTIALS` environment variable: `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/credentials.json`.
4. Create a **custom role** called `kettle-beam-role` via the [GCP Roles page](https://console.cloud.google.com/iam-admin/roles). Find further instructions [here](https://cloud.google.com/iam/docs/creating-custom-roles)). Give this role everything you can find related to API, Storage, Dataflow. Not complete list of required permissions: `compute.machineTypes.get`, `dataflow.*`, `storage.*`, `firebase.projects.get`, `resourcemanager.projects.get`.
5. Assign your **service account** to the **custom role**. Go back to the **IAM** page, click on the **pencil** icon left next to the service account and click on the **Role** pull-down menu. Type `beam` into the filter and then select our `kettle-beam-role`.
6. Create a **storage bucket**: Go to the [GCP Storage page](https://console.cloud.google.com/storage/browser) and click on **Create bucket**. Name it `kettle-beam-storage`. Once created, you should be automatically forwarded to the **Bucket Details** page: Click on **Create folder** and name it `input`. Create two other ones: `binaries`, `output`.
7. Still on the **Bucket Details** page, click on the `input` folder and click on **Upload files**. Upload the input file for your PDI Beam pipeline.
8. **Grant service account permissions on your storage bucket**: Still on the **Bucket Details** page, click on the **Permissions** tab. Here your service account should be listed.
9. Enable the **Compute Engine API** for your project from the [APIs page](https://console.cloud.google.com/project/_/apiui/apis/library) in the Google Cloud Platform Console. Pick your project and search for `Compute Engine API`. After a few clicks you come to the full info page. Give it a minute or so to show the **Manage** etc buttons. In my case the **API enabled** status was already shown:
![](/images/kettle-beam/kettle-beam-9.png)
  Following APIs are required (most of them are enabled by default):
    - Cloud Datastore API
    - Cloud Filestore API: used for creating and managing cloud file servers.
    - Cloud Firestore API (NoSQL document database) - not enabled by default
    - Firebase Rules API
    - Cloud OS Login API
    - Cloud Resource Manager API
    - Compute Engine API
    - Compute Engine Instance Group API
    - Dataflow API
    - Google Cloud APIs
    - Google Cloud Deployment Manager v2 API
    - Google Cloud Storage
    - Google Cloud Storage JSON API
    - Google Compute Engine Instance Group Updater API
    - Service Management API
    - Service Usage API
    - Stackdriver Debugger API
    - Stackdriver Logging API
    - Stackdriver Monitoring API
    - Stackdriver Trace API
    
1. **Grant service account permissions on DataFlow**: Go to the [GCP Dataflow page](https://console.cloud.google.com/dataflow) and ?? [OPEN] I didn't see any options here. See also [Google Cloud Dataflow Security and Permissions](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions) for more info. Two accounts required: **Cloud Dataflow Service Account** and **Controller Service Account**. By default, workers use your project’s Compute Engine service account as the controller service account.  This service account ( `<project-number>-compute@developer.gserviceaccount.com`) is automatically created when you enable the **Compute Engine API** for your project from the [APIs page](https://console.cloud.google.com/project/_/apiui/apis/library) in the Google Cloud Platform Console.

**Additional info**:

These are the roles required on Google Storage for this to work ([Source](https://forums.pentaho.com/threads/231062-Pentaho-8-1-is-available/)): 

- Storage Admin
- Storage Object Admin
- Storage Object Creator
- Storage Object Viewer

Then locally on your machine set following **environment variables** so that **PDI** can pick up the **credentials**:
 
```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-key.json
GOOGLE_CLOUD_PROJECT=yourproject # this one is no longer needed
# since now include in the PDI Beam job config
```

### Working with the Google Cloud SDK Command Line Tools

You might have the [Cloud SDK](https://cloud.google.com/sdk/docs/quickstart-linux) command line tools already installed. 

In case you are wondering with which user you are running the commands, execute this:

```bash
$ gcloud auth list 
            Credentialed Accounts
ACTIVE  ACCOUNT
*       diethard.steiner@bissolconsulting.com

To set the active account, run:
    $ gcloud config set account `ACCOUNT`
``` 

If you created the project earlier on, you can list all projects like so:

```bash
$ gcloud projects list
```

You can switch to the `kettle-beam` project run:

```bash
$ gcloud config set project kettle-beam
$ gcloud config list project
[core]
project = kettle-beam

Your active configuration is: [default]
```

> **Note**: `GOOGLE_APPLICATION_CREDENTIALS` are not used by the gcloud client tools and there is no way to set this. See also [here](https://serverfault.com/questions/848580/how-to-use-google-application-credentials-with-gcloud-on-a-server). It is only used for **service accounts**.

There many command options available which you can explore by yourself easily.

### Testing Basic GCP Connection from within Spoon

Restart **Spoon** and choose **File > Open URL**. From the **Open File** dialog pick **Google Cloud Storage** from the **Location** pull-down menu. You will notice that the folder path automatically changes to `gs://`, but no files or folders are shown. Just add your bucket name to the folder path (e.g `gs://kettle-beam-storage/`) and hit Enter. Now the folder should be displayed:

![](/images/kettle-beam/kettle-beam-8.png)

### PDI Config

For your transformation create a dedicated **GCP DataFlow** config, e.g.:

![](/images/kettle-beam/kettle-beam-7.png)

> **Important**: Prefix all paths with `gs://` (also for the **Temporary Location**). Also pay attention to the **Project ID**: This is not the name but - well as it says - the ID (which can be different). If you are not sure what the project ID is, simply click on the project picker at the top of the Web UI and the pop-up will show you the project name with the ID:

![](/images/kettle-beam/kettle-beam-11.png)

Alternatively, you can query the **project ID** with the command line tool:

```bash
$ gcloud projects list
PROJECT_ID            NAME                PROJECT_NUMBER
k8s-pentaho-server    k8s-pentaho-server  619953174803
kettle-beam-224314    kettle-beam         684324837668
```

The first time you run the **PDI Beam pipeline** in **GCP DataFlow ** it will be quite slow:

In the **Beam Job Configuration** within the **DataFlow** settings you define under **Staging location** where to store the **binaries** (example: `gs://kettledataflow/binaries`). The first time your run your pipeline the **Google DataFlow** runner will upload the **PDI/Kettle binaries** from the lib folder to this **Staging folder**. This allows you to also include any specific **PDI plugins** that you require for your project.

Note that any other file paths you define should also follow this VFS pattern, e.g. `gs://ketteldataflow/input/sales-data.csv`.

### Running the PDI Beam Pipeline

This is no differnt from any other PDI Beam execution - just pick the correct config.

Once started, you should after a bit see this screen:

![](/images/kettle-beam/kettle-beam-14.png)

Just click **Yes** and the make your way to the **GCP admin console**. Go to the **DataFlow** section. You should see your **PDI Beam Pipeline** there now:

![](/images/kettle-beam/kettle-beam-12.png)

Click on your **Job** and you should see some more details:

![](/images/kettle-beam/kettle-beam-13.png)

And then check the output:

![](/images/kettle-beam/kettle-beam-15.png)

The job/PDI Beam pipeline you saw the screenshot of above was extremely simple (just input and output) - if we add another step to transform the data, in example a **Memory Group By** step, the resulting graph will look like this:

![](/images/kettle-beam/kettle-beam-16.png)

### How to check if it is working

As mentioned above, the first time your execute the process on **GCP DataFlow**, it will take a bit for the binaries to be uploaded to **GCP Storage**: This might give the impression that nothing is happening. The best thing to do in this case is to check if the jar files are showing up in the `binaries` folder in the **GCP Storage bucket**. You can do this via the web interface or by running e.g. following command:

```bash
$ gsutil du -sh gs://kettle-beam-storage/binaries
397.17 MiB  gs://kettle-beam-storage/binaries
```

As you can see, all the uploaded jar files amount to nearly 400MB.

What the **logs**:

- Within Spoon/Stdout
- In the **GCP DataFlow Web Console** you find an option to see the log for your job.

### Common Errors

#### Errors shown within Spoon

If there are permission issues, Spoon will show the error message in a pop-up, e.g.: 

```
Failed to create a workflow job: (7b5183c85358d0cf): Could not create workflow; user does not have write access to project: kettle-beam Causes: (7b5183c85358d196): Permission 'dataflow.jobs.create' denied on project: 'kettle-beam'
```

In my case I did originally specify the project name instead of the project ID in the Beam job config. Changing this resolved the problem.

#### Errors shown within GCP Dataflow Web Console

```
java.io.FileNotFoundException: No files matched spec: gs://...
```

Make sure you have permissions to see the file! Open **Spoon** and go to **New > Open URL**. For **Location** pick **Google Cloud Storage** and paste the whole path into the **Folder** input field and hit enter. If you have the correct permissions you should see the folder/file.

## Analysing the Stats

Via the GCP interface you can analyse the various stats around **Counters** (see **Custom Counters** section). Each **PDI step** gets its own counter here.