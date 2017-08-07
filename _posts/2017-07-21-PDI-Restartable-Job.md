---
layout: post
title: "Pentaho Data Integration: Restartable Job"
summary: This article explains how to easily add restartablility to Pentaho jobs
date: 2017-07-21
categories: PDI
tags: PDI
published: true
---  


This blog post could as well be labeled *The world's simplest restartable job*. My main aim here is to have a fast and easy implementation, without any dependencies. It is very simple and very basic. 

The idea is that we create an empty file (a **control file**) for every **unit of work** successfully done. Simple. That's it really. If a unit of work does not get completed successfully, no file will be written out. Before executing a unit of work, we check if the **control file** exits. If it exists, we do not execute the current unit of work, but move on to check if the **control file** for the next unit of work exists. Once we completed the last unit of work successfully, we know that the whole **process** finished successfully, hence we can **delete** all **control files.**

> **Note**: With this setup you can only execute units of work sequentially and care must be taken within your units of work that everything is really restartable.

> **Note**: When you take a look at the screenshot of the Pentaho Data Integration job, you will notice that the unit of work is represented by a *Wait* job entry. In reality, this would be a job or transformation job entry.

![](/images/pdi-restartable-jb-1.png)

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


Now from your master job, you can simply call the wrapper and pass down the parameter values:

![](/images/pdi-restartable-jb-3.png)

Here is an example of the parameter mapping:

![](/images/pdi-restartable-jb-4.png)

# Extending the previous job

This section is developing ...

There is one more use case we can integrate into the previous job. There are two kind of **control files** we are interest in:

- **Restart** control files
- Master job **lock** control file

The reason for having the **master job lock control file** within the **restartable wrapper** is because restartability and master job control are **interlinked**: If any subprocess **fails** (transformation or job), then the **master job lock control file** has to be deleted as well.

The **logic** for the **master job lock control** is slightly different:

- The lock control file has to be **created before** we run the job
- The lock control file has to be **deleted after** the job ran, **no matter** if it was successful or not (since we want to be able to rerun the job in any case).
- As it currently stands, saving the **job lock control file** in the same folder as the **restart control files** would not work (since they are not following the same flow), so we will store them in a dedicated sub-directory called `lock-control`.

The **implementation** of this looks this:

As you can see we added an extra **check** if it is a **master lock file** request (using the existing parameter ... which now also accepts a `master` value). If the value `master` is passed in, the process continues on a **different branch**, where we first create the **lock control file**, run the job and then delete the lock control file.

Other **considerations**:

When using the **wrapper** with the **master job lock** functionality in job x, job x cannot also reference the same wrapper for the **restartability** functionality, because the **lock control file** would be deleted after the first job entry ran. If both is require, create one **master job** for the **lock functionality**. This job has then to call **another job** for the **restartability functionality**. So there are **two levels** for which the **wrapper** can be called:

- **Job Level 1**: Call wrapper for **lock functionality** (and reference job level 2)
- **Job Level 2**: Call wrapper for **restartability functionality**
