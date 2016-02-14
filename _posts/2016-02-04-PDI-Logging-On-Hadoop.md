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
;
```

É violá, we have our very basic logging table which we can extend with additional info if we wanted to.

The process for storing this data looks like this:

`jb_master_wrapper`

This job expects following parameters:

Parameter | Description
----------|------------
`VAR_DI_PROCESS_NAME` | transformation or job name
`VAR_PROPERTIES_FILE` | full path to and name of properties file


Project specific properties file:

[OPEN] I stored it in kettle properties for now for easier testing!

```
VAR_DB_LOGGING_NAME=logs
VAR_TABLE_CUSTOM_PDI_LOGS=di_process_log
VAR_HDFS_DI_LOG_PATH=/user/hive/warehouse/logs.db/di_process_log/di_process_log
VAR_PDI_HDFS_DELIMITER=$[01]
#VAR_HDFS_HOST=localhost
#VAR_HDFS_PORT=8020
#VAR_DB_LOGS_JDBC_URL=jdbc:presto://localhost:8080/hive/logs
#VAR_DB_LOGS_JDBC_DRIVER=com.facebook.presto.jdbc.PrestoDriver
#VAR_DB_LOGS_USER_NAME=a
#VAR_DB_LOGS_PW=a
VAR_HDFS_HOST=localhost
VAR_HDFS_PORT=32775
VAR_DB_LOGS_JDBC_URL=jdbc:impala://localhost:32772/;auth=noSasl
VAR_DB_LOGS_JDBC_DRIVER=org.apache.hive.jdbc.HiveDriver
VAR_DB_LOGS_USER_NAME=cloudera
VAR_DB_LOGS_PW=cloudera
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
sh ./kitchen.sh -file=jb_master_wrapper.kjb
-param:VAR_PROPERTIES_FILE='project.properties' \
-param:VAR_JOB_NAME='jb_sample.kjb' \
-param:VAR_TRANSFORMATION_NAME='' \
> /tmp/jb_sample.err.log
```

