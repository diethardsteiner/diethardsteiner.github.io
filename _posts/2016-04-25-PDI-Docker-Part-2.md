---
layout: post
title:  "Using Pentaho Data Integration with Docker: Part 2"
summary: This provides detailled instructions on how to use PDI with Docker
date: 2016-04-25
categories: PDI
tags: PDI, Docker
published: false
---

In the [previous blog post] we got a **Docker Carte cluster** running. In this blog post, we will start shipping **PDI transformations** to the cluster. The easiest way to achieve this (primarily for develpment purposes) is to register the cluster in **Spoon** (PDI's GUI client) and tell it to execute a given transformation on the cluster.

If your cluster is not running yet, issue the following command from within the project director (which we set up last time):

```bash
$ docker-compose start
# alternatively:
$ docker-compose up -d 
```

Let's get hold of the host ports:

```bash
$ docker-compose ps
    Name                  Command               State                 Ports               
-----------------------------------------------------------------------------------------
pdi_master_1   ../scripts/docker-entrypoi ...   Up      0.0.0.0:8181->8181/tcp            
pdi_slave_1    ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:32771->8182/tcp 
pdi_slave_2    ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:32770->8182/tcp
```

Make sure that the slaves registered with the master by looking at following page:

```
http://<master-ip>:8181/kettle/getSlaves/
```

Wait a few seconds until all slaves are ready, if you cannot see them being registered by then, restart the slaves like so:

```bash
$ docker-compose restart slave
```


Next start **Spoon**. For this tutorial I am using **PDI** version 6.1. Create a **new transformation**. On the left hand side, click on the **View** tab and then right click on the **Slave server** folder and choose **New**.

First we will specify the details for the **Carte master** - make sure you tick *Is the master*:

![](pdi-docker-II-1.png)

To ensure that the connection details are correct, right click on each **Carte server** node and choose **Monitor**.

![](pdi-docker-II-2.png)

Next right click on the **Kettle cluster schemas** folder (within the **View** tab) and choose **New**. Provide a **Schema name**, tick **Dynamic cluster** and then click on the **Select slave servers** button: Select our master server. If you had another **master** server, you could define it here as well for failover purposes (see [docu](http://wiki.pentaho.com/display/EAI/Dynamic+clusters)).

![](pdi-docker-II-3.png)

> **Important**: We specified port 40000 (the default) for our **Kettle cluster schema**. For the communication between **Carte master** and **Carte slaves** a range of **TCP ports** are required. In our case port 40000 is the first port that the Carte cluster will use, if any additional ones are required, other ports (with increasing port number) will be opened. So basically in the **Kettle cluster schema** you define the start of the port range. Some more interesing info on this can be found on [Slawo's excellent blog post](http://type-exit.org/adventures-with-open-source-bi/2011/10/clustering-in-kettle/).

As we haven't opened a range of ports starting from port number 40000 in our **Docker** configuration, we have one more task to do to ensure that everything is running properly .... [OPEN] 

Add a **Data Grid**, **Sort rows**, **Sorted Merge** and **Dummy** step to the canvas and connect them. Populate the **Data Grid** step with a few sample records, including and ID as the first column. Specify this **ID** in the **Sort rows** and **Sorted Merge** steps.

Now the idea is to run the **Data Grid** and **Sort rows** steps on the slaves and the remaining steps on the master. Right click on the **Data Grid** step and choose **Clusters**. From the pop-up window choose the cluster we created earlier on. Repeat the same process for the **Sort rows** step.

You transformation should look like this now:

![]()

Execute the transformation now. In the **Run options** dialog choose **Clustered** as **Environment Type**. Then click **Run**.


-- OLD --

## Running a PDI job or transformation on a running Cluster

So now we can open our favourite web browser and check if **Carte** is available on `http://192.168.99.100:8181`. As we didn't provide a custom user name and password environment variable, the default ones are `cluster` and `cluster`. Afer logging in, click on the **Show status** link. On the next page, just below the picture of the transformation, click **Start transformation**.


Observer the **status** of the execution in the **Active** column. Additionally, pay attention to the log output. If all goes well, the status shown should be **Finished**.

...

## Dynamically spinning up a cluster to run a PDI job or transformation


## Calling the REST API to run a job or transformation

Another option to run jobs or transformations is to call the REST API like so


[Official Docu](http://wiki.pentaho.com/display/EAI/Carte+Configuration)

![[OPEN]] You can also add **PDI repository** details to the the Carte **config** to make the execution of jobs and transformations easier.

