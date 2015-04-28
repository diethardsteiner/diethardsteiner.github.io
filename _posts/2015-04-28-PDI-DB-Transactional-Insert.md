---
layout: post
title:  "Pentaho Data Integration: DB Rollback on Error"
summary: In this article we will discuss how implement a transactional behaviour in PDI
date:   2015-04-28
categories: PDI
tags: PDI
published: true
---

When inserting data into a database table a often requested behaviour is that the ETL fails on error and the whole transaction gets rolled back.

In standard mode, the **Table Output** step in **Pentaho Data Integration** will insert and commit any records until an error occurs. So imagine the ETL process processes 10,000 records and the 8,999th record has a string field which is too long (e.g. the DDL defines the field as `VARCHAR(20)` but it is actually 122 characters long), the ETL process will come to a sudden halt. Now the problem is though, that we have 8,998 records in the DB table from this current ETL run. 

In some situations this might not be an issue, e.g. if you have a **CDC** process set up which can deal with this (e.g. it can just start off the highest id or timestamp), but in other scenarios, this situation might be a problem.

Luckily, **Pentaho Data Integration** provides a transactional feature, which is really quite easy to use:

1. Make sure your DB engine supports transactions! 
E.g. **MySQL MyISAM** does not support transactions. So the transformation will run and insert records even though there was an error.

2. Go to **Transformation Settings > Misc**: 
Tick **Make the transformation database transactional**. 

![](/images/pdi-db-transactional-insert.png)

> **Note**: This will disable the **Use batch update for insert** option in the **Table Output** step also ignore the **Commit Size** setting. So effectively this will slow down the insert operation.

As you can see, this is really quite easy to setup and might come in hand in a few use cases.

[Source1](http://www.ibridge.be/?p=93)