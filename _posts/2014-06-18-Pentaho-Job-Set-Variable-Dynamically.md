---
layout: post
title:  "Setting a variable value dynamically in a Pentaho Data Integration job"
summary: This article explains an easy way to define a variable in a Pentaho Kettle job
date:   2014-06-18
categories: PDI
tags: Pentaho PDI
published: true
---

On some occasions you might have to set a variable value dynamically in a job so that you can pass it on to the **Execute SQL Script** job entry in example. In this blog post we will take a look at how to create an integer representation of the date of 30 days ago. And we want to achieve this without using an additional transformation!

The way to achieve this in a simple fashion on the **job** level is to use the **Evaluate JavaScript** job entry \[[Pentaho Wiki](http://wiki.pentaho.com/display/EAI/Evaluating+conditions+in+The+JavaScript+job+entry)\]. While this job entry is not really intended to do this, it currently offers the easiest way to accomplish just this. Just add this *job entry* to your Kettle job and paste the following JavaScript:

```javascript
date = new java.util.Date();
date.setDate(date.getDate()-30); //Go back 30 full days
var date_tk_30_days_ago = new java.text.SimpleDateFormat("yyyyMMdd").format(date);
parent_job.setVariable("VAR_DATE_TK_30_DAYS_AGO", date_tk_30_days_ago);
true; // remember that this job entry has to return true or false
```

To test this let's add a **Log** job entry:

![](/images/pentaho-job-set-variable-dynamically-1.png)

Add this to the log message to the job entry settings:

```
The date 30 days ago was: ${VAR_DATE_TK_30_DAYS_AGO}
```

And then run the job. You should see something similar to this:

![](/images/pentaho-job-set-variable-dynamically-2.png)

Certainly you could just pass the value as parameter from the command line to the job, but on some occasions it is more convenient to create the value dynamically inside the job.

Software used:
- pdi-4.4.0-stable

