---
layout: post
title:  "Automated testing of data processes: Part 2"
summary: This article explains how to set up an environment for automatically testing data integration processes
date: 2020-01-20
categories: CI
tags: PDI
published: true
---

This article focuses on generating the **junit** compatible test report that can be consumed by a **CI Server**.

We collect the result and log of our tests with a dedicated process and then generate the **XML** based test report. The related junit5 **XSD Schema** can be found [here](https://github.com/junit-team/junit5/blob/master/platform-tests/src/test/resources/jenkins-junit.xsd). 

The XML document starts with a `testsuite` tag containing some summary info:

```xml
<testsuite name="TestSuitName" tests="NumberOfTests" failures="ErrorCount" skipped="0" errors="0">

</testsuite>
```


Inside the `testsuite` tag we can have several test cases which are constructed like so:

In case the test is a **success**:

```xml
  <testcase name="TestName" classname="ClassName" package="PackageName" time="ExecutionTimeSeconds">
    <system-out>
      <![CDATA[ 
        Execution Log Text 
      ]]>
    </system-out>
  </testcase>
```


In case the test result is an **error**:


```xml
  <testcase name="TestName" classname="ClassName" package="PackageName" time="ExecutionTimeSeconds">
    <failure type="value" message="ERROR Found"/> <!-- ONLY THIS BIT IS DIFFERENT -->
    <system-out>
      <![CDATA[ 
        Execution Log Text 
      ]]>
    </system-out>
  </testcase>
```

What follows is a sample implementation with Kettle/PDI/Project Hob. I'd guess that there are many implementations of this already around. The one shown below is mainly based on what Beppe Raymaeker provided for Hitachi Vantara's official recommendation:

- [Framework Repo](https://github.com/braymaekers/framework)
- [Example Code Repo](https://github.com/braymaekers/sales_dwh) 
- [Example Config Repo](https://github.com/braymaekers/sales_dwh-configuration)

You can download the example yourself and go through it, so I will discuss it only on a high level here:

![](/images/pdi-gitlab-jenkins/pdi-gitlab-jenkins-7.png)

This PDI process (transformation) will load all the tests defined in the `test-suite.xml` file and then execute each one of them: Each test can define a PDI job or PDI transformation as the test subject. In the below screenshot we can see the sub-transformation which actually executes the job-to-be-tested:

![](/images/pdi-gitlab-jenkins/pdi-gitlab-jenkins-8.png)

All the execution stats get collected and propagated to the parent job. Once all tests are executed, the parent job constructs the **JUnit based test report** for all the tests.
