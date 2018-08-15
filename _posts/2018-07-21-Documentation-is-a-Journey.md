---
layout: post
title: "Documentation is a Journey"
summary: This article explains how to encourage developers to write documentation
date: 2018-07-21
categories: Documentation
tags: Documentation
published: true
---

In a lot of projects that I’ve worked on writing **documentation** is usually the most neglected part. To a lot of **developers** writing documentation is an afterthought and more often then not omitted. This is **bad practice**. 

As we approach a new **user story**, there are a lot of things that we **learn** along the way (e.g. how the data is structured, what to consider when querying the data, data quality problems, business rules etc). All of this is important information that should be written down (ideally in a Wiki/Confluence) so that QA testers or any other developers who have to take over your work later on have all the required info. It’s also beneficial for your own sanity to have these learning written down - not just on your own piece of paper but on a **central Wiki** that any of your colleagues can access.

**Documentation** is an **ongoing process**! As you learn about new facts, you should write them down straight away. You must not leave documentation to the very end - you’d have forgotten a lot of the details by then. 

> **Make it a habit**: As you start working on a **user story**, always try to write down essential learnings. Make documentation part of your daily work! It will feel less of a burden and you'll realise that writing process is much easier since the thoughts are still fresh in your head!

I’ve lately introduced a **new approach** with my team that seems to have helped my team with documentation. It kind of encourages everyone to write documentation throughout the implementation of a new user story. The idea is writing documentation is **not a one-off task** but a **continuous journey** - from the beginning of finding out all the details of a user story to the end (acceptance testing, live deployment, handover to support). We always add to the documentation as we work on our user story.

The **proposed outline of a documentation** for a Wiki page is as follows:

# User Story

As a campaign manager I want to see the response rate ...

# Acceptance Criteria

- Dashboard showing the reponse rate by campaign
- Data is refreshed every 5 min
- ...

# Business rules

The response rate is calculated this way: `(reponses / recipients) * 100`

# Notes
In the initial meeting Mr Summer expressed that there are complications with source data ...



# Required Attributes
To fulfil this user story, we need the following data points:

- response time
- id of person responding
- ...

# Source Data

## Database

| name         | description
| ------------ |------------------
| `cmpgn`      | main campaign database holding details on recipients, responses 



## Tables

| database |   table        | granularity | description
| -------- | -------------- |--------------|--------------
| `cmpgn`  |   `table1` | `field1` , `field2`, `field3` | This table holds ...

## Join Conditions

Show E/R diagram

## Columns

| database |   table        | column | description
| -------- | -------------- |--------------|--------------
| `cmpgn`  |   `recipients` | `field1`  | This field captures ...  

## Source Data Quality Problems

Mention any issues that you found when analysing the data and any issues you were told about.

## Parameters


| Parameter | Description
|-----------|-------------
| ... | ...

# Implementation Details

## Artefacts

| Artefact | Description
|----------|------------------
| `[CODE-REPO]/sql/cmpgn_reponses.sql` | Holds the main logic to source and combine the data. We exclude ... because ... . This was necessary due to ... . We further narrow the result down by constraining on ... . This ensures that ... . Finally we group by ... .
| `[CODE-REPO]/etl/cmpgn/cmpgn_main.kjb` | This job runs the above mentioned sql script and ... .
| `[ENV-CONFIG-REPO]/shell-scripts/run_cmpgn_main.sh` | This shell script can be used to execute the process.
| `[ENV-CONFIG-REPO]/properties/cmpgn_main.properties` | ...

## Generated Tables

This process creates following **tables**:

| database |   table        | granularity | description
| -------- | -------------- |--------------|--------------
| `staging`  |   `tmp_reponses`  | `field1` , `field2`, `field3` | This table holds ...

## Data Flow Diagram

Please find below a simplified diagram depicting the flow of data from table to table:


