---
layout: post
title:  "Pentaho Kettle: Implementing Error Handling for Job and Transformation Executor Steps"
summary: Learn how to implement correct error handling for job and transformation executor steps
date:   2014-10-16
categories: Data-Integration
tags: Pentaho, Data-Integration
published: true
---

In this article I'd like to discuss how to add error handling for the new **Job Executor** and **Transformation Executor** steps in **Pentaho Data Integration**. A simple set up for demo: We use a **Data Grid** step and a **Job Executor** step for as the master transformation. 

![](/images/pdi-job-executor-initial-job-layout.jpeg)

The slave job has only a **Start**, **JavaScript** and **Abort** job entry. We set it up in a way that once the **JavaScript** entry throws an error, the **Abort** entry is called. 

![](/images/pdi-job-executor-slave-layout.jpeg)

For our demo we just make the slave job fail by adding just this to the **JavaScript** entry:

```javascript
false;
```

Let's execute the master transformation now. Observe that everything seems to have run just fine: We get the nice green tick marks on our job entries:

![](/images/pdi-job-executor-initial-job-layout.jpeg)

The **Execution Results** show no errors:

![](/images/pdi-job-executor-result.jpeg)

But on inspecting the log we can see an error message:

![](/images/pdi-job-executor-log.jpeg)

Now that is interesting! So when there is a problem in the slave job, our **Job Executor** step just happily carries on ... not quite what we want.

Luckily, the **Job Executor** step (as well as the related **Transformation Executor** step) has an optional error output, which will allow us to take further action:

1. Let's add a **Dummy**, **Filter**, **Abort** and another **Dummy** step to our master transformation. 
2. Connect the **Job Executor** step with the first **Dummy** step and choose **This output will contain the execution results** from the stream options. 
3. Set the **Filter** condition to `ExecutionNrErrors > 0`.
4. Point the `true` **Filter** output to the **Abort** step and the `false` output to the last **Dummy** step.

![](/images/pdi-job-executor-error-handling-setup.jpeg)

When we run the master transfromation now, we will see that the **Abort** step on the canvas gets a red highlight as well as the **Abort** entry in the log:

![](/images/pdi-job-executor-error-handling-results.jpeg)

As we will be reusing this logic probably quite a lot, we can be efficient and store it in a dedicated transformation which we call via a **Mapping (sub-transformation)** step from the master transformation.

Our master transfromation looks like this now:

![](/images/pdi-job-executor-error-handling-mapping.jpeg)

And our error-handling transformation looks like this:

![](/images/pdi-job-executor-error-handling-mapping-sub-trans.jpeg)

We can reuse the error handling transformation now with any other **Job Executor** and **Transformation Executor** steps.

> **Note**: For some strange reason it didn't seem possible to connect the **Job Executor** step directly with the **Mapping** step, hence I kept the **Dummy** step still in the master transformation.