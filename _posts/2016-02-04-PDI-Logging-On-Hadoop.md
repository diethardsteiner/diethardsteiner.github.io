---
layout: post
title:  "Creating DI Execution Logs for Pentaho Data Integration on Hadoop"
summary: This article explains how to set up a simple custom PDI logging framework for Hadoop
date: 2016-02-04
categories: Big Data
tags: Big Data, PDI
published: false
---

As many of you will know, **Hadoop** historically did not support **updates**. Recently, however, support for **updates** was added to **Hive**, but don't expect balzing fast performance from it.

If you are using **Pentaho Data Integration** on Hadoop, chances are that you want to store the execution logs somewhere. The problem is though that the out-of-the-box execution logging only supports relational databases (as it requires **updates**). You could also store the standard text logs directly on Hadoop, but let's come up with a nicer solution: I quite like the idea of storing all the data in one place, so I do not want to opt for a relational database but keep everything on **Hadoop**. Plus **Impala** provides a very fast way to query this data.


The strategy is very simple: We just want to store the following basic info (and can possibliy extend on this later):

field name | description
-----------|------------
| `di_process_id` | As there is currently no built-in support for auto-incremented ids in Hive nor in Impala, we will just generate a **UUID**.
| `di_process_name` | The name of the job or transformation that is being executed
| `di_process_status` | Will be set to `running`, `error` or `finished`.
| `creation_time` | The time this very record was created. 


You should have a local Hadoop dev environment set up (be it vanilla Apache Hadoop, CDH, HDP, MapR etc). If you don't have such an environment available or do not want to set one up, you can also use **PostgreSQL** or similar (any relational DB with **foreign data wrapper** support). For those ones that want to use **PostrgreSQL** instead, find some special instructions at the very end of this article.

Which means the DDL for our table should look like this:

```sql
CREATE DATABASE logs;
CREATE TABLE logs.di_process_log
(
  di_process_id STRING
  , di_process_name STRING
  , di_process_status STRING
  , creation_time TIMESTAMP
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY '001'
STORED AS TEXTFILE
;
```

For each execution of a **data integration** process, we will store two records in this table. It's not very nice to read the data this way: Luckily, both **Impala** and **Hive** support **windowing** functions, so we can condense this data a bit for better readibiliy:

```sql
CREATE VIEW logs.vw_di_process_log AS
SELECT
  di_process_id
  , di_process_name
  , current_status AS di_process_status
  , start_time
  , end_time
  , COALESCE(
      ( UNIX_TIMESTAMP(end_time) - UNIX_TIMESTAMP(start_time) ) / 60
      , ( UNIX_TIMESTAMP() - UNIX_TIMESTAMP(start_time) ) / 60
    ) AS duration_minutes
FROM 
(
  SELECT
    di_process_id
    , di_process_name
    , di_process_status
    , COALESCE(
        LEAD(di_process_status, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time)
        , di_process_status
      ) AS current_status
    , creation_time AS start_time
    , LEAD(creation_time, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time) AS end_time
  FROM logs.di_process_log  
) a
WHERE  
  /* keep first record only */
  di_process_status = 'running'
ORDER BY start_time DESC
;
```

Equivalent query for **Presto**:

```sql
SELECT
  di_process_id
  , di_process_name
  , current_status AS di_process_status
  , start_time
  , end_time
  , COALESCE(
      ( TO_UNIXTIME(end_time) - TO_UNIXTIME(start_time) ) / 60
      , ( TO_UNIXTIME(CURRENT_TIMESTAMP) - TO_UNIXTIME(start_time) ) / 60
    ) AS duration_minutes
FROM
(
  SELECT
    di_process_id
    , di_process_name
    , di_process_status
    , COALESCE(
        LEAD(di_process_status, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time)
        , di_process_status
      ) AS current_status
    , creation_time AS start_time
    , LEAD(creation_time, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time) AS end_time
  FROM logs.di_process_log  
) a
WHERE  
  /* keep first record only */
  di_process_status = 'running'
ORDER BY start_time DESC
;
```

This view basically turns our initial records:

di_process_id | di_process_name | di_process_status | creation_time
--------------|-----------------|-------------------|---------------
fa3a55ac-d341-11e5-8cca-29f865b7b98b | sample job | running | 2016-02-14 17:40:02.913
fa3a55ac-d341-11e5-8cca-29f865b7b98b | sample job | error | 2016-02-14 17:40:02.953

into a flat representation:

di_process_id | di_process_name | di_process_status | start_time | end_time | duration_minutes
--------------|-----------------|-------------------|------------|----------|-------------------
fa3a55ac-d341-11e5-8cca-29f865b7b98b | sample job | error | 2016-02-14 17:40:02.913 | 2016-02-14 17:40:02.953 | 00:00:00.04


É violá, we have our very basic logging table which we can extend with additional info if we wanted to.

The process for storing this data looks like this:

`jb_master_wrapper`

This job expects following parameters:

Parameter | Description
----------|------------
`VAR_DI_PROCESS_NAME` | Pretty print name of transformation or job
`VAR_PROPERTIES_FILE` | Full path to and name of properties file
`VAR_DI_PROCESS_FILE_PATH` | Full path to and name of ktr or kjb file you want to execute


Project specific properties file:

```properties
VAR_DB_LOGGING_NAME=logs
VAR_TABLE_CUSTOM_PDI_LOGS=di_process_log

# Setup for vanilla Apache Hadoop and Presto
# VAR_HDFS_HOST=localhost
# VAR_HDFS_PORT=8020
# VAR_DB_LOGS_JDBC_URL=jdbc:presto://localhost:8080/hive/logs
# VAR_DB_LOGS_JDBC_DRIVER=com.facebook.presto.jdbc.PrestoDriver
# VAR_DB_LOGS_USER_NAME=a
# VAR_DB_LOGS_PW=a
# VAR_TABLE_REQUIRES_REFRESH=N
# VAR_DI_LOG_FILE_PATH=/user/hive/warehouse/logs.db/di_process_log/di_process_log.txt
# VAR_PDI_TEXT_DELIMITER=$[01]
# VAR_PDI_TEXT_ENCLOSURE=

# Setup for CDH Quickstart
VAR_HDFS_HOST=localhost
VAR_HDFS_PORT=32775
VAR_DB_LOGS_JDBC_URL=jdbc:impala://localhost:32772/;auth=noSasl
VAR_DB_LOGS_JDBC_DRIVER=org.apache.hive.jdbc.HiveDriver
VAR_DB_LOGS_USER_NAME=cloudera
VAR_DB_LOGS_PW=cloudera
VAR_TABLE_REQUIRES_REFRESH=Y
VAR_DI_LOG_FILE_PATH=/user/hive/warehouse/logs.db/di_process_log/di_process_log.txt
VAR_PDI_TEXT_DELIMITER=$[01]
VAR_PDI_TEXT_ENCLOSURE=
```

`tr_check_process_running`:

```sql
SELECT
  COUNT(*) AS no_of_processes_running
FROM logging.vw_di_process_log
WHERE
  di_process_name = '${VAR_DI_PROCESS_NAME}'
  AND di_process_status = 'running'
```

Query for database which do not support views or querying from views (this one is Presto specific):

```sql
SELECT
  COUNT(*) AS no_of_processes_running
FROM
(
  SELECT
    di_process_id
    , di_process_name
    , current_status AS di_process_status
    , start_time
    , end_time
    , COALESCE(
        ( TO_UNIXTIME(end_time) - TO_UNIXTIME(start_time) ) / 60
        , ( TO_UNIXTIME(CURRENT_TIMESTAMP) - TO_UNIXTIME(start_time) ) / 60
      ) AS duration_minutes
  FROM
  (
    SELECT
      di_process_id
      , di_process_name
      , di_process_status
      , COALESCE(
          LEAD(di_process_status, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time)
          , di_process_status
        ) AS current_status
      , creation_time AS start_time
      , LEAD(creation_time, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time) AS end_time
    FROM logs.di_process_log  
    WHERE
      di_process_name = '${VAR_DI_PROCESS_NAME}'
  ) a
  WHERE  
    /* keep first record only */
    di_process_status = 'running'
) b
WHERE
  di_process_status = 'running'
;
```

`tr_log_process_as_running`:


We set the variable `VAR_DI_PROCESS_ID` so that it is available for all the other sub-jobs and transformation. We will need it later on when we log the next status.

`tr_log_process_as_x`:

This transformation is very flexible: It accepts as parameters the process name as well as the process status. The process id value is retrieved from the `VAR_DI_PROCESS_ID` we set earlier on in the `tr_log_process_as_running` transformation.

```bash
#!/bin/bash

# simulating properly set up env
export PDI_ROOT_DIR=/Applications/Development/pdi-ce-6.0.1
export PROJECT_ROOT_DIR=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/logging-on-hadoop

cd $PDI_ROOT_DIR

sh ./kitchen.sh -file=$PROJECT_ROOT_DIR/di/jb_master_wrapper.kjb \
-param:VAR_PROPERTIES_FILE=$PROJECT_ROOT_DIR/config/project.properties \
-param:VAR_DI_PROCESS_NAME='sample job' \
-param:VAR_DI_PROCESS_FILE_PATH=$PROJECT_ROOT_DIR/di/jb_sample.kjb \
> /tmp/jb_sample.err.log
> ```

# Special Case: PostgreSQL for testing the framework

For those wanting to go down the **PostgreSQL** route, I should quickly provide the setup for the foreign data wrapper (more detailed info on [this excellent blog post](http://www.postgresonline.com/journal/archives/250-File-FDW-Family-Part-1-file_fdw.html)):

```sql
CREATE SCHEMA logs;
CREATE EXTENSION file_fdw;
CREATE SERVER file_fdw_server FOREIGN DATA WRAPPER file_fdw;

CREATE FOREIGN TABLE logs.di_process_log
(
  di_process_id VARCHAR(70)
  , di_process_name  VARCHAR(120)
  , di_process_status VARCHAR(20)
  , creation_time TIMESTAMP
)
SERVER file_fdw_server
OPTIONS (
	format 'text'
	, filename '/tmp/di_process_log.txt'
	, delimiter '|'
	, null '\N'
)
;

-- you will have to create the log file at this stage

CREATE VIEW logs.vw_di_process_log AS
SELECT
  di_process_id
  , di_process_name
  , current_status AS di_process_status
  , start_time
  , end_time
  , COALESCE(
      ( end_time - start_time ) 
      , ( NOW() - start_time) 
    ) AS duration_minutes
FROM (
  SELECT
    di_process_id
    , di_process_name
    , di_process_status
    , COALESCE(
        LEAD(di_process_status, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time)
        , di_process_status
      ) AS current_status
    , creation_time AS start_time
    , LEAD(creation_time, 1) OVER (PARTITION BY di_process_id ORDER BY creation_time) AS end_time
  FROM logs.di_process_log  
) a
WHERE  
  /* keep first record only */
  di_process_status = 'running'
ORDER BY start_time DESC
;
```
Values for the properties file:

```
# Setup for PostgreSQL
VAR_DB_LOGGING_NAME=logs
VAR_TABLE_CUSTOM_PDI_LOGS=di_process_log
VAR_HDFS_HOST=
VAR_HDFS_PORT=
VAR_DB_LOGS_JDBC_URL=jdbc:postgresql://localhost:5432/test
VAR_DB_LOGS_JDBC_DRIVER=org.postgresql.Driver
VAR_DB_LOGS_USER_NAME=postgres
VAR_DB_LOGS_PW=postgres
VAR_TABLE_REQUIRES_REFRESH=N
VAR_DI_LOG_FILE_PATH=/tmp/di_process_log.txt
VAR_PDI_TEXT_DELIMITER=|
VAR_PDI_TEXT_ENCLOSURE=
```


