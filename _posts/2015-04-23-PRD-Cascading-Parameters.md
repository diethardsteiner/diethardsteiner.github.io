---
layout: post
title:  "Cascading Parameters in Pentaho Report Designer"
summary: In this article we will discuss how implement cascading parameters in PRD
date:   2015-04-23
categories: PRD
tags: PRD
published: true
---


**Pentaho Report Designer** is quite a powerful tool (in some areas). I personally quite like the dynamic formatting functions. Another area where it shines is query and parameter definition. This time we will take a look at how to implement **cascading parameters**. 

Imagine we want that our end user first pick the year from a drop down list and then PRD displays a dropdown list with quarters in the seleceted year. This is actually very easy to implement in PRD. I assume that you are familiar with **PRD**, so I will not go into any details, but just quickly illustrate the high level idea:

Let's first create the *Year* **Query** using the **SampleData (Memory)** datasource:

```sql
SELECT DISTINCT
     "DIM_TIME"."YEAR_ID"
FROM
     "DIM_TIME"
ORDER BY 1 ASC
```

Next create the *Quarters* **Query**. This time we also reference the `PARAM_YEAR` parameter (which we have not created yet):

```sql
SELECT DISTINCT
     "DIM_TIME"."QTR_NAME"
FROM
     "DIM_TIME"
WHERE "DIM_TIME"."YEAR_ID" = ${PARAM_YEAR}
ORDER BY 1 ASC
```

The idea is that the second query receives the year parameter value, which will be used to source the available quarters.

Now let's define two parameters: `PARAM_YEAR` and `PARAM_QUARTER`. Make sure you define the year parameter before the quarter paramter - otherwise it will not work. Define the first one as `Integer` and the second one as `String`. For both set the **Display Type** to *Drop Down* and select the respective **query** to populate these selects. Also tick **Mandatory**.

You should be in the position to test the cascading parameters now: Just click the **Preview** button. An example can be downloaded from [here](/sample-files/prd/cascading_parameters.prpt).