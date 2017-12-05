---
layout: post
title: "Pentaho Data Integration: Restartable Job"
summary: This article explains how to easily add restartablility to Pentaho jobs
date: 2017-07-21
categories: PDI
tags: PDI
published: true
---

**Motivation**: When you build a job, also think of the people that have to support/monitor it moving forward. You want to make their life as easy as possible. Keep in mind that they have hundred other jobs to take care of. When your job falls over, it should not require support to dive into a 100 page instruction booklet and to find out how many special case are to consider when restarting the job (apart from also learning how to restart the job). You want to go to them and say: "**Hey folks, if the job every falls over, simply restart it**". That's easy enough. Worry free times. Hurray! Now we will take a look at how to achieve this in a very simple fashion. 

The code for the examples discussed in this post can be found [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/pdi/restartable-job).

# Restartable Job

This blog post could as well be labeled *The world's simplest restartable job*. My main aim here is to have a fast and easy implementation, without any dependencies. It is very simple and very basic. 

The idea is that we create an empty file (a **control file**) for every **unit of work** successfully done. Simple. That's it really. If a unit of work does not get completed successfully, no file will be written out. Before executing a unit of work, we check if the **control file** exits. If it exists, we do not execute the current unit of work, but move on to check if the **control file** for the next unit of work exists. Once we completed the last unit of work successfully, we know that the whole **process** finished successfully, hence we can **delete** all **control files.**

> **Note**: With this setup you can only execute units of work sequentially and care must be taken within your units of work that everything is really restartable.

> **Note**: When you take a look at the screenshot of the Pentaho Data Integration job, you will notice that the unit of work is represented by a *Wait* job entry. In reality, this would be a job or transformation job entry.

![](/images/pdi-restartable-jb-1.png)

# Restartability Wrapper

Now that we understand the simple concept perfectly, let's go one step further. Always creating a master job as shown in the screenshot would be cumbersome, hence, we will create a module/wrapper, which centralises all the functionality we need. The wrapper looks like this: 

![](/images/pdi-restartable-jb-2.png)

It accepts following parameters:

Parameter                    | Description
-----------------------------|------------------------------
`IS_LAST_UNIT_OF_WORK`         | Is this the last job or transformation to be executed? Set to `Y` or `N`
`PARAM_CONTROL_FILE_DIRECTORY` | location of the control file. must be a dedicated directory as all files will be deleted from it at the end of this process.
`PARAM_CONTROL_FILE_NAME`      | file name of the control file
`PARAM_UNIT_OF_WORK_DIRECTORY` | directory of the transformation or job to be executed
`PARAM_UNIT_OF_WORK_NAME`      | file name of the transformation or job to be executed
`PARAM_UNIT_OF_WORK_TYPE`      | set to `job` or `transformation`


Now from your master job, you can simply call the **wrapper** and **pass down the parameter** values:

![](/images/pdi-restartable-jb-3.png)

Here is an example of the parameter mapping:

![](/images/pdi-restartable-jb-4.png)

# Lock Control Wrapper

There is at least one more use case we can add. There are two kind of **control files** we are interest in:

- **Restart control files**
- Master job **lock control file**

The **logic** for the **master job lock control** is slightly different:

- The lock control file has to be **created before** we run the job
- The lock control file has to be **deleted after** the job ran, **no matter** if it was successful or not (since we want to be able to rerun the job in any case).
- As it currently stands, saving the **job lock control file** in the same folder as the **restart control files** would not work (since they are not following the same flow), so we will store them in a dedicated sub-directory called `lock-control` (we will actually create a parameter for it if someone every wanted to change the name).

The **implementation** of this looks this: 

It is best to keep the logic in separate jobs. Moving forward we will have **two wrappers**:

- `jb_lock_control_wrapper`: takes care of the **lock control** file logic.
- `jb_restartability_control_wrapper`: takes care of the **restartability** logic. This is the same job as outlined in the first section of this article.

A screenshot of `jb_lock_control_wrapper`:

![](/images/pdi-restartable-jb-5.png)

It accepts following parameters:

Parameter                    | Description
-----------------------------|------------------------------
`PARAM_CONTROL_FILE_DIRECTORY` | location of the control file. must be a dedicated directory as all files will be deleted from it at the end of this process.
`PARAM_CONTROL_FILE_NAME`      | file name of the control file
`PARAM_UNIT_OF_WORK_DIRECTORY` | directory of the job to be executed
`PARAM_UNIT_OF_WORK_NAME`      | file name of the job to be executed


Other **considerations**:

When using these **wrappers**, we have to be mindful: We cannot reference `jb_lock_control_wrapper` and `jb_restartability_control_wrapper` from the same job, because the **lock control file** would be deleted after the first job entry ran. If both are required, create one **master job** for the **lock functionality**: So you reference `jb_lock_control_wrapper` and ask it to run a **slave job**. The **slave job** then takes care of the **restartability functionality** by calling `jb_restartability_control_wrapper` for each unit of work. So there are effectively **two levels**:

- **Job Level 1**: Call wrapper for **lock functionality**
- **Job Level 2**: Call wrapper for **restartability functionality**

![](/images/pdi-restartable-jb-9.png)

A screenshot of the master job:

![](/images/pdi-restartable-jb-7.png)

A screenshot of the slave job:

![](/images/pdi-restartable-jb-8.png)


> **Note**: Since the control files are created on the local filesystem, this doesn't stop someone from kicking off the same job on another machine. If you want to avoid this situation, you can simply write the file to a shared file system, like HDFS. This can be achieved easily by using the virtual file system references, which all of the PDI steps used in the wrapper support.


## Design Decisions

**Why use a wrapper?**:

You can do whatever needs to be done **before** starting the job/transformation and also whatever needs to be done **after** the job/transformation ran. You are in full control. You could argue that you could split the functionality into two separate modules:

- Tasks **before** running and
- Tasks **after** running the job/transformation 

However in this case you rely on whoever uses your modules to use them correctly. What if they forget to use the second module (which has to run as it is part of the common logic)?

A word on the **complexity** of the wrapper: The wrapper can be as simple or complex as it is required to be. Generally speaking it should be treated like an **API**: We pass a few parameters to it, what goes on behind the scenes is not essential (but still good) to know as long as the work is completed in a satisfactory manner. 


## Some additional background info

During this project I had a mildly interesting discovery. On the first go I tried to just add the **control file functionality** to the existing **restartability wrapper** job, however, this did not work out well for the following reason:

Remember how I told you that using the wrapper to access both features (**master lock control** and **restartability feature**), you would have to use **two levels of normal jobs**:

```
master -> wrapper -> restartable job -> wrapper -> other job(s)
```

![](/images/pdi-restartable-jb-10.png)

It turns out that this setup does not work in this case since the wrapper file name is the same - unless: Well, actually, the behaviour is pretty inconsistent. If you use the **file base repo** (and I assume any other PDI repo), this runs without problems. I tested this with **PDI v5.4**. If you use the **normal files** (so not any PDI repo), then you get an error about **recursive job usage**:

```
org.pentaho.di.core.exception.KettleException:
Endless loop detected: A Job entry in this job called is calling itself.
```

It seems like this inconsistency got cleaned up in PDI v7 at some stage with the uniform referencing system.

Alex Schurman found the references in the code, which show the checks, as well as a related Jira case:

- [Code reference 1](https://github.com/pentaho/pentaho-kettle/blob/c65c88abcae13625e3bc8ddeef7337cfb1927ae1/engine/src/main/java/org/pentaho/di/job/entries/job/JobEntryJob.java#L1172): Check for parent job name
- [Code reference 2](https://github.com/pentaho/pentaho-kettle/blob/c65c88abcae13625e3bc8ddeef7337cfb1927ae1/engine/src/main/java/org/pentaho/di/job/entries/job/JobEntryJob.java#L1195): Recursive check for grand-parents etc. job name
- [Jira Case from PDI v4.4](http://jira.pentaho.com/browse/PDI-5442)

In a nutshell, while it is great to have a check like this in place (that the same job is not used recursively by accident), if you are after a wrapper, that you can use in multiple places of your process hierarchy, then you have a problem. This is not the case in the final setup discussed above, since we have split the functionality into two separate wrappers and these wrappers normally won't be called on any different levels in the hierarchy.