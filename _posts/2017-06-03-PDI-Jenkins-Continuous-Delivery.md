---
layout: post
title: "Pentaho Data Integration: Continuous Integration and Delivery with Jenkins"
summary: This article explains how to configure PDI to run with Spark
date: 2017-06-03
categories: PDI
tags: PDI, Jenkins
published: false
---


# Components


Normally, the steps are as follows:

(1) Agile Development (AD)

(2) Continuous Integration (CI) / Integration Test

- Checkout code
- Compile solution
- Run unit test

(3) Continuous Delivery

- Same as before and
- ready to be deployed without human intervention (so this includes a **release**)

(4) Continuous Deployment (CD)

- end-to-end solution deployment into production without any human intervention.

DevOps:

Stages  | AD | CI | CDel | CD | PDI fit (differences)
--------|----|----|------|----|-----------------
Code    | X  | X  | X    | X  | no code, but XML (instructions)
Build   | X  | X  | X    | X  | nothing to compile
Test    |    | X  | X    | X  | no existing test frameworks
Release |    |    | X    | X  | static cut of XML
Deploy  |    |    | X    | X  |
Operate |    |    |      | X  |
First stage (Plan) not shown above.


Software Packages:

- **Contiuous Integration Server** (e.g. Jenkins with jUnit): CI and CD
- **Binary Artifact Repository** (e.g. [Artifactory](https://www.jfrog.com/Artifactory) or [Nexus](https://www.sonatype.com/nexus-repository-oss))
- **Automatic Deployment Framework** (e.g. [Ansible](https://www.ansible.com/)): Deployment


# Jenkins

[Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/) is a setup of plugins to create continuous integration and/or delivery pipelines. Pipelines can be defined via a **Pipeline DSL** (Domain-Specific Language, based on **Groovy**) in a [Jenkinsfile](https://jenkins.io/doc/book/pipeline/jenkinsfile/) using a text editor of your choice. The `Jenkinsfile` would normally be version controlled. You can define the pipeline either in a *declarative* or *scripted* syntax. 

When creating a **pipeline** via the **web UI**, you can either reference the `Jenkinsfile` from your version controlled project repository via the **Pipeline Script from SCM** (see [here](https://jenkins.io/doc/book/pipeline/getting-started/#defining-a-pipeline-in-scm)) or use an embedded online text editor to create the file there.

1. The first thing to define in a **pipeline** is an `agent`, which allocates an **executor** and **workspace**. 
2. You then define the `stages` of the **pipeline** by listing what each `stage` is supposed to do. A stage is a distinct set of work, e.g. build, test, deploy. Each stage has to finish **successfully** before the next stage can begin.
3. For each `stage` you can define the **actions** within the `steps` element.
4. There are various actions available, e.g. `sh` to execute a shell command or `junit` to aggregating test reports.

```groovy
pipeline {
    agent any 

    stages {
        stage('Build') { 
            steps { 
                sh 'make' 
            }
        }
        stage('Test'){
            steps {
                sh 'make check'
                junit 'reports/**/*.xml' 
            }
        }
        stage('Deploy') {
            steps {
                sh 'make publish'
            }
        }
    }
}
```

Steps to run are added as an item to the **Jenkins queue**. The steps will run as soon as an **executor** is free on a node.

Some **workspaces** might not get cleaned after a period of inactivity (see [here](https://issues.jenkins-ci.org/browse/JENKINS-2111)).

[Getting Started with Pipeline](https://jenkins.io/doc/book/pipeline/getting-started/#getting-started-with-pipeline) provides a splendid introduction to the topic.

We will create a `Jenkinsfile` ourselves to get more familiar with the syntax (you can later on use the web UI to create it in an easier fashion):

1. In the **root** directory of your versioned project directory create a `Jenkinsfile`.
2. Use the snippet below as a starting point. This snippet is written in the declarative syntax:

	```groovy
  pipeline {

    agent any

    stages {
      stage('Build') {
        steps {
          echo 'Building..'
        }
      }
      stage('Test') {
        steps {
          echo 'Testing..'
        }
      }
      stage('Deploy') {
        steps {
          echo 'Deploying....'
        }
      }
    }
  }
  ```

3. As mentioned, the `agent` **directive** (see full docu [here](https://jenkins.io/doc/book/pipeline/syntax/#agent)) **instructs** Jenkins to allocate an executor and workspace for the Pipeline. **Note**: By default the agent directive will **check out** the source repository. There are various agents available: `any`, `docker`, `dockerfile`, `none` etc. If you define `none`, you have to define an `agent` for each `stage` directive.
4. In quite a lot of project where your source code is written in some programming language like Java or Scala, the first **stage** would be the build stage, where the code is assembled, compiled and packaged. Jenkins would just invoke the build tool and then you would instruct Jenkins to pick up the target/built file (e.g. jar file) and saves them to the Jenkins master repository for later retrieval (for unit testing etc). However, with PDI, there is no code to be compiled, since the transformations and jobs are saved as **XML** files and are basically just instructing **PDI** what to do. 
5. **Unit Tests** are usually part of the next stage. Jenkins has a number of test recording, reporting, and visualization facilities provided by a [various plugins](https://plugins.jenkins.io/?labels=report). We can use the `junit` step - install the [JUnit plugin](https://plugins.jenkins.io/junit) therefor.
6. The final **deployment** stage usually publishes the code to an **Artifactory** server and/or pushes the code to the **production system**.

The stages mentioned above are just an example, you can have any kind of stages.

# Jenkins Pipeline for PDI

So what should our pipeline actually do?

1. Check out the code from CVS, e.g. git.
2. Spin up a PDI Docker container.
3. Run PDI Unit Tests on the Docker Container
4. Run Integration Test on the Docker Container
5. Deploy the code to Artifactory. 


## Checkout Code

1. Create new **Pipeline** via **web UI**.
2. Define **SCM polling trigger**.
3. Reference `Jenkinsfile` via the **Pipeline script from SCM** option.


## Unit testing

Most **PDI** developers are not **Java** developers. 

A **unit test** should be written by the same ETL developer who creates the transformation, not someone else. Before you build your ETL process, you should create the sample input dataset and the golden dataset (the expected outcome). This is best practice.

**PDI** does not have an officially supported Unit Testing Framework, although Matt Casters created a plugin for such purpose (see [here](https://github.com/mattcasters/pentaho-pdi-dataset)).

We can keep unit testing simple: We run our transformation in a dedicated environment with our testing data. **Unit testing is just another environment**: This way we do not have to replace input and output steps - we just create a new environment for performing unit tests. In the simplest case we have one unit test per transformation. We can run the whole master job with our test data. All that we have to do then is to check the output of this process against the expected output (our **golden dataset**). For the comparison we can just use the two Kettle transformation which Andrea Torre highlighted in [this blog post](http://andtorg.github.io/bi/2015/08/12/testing-with-kettle) and a master job.

In general the approach could be:

1. Developer has a local dev environment with synthetic data (which is either provided or the developer creates himself/herself). The developer also prepares the golden dataset. Once her/his transformation passes the unit test, the code gets commit
2. Integration Test and Unit Test: Run the whole process with the synthetic data in a dedicated environment.
3. Integration Test with subset of real data
4. Deploy to Artifactory

Note that each environment is using the same models, so if you use Hadoop in production, all the other environments must be set up in the same way. Unit testing in this case does not mean actually dynamically replacing input and output steps with the test data, as it quite happens with junit etc, but we just run the whole process as is in a dedicated environment with the test data and in addition compare the results to the golden datasets.

Ad 2) Maybe instead of doing everything in PDI, we can just offload the work to the DB directly?

Ad 4) There are no binary artifacts with PDI. Unless maybe you create the environment with a Docker image?



---

## Our First Pipeline: Run Transformation

First we try without Docker. Our aim it to only clone the repo to a dedicated workspace and run a transformation.

Create a new Github repo (or similar) and clone it to your local drive.

Create following basic folder structure:

```bash
mkdir config etl shell-scripts
```

Create a simple ktr which reads in an integer column from a text file, adds 1 and outputs the result to another text file. Save the file in the `etl` folder with the name `tr_add_one.ktr`. In the `shell-scripts` folder create a new file called `run_tr_add_one.sh` and make it executable. Add the following content:

```bash
#!/bin/bash
# full path required
/home/dsteiner/apps/data-integration/pan.sh -file=../etl/tr_add_one.ktr
RES=$?
exit $RES
```

In the project root directory create a new `Jenkinsfile` with following content:

```groovy
#!/usr/bin/env groovy
pipeline {

  agent any

  stages {
    stage('Test'){
      steps{
        sh 'shell-scripts/run_tr_add_one.sh'
      }
    }
  }
}
```

Add all the new files to git, commit and push.

Next start **Jenkins** if it is not already running. If you haven't installed it yet, you can follow the instructions outlined in my blog post [Agile Data Integration: Continuous Integration with Jenkins and PDI](http://diethardsteiner.github.io/pdi/2017/02/17/Continous-Integration-with-Jenkins-and-PDI.html).

In the **Jenkins Web UI** click on **New Item**. On the next page provide a name, e.g. `pdi-ci-test` and then click on **Pipeline**.

On the next page, under **Build Triggers** tick **Poll SCM** and provide a **Schedule**, e.g. `H * * * *`.

In the **Pipeline** section, under **Definition**, pick **Pipeline script from SCM**. Define the **Repository URL**.

Finally click **Save**.

Next click on **Build Now**.

If the build fails, just click on **Console Output** to understand what went wrong.

![](/images/pdi-jenkins-ci/pic1.png)

## Publish Code to Artifactory

- [Example here](https://jenkins.io/doc/pipeline/examples/#artifactory-generic-upload-download)
- [Working With Pipeline Jobs in Jenkins](https://www.jfrog.com/confluence/display/RTF/Working+With+Pipeline+Jobs+in+Jenkins)

We have to install the [Jenkins Artifactory Plugin](https://github.com/JFrogDev/jenkins-artifactory-plugin) which provides support for **Artifactory** operations as part of the Pipeline script DSL. The plugin provides following features:

- downloading of dependencies
- uploading of artifacts and 
- publishing build-info to Artifactory

