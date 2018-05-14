---
layout: post
title: "Pentaho Data Integration on YARN"
summary: This article explains how to use YARN to manage PDI processes
date: 2018-05-12
categories: PDI
tags: PDI, YARN
published: false
---

Quite often the need emerges to run **PDI processes** on **several nodes**. Historically this has been possible by using the **Carte server** that ships with the default PDI package. You can configure a Master-Slaves setup and then your nodes would be ready to accept **PDI processes**: Kind of, that is. You still would have to implement a lot of logic to deal with load balancing (you don't want to hit sole one node), failover etc. This logic can we implemented just via PDI processes, however, how hard you try, this will hardly ever be a bullet-proof solution. In general, using this approach, there is a lot of overhead on your side to get this multi-node setup going.

But why reinvent the wheel? 12 years ago or so this work would have been justified, but these days we have massively tested and accpeted resource managers that can take care of it: Take **YARN** for example. Why would we go through the pain of implementing this logic if YARN (or any other resource manager for that matter) can already provide us with this functionality out-of-the-box?

Now, before we proceed, please let me explain, that what you will read over the next few paragraphs is not something I discovered! The whole idea is originating from one person that continuously keeps surprising me when it comes to solutions around Pentaho: **Alexander Schurman**. The beautify of this solution that we will take a look at in a minute is that it is so simple! It surprises me that throught the many years that **Pentaho MapReduce** has been available, no one come up with this!

It's important to mention that the strategy outlined below is **not** a **MapReduced** job! Nor can it be used to run one process across multiple nodes: You can schudule to run one PDI job as many YARN applications (on many nodes), however, they will all run on their own and not know of each other. This contrary to what you would do with **Pentaho MapReduce**, Pentaho **PDI Cluster on YARN** (EE feature) or  Pentaho **AEL Spark**. You could in example create on PDI job, that accepts different parmaters, and then schedule it e.g. 10 times to run on 10 nodes/containers with 10 different parameter values. Overall this setup behaves similar to the new Pentaho EE **Worker Nodes** feature.

The **PDI Cluster on YARN** uses **Carte**, and you need a minimum of two nodes (1 master + 1 slave): It is intended to execute distributed a transformation. In contrast, the process described in this article, will execute a simple `kitchen` or `pan` process, which stops after it is finished.

Let us just summarise which benefits we get by running PDI processes via **YARN**:

- Define resources like **memory**, **cores** for each PDI process
- Use a specific **queue**. E.g. Create a queue that guarantees resources for your processes even when the cluster is very busy.
- **Impersonation**
- **Kerberos** integration  
- **Monitoring**

> **Note**: Any dependencies, like PDI plugins, settings (e.g. `KETTLE_HOME`), etc can be included if required and will be automatically distributed to each node by YARN.

## PDI via YARN: How does it work?

Before you start, make sure that your PDI client and/or Pentaho Server is/are configured to run on a Hadoop Cluster (Cloudera, MapR, Hortenworks, ...). There is enough info on the Hitachi Vantara help pages, which cover this topic, so I will not go into it.

1. When you ask **PDI** to run a job or transformation on the Hadoop Cluster, what actually happens behind the scenes is that PDI uses the [Hadoop YARN Distributed Shell](https://github.com/apache/hadoop-common/tree/trunk/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell) to request containers with certain memory, cores, etc from the **YARN Resource Manager**.
2. Roughly at the same time the PDI client and dependencies will be uploaded to HDFS.
3. Inside the container, your process is run via `pan.sh` or `kitchen.sh` - just as it would normally happen.

> **Note**: You can find more info on the **Hadoop YARN Distributed Shell** [here](http://apprize.info/php/hadoop/13.html). "The Hadoop YARN project includes the Distributed-Shell application, 
which is an **example of a non-MapReduce application** built on top of YARN. Distributed-Shell is a simple mechanism for running shell commands and scripts in containers on multiple nodes in a Hadoop cluster."

### How to source the PDI jobs and transformations inside the Container

You have the following options (not a complet list):

- In the files that get deployed to the container, you can also include the `.kettle` folder with all the `repository.xml` and `kettle.properties`. This way the PDI process can connect to the **Pentaho Server repository** from each container and source the jobs and transformations.
- **Include** the PDI jobs and transformations in the files that get deployed to the container.
- Pulling the files from a **Git Repo**: In the shell script that you run on the container, you can also include a command to pull all your required PDI jobs and transformations from a git repo.

### Required Initialisation Logic

1. We upload the **PDI client** to HDFS as an archived file (e.g. `tar`). You can also add any plugins, PDI jobs, transformations, `.kettle` folder, etc to this archive if required.
2. The only artifact that is pushed to the container and executed is a shell Script. ??? HOW DOES THE SHELL FILE GET INTO THE CONTAINER ???
3. In this Script the first action is to copy the PDI client from HDFS into the container and extract it:

```
hdfs dfs -cat /path/to/pdi.tar | tar xvf –
```

The output `cat` command gets piped into the `tar` command. This series of commands extracts the PDI in the container.

## Background Info: YARN Distributed Shell

Depending on your Hadoop distribution, the location of the distributed shell might vary. With Vanilla [Apache Hadoop](http://hadoop.apache.org/), you can find it in following directory:

```bash
hadoop-2.8.0/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.8.0.jar
```

To see which arguments are accepted, run:

```bash
# Check the parameters for the DistributedShell client.
$HADOOP_HOME/bin/hadoop jar $HADOOP_HOME/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.8.0.jar org.apache.hadoop.yarn.applications.distributedshell.Client
```

Next let's try running the Linux shell `date` command. This will spin up two containers on the Hadoop cluster:

```bash
$HADOOP_HOME/bin/hadoop jar \
$HADOOP_HOME/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.8.0.jar \
org.apache.hadoop.yarn.applications.distributedshell.Client \
--jar $HADOOP_HOME/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.8.0.jar \
--shell_command date \
--num_containers 2 \
--master_memory 1024
```

In more detail: We are asking YARN to run the `Client` class in the `hadoop-yarn-applications-distributedshell-2.8.0.jar`. With the `--jar` flag we provide the definition of the **Application Master**, which is the same jar file.

The last line of the execution log should say:

```
INFO distributedshell.Client: Application completed successfully`
```

Somewhere further up in the log you should also find the application id that was assigned to the execution:

```
INFO impl.YarnClientImpl: Submitted application application_1526138475801_0001
```

If you are running a pseudo-distributed Hadoop cluster, you can check for the output of the `date` command like so:

```
# grep "" $HADOOP_HOME/logs/userlogs/<APPLICATION ID>/**/stdout
$ grep "" $HADOOP_HOME/logs/userlogs/application_1526138475801_0001/**/stdout
/home/dsteiner/apps/hadoop-2.8.0/logs/userlogs/application_1526138475801_0001/container_1526138475801_0001_01_000002/stdout:Sat 12 May 16:42:44 BST 2018
/home/dsteiner/apps/hadoop-2.8.0/logs/userlogs/application_1526138475801_0001/container_1526138475801_0001_01_000003/stdout:Sat 12 May 16:42:45 BST 2018
```

- [Source](https://www.alexjf.net/blog/distributed-systems/hadoop-yarn-installation-definitive-guide/)

## Actual Implementation

To "talk" with the **Hadoop YARN Distributed Shell** from within **PDI**, we will use the **Hadoop Job Executer** job entry.

The following settings are very important:

- **Jar**: Local?? path to `hadoop-yarn-applications-distributedshell-*.jar`
- **Driver Class**: `org.apache.hadoop.yarn.applications.distributedshell.Client`
- **Command Line Arguments**:

```bash
-jar ${PARAM_YARN_APP_JAR} -num_containers 1 -container_memory ${PARAM_YARN_CONTAINER_MEMORY} -container_vcores ${PARAM_YARN_CONTAINER_VCORES} -queue ${PARAM_YARN_QUEUE} -appname ${VAR_APP_UNIQUE_ID}-${PARAM_YARN_APP_NAME} -shell_script ${PARAM_YARN_DS_SHELL_SCRIPT}  ${PARAM_YARN_DS_SHELL_ENV} -shell_args ${PARAM_YARN_DS_SHELL_ARGS}
```

- Tick **Enable Blocking**.
- Set **Logging Interval** to `10`.

Variable name | Value | Description
--------------|---------|---------------------------
`PARAM_PDI_JOB_DIR` |	`/public/Push_YARN`	|
`PARAM_PDI_JOB_NAME`	   | `jb_example`	|
`PARAM_YARN_CONTAINER_MEMORY` |	15 |	
`PARAM_YARN_CONTAINER_VCORES` |	1	|
`PARAM_YARN_QUEUE` | default	
`PARAM_YARN_APP_NAME` |	my-dummy-app
`PARAM_YARN_DS_SHELL_SCRIPT` |	/home/pentaho/container_kitchen.sh	
`PARAM_YARN_DS_SHELL_ENV`      |	`-shell_env KETTLE_USER=admin -shell_env KETTLE_PASSWORD=password` |	
`PARAM_YARN_DS_SHELL_ARGS`    |	`${PARAM_PDI_JOB_DIR} ${PARAM_PDI_JOB_NAME}`	| 
`PARAM_YARN_APP_JAR`   |	`$HADOOP_HOME/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.8.0.jar` |	

## Retrieving the Logs

### REST Call

You can perform this simple REST call to get the execution status of your application:

```
http://localhost:8088/ws/v1/cluster/apps?startedTimeBegin=${START_EPOCH}
```

### YARN client

You can again configure a **Hadoop Job Executor** job entry


- **Jar**: Local path to YARN Distributed Shell jar.
- **Driver Class**: `org.apache.hadoop.yarn.client.cli.LogsCLI`
- **Command Line Arguments**: `-applicationId application_[YOUR APPLICATION ID]`
- Tick **Enable Blocking**