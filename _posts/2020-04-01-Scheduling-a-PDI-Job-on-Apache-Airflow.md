---
typora-root-url: ..
typora-copy-images-to: ../images/airflow-example
layout: post
title: "Scheduling a PDI job on Apache Airflow"
summary: A tutorial on how to schedule a PDI job via Apache Airflow
date: 2020-04-01
categories: [PDI]
tags: 
published: true
---

[Apache Airflow]() is a very popular solution to **schedule** processes. Kettle/Hop community superstar Dan Keeley wrote an [interesting article](https://dankeeley.wordpress.com/2019/10/07/using-kettle-hop-with-apache-airflow/) on it a few months ago. I highly recommend that you read through his article.

My aim with this article is to just provide a short practical approach to scheduling a Kettle/Hob/PDI job. We will be using a Docker image to get up and running as fast as possible.

## Local PDI Setup

Download **PDI** from [here](https://sourceforge.net/projects/pentaho/files/) and extract it in a convenient location.

We want to **expose** PDI as a **webservice** via [Carte](https://wiki.pentaho.com/display/EAI/Carte+User+Documentation) since we want to able to execute a job from a **Docker container** that runs **Apache Airflow**. Let's start the **Carte server**:

```bash
cd <pdi-root-dir>
./carte.sh localhost 8081
```

> **Note**: We set the port to `8081`. Change it if it is already taken on your machine.

Access the **Carte Web UI** via:

```
http://cluster:cluster@localhost:8081/kettle/status/
```

The default username is `cluster` and password is `cluster`.

The next step is to **execute a PDI job via Carte**. For this clone my **sample repo**:

```bash
# download my sample files
mkdir ~/git && cd ~/git
git clone git@github.com:diethardsteiner/airflow-example.git
```

Let's execute our job - just paste the URL into your **favourite web browser** (adjust URL):

```
http://cluster:cluster@localhost:8081/kettle/executeJob/?job=/Users/diethardsteiner/git/airflow-example/pdi/jobs-and-transformations/job-write-to-log.kjb&level=Basic
```

You should get a **response** similar to this one:

```xml
<webresult>
  <result>OK</result>
  <message>Job started</message>
  <id>52b40353-f674-44d4-b39b-f773eb3d870f</id>
</webresult>
```

Let's extract the `id` from this response to uniquely identify the running job.

Next let's retrieve the **status** and **log** (replace the `id` in the URL below):

```
http://cluster:cluster@localhost:8081/kettle/jobStatus/?name=job-write-to-log&id=52b40353-f674-44d4-b39b-f773eb3d870f
```

If you want to retrieve the response as XML output instead run this (replace the `id` in the URL below):

```
http://cluster:cluster@localhost:8081/kettle/jobStatus/?name=job-write-to-log&id=52b40353-f674-44d4-b39b-f773eb3d870f&xml=Y
```

You should get a response similar to this one:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jobstatus>
  <jobname>job-write-to-log</jobname>
  <id>52b40353-f674-44d4-b39b-f773eb3d870f</id>
  <status_desc>Finished</status_desc>
  <error_desc/>
  <log_date>2020/04/02 19:19:15.420</log_date>
  <logging_string>&lt;![CDATA[H4sIAAAAAAAAALVVz0/CMBS+81d8nkDCYC54kIQYLxq9cODgAUkdW2GFsZK1E0n4430dzKDJJhRp
m7f0ffu+vv569VzP7bjdjuvh5q5n2i0czOXEWadCc0dLJ5Yzcg21n2rIqcHAP3mQaSGTmncSXyQz
8ESnG4xeDQwtQfC4RIZa/69STi1Hms0mnnVdQWVBBD+h+QQiDy70Nwa1FoW/3C8OD/EhfDyIdBrL
ta3oRaZfjgAMhSkrFewt1Z2xZDNmTB4B1cLk39xZyTY/5qObWjdSZDpg74XzmMi3bHvQQYN9O6vY
bJubN8ZarOgAu66xlezqYqZgzaYFJbrNSRgsWtCRby4JT2Q2i9ButxFzjSWHijlfwcdEaCR0uAk6
IxH4pDKV6e8k4Lm9rkUuIfhJynCy4WWCFtfqgFqODKIW5pnSWMsFR7bKVywUIWWFpVCUbOSS64jC
vr+ykbeN+Yj1G2ZBwJU6YQceRSJURDkufw1+qqCRcpXFuj/SacbH1+epluztPw9SnMHLxX6G9Mvh
i4vpfqDaF0bN3NG7BwAA
]]&gt;</logging_string>
  <first_log_line_nr>0</first_log_line_nr>
  <last_log_line_nr>176</last_log_line_nr>
<result><lines_input>0</lines_input>
<lines_output>0</lines_output>
<lines_read>0</lines_read>
<lines_written>0</lines_written>
<lines_updated>0</lines_updated>
<lines_rejected>0</lines_rejected>
<lines_deleted>0</lines_deleted>
<nr_errors>0</nr_errors>
<nr_files_retrieved>0</nr_files_retrieved>
<entry_nr>0</entry_nr>
<result>Y</result>
<exit_status>0</exit_status>
<is_stopped>N</is_stopped>
<log_channel_id/>
<log_text>2020/04/02 19:20:45 -  - =================================
2020/04/02 19:20:45 -  - 
2020/04/02 19:20:45 -  - Oh, just woke up ... did I miss something?!
2020/04/02 19:20:45 -  - 
2020/04/02 19:20:45 -  - =================================
2020/04/02 19:19:15 -  - =================================
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - *** It&#39;s such an exciting day ***
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - *** I am executed via Airflow ***
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - =================================
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  -   _    _                            
2020/04/02 19:19:15 -  -  | |  | |                           
2020/04/02 19:19:15 -  -  | |__| |_   _ _ __ _ __ __ _ _   _ 
2020/04/02 19:19:15 -  -  |  __  | | | | &#39;__| &#39;__/ _` | | | |
2020/04/02 19:19:15 -  -  | |  | | |_| | |  | | | (_| | |_| |
2020/04/02 19:19:15 -  -  |_|  |_|\__,_|_|  |_|  \__,_|\__, |
2020/04/02 19:19:15 -  -                                __/ |
2020/04/02 19:19:15 -  -                               |___/ 
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - Ok, that&#39;s enough ... let me sleep a bit now ...
null</log_text>
<elapsedTimeMillis>0</elapsedTimeMillis>
<executionId/>
</result></jobstatus>
```

There is no way to stream the log messages via the **Carte API**, so what we have to come up with a way to check for the status and once the job is finished get the whole log.

Let's execute the job again, this time from the **Command Line**:

```bash
curl "http://cluster:cluster@localhost:8081/kettle/executeJob/?job=/Users/diethardsteiner/git/airflow-example/pdi/jobs-and-transformations/job-write-to-log.kjb&level=Basic"
```

Next we try to extract the `id` of the result message. We also add the `-s` argument to the `curl` command so that it doesn't print the progress/status message. We will use the handy [XMLStarlet](http://xmlstar.sourceforge.net/doc/UG/xmlstarlet-ug.html) command line utility to extract the value of the `id` field. If you don't have XMLStarlet installed, follow the install instructions on their website first.

```bash
curl -s "http://cluster:cluster@localhost:8081/kettle/executeJob/?job=/Users/diethardsteiner/git/airflow-example/pdi/jobs-and-transformations/job-write-to-log.kjb&level=Basic" | xml sel -t -m '/webresult/id' -v . -n
```

Next we want to pass the job id value on to the `jobStatus` API call and extract the job status:

```bash
unset PDI_JOB_ID
PDI_JOB_ID=$(curl -s "http://cluster:cluster@localhost:8081/kettle/executeJob/?job=/Users/diethardsteiner/git/airflow-example/pdi/jobs-and-transformations/job-write-to-log.kjb&level=Basic" | xml sel -t -m '/webresult/id' -v . -n)

echo "The PDI job ID is: " ${PDI_JOB_ID}

curl -s "http://cluster:cluster@localhost:8081/kettle/jobStatus/?name=job-write-to-log&id=${PDI_JOB_ID}&xml=Y" | xml sel -t -m '/jobstatus/status_desc' -v . -n
```

At this stage it might be a bit tricky to see how the **log element** is nested. **XMLStarlet** comes to the rescue. We can easily display the paths to all elements:

```bash
% curl -s "http://cluster:cluster@localhost:8081/kettle/jobStatus/?name=job-write-to-log&id=${PDI_JOB_ID}&xml=Y" | xml el                          
jobstatus
jobstatus/jobname
jobstatus/id
jobstatus/status_desc
jobstatus/error_desc
jobstatus/log_date
jobstatus/logging_string
jobstatus/first_log_line_nr
jobstatus/last_log_line_nr
jobstatus/result
jobstatus/result/lines_input
jobstatus/result/lines_output
jobstatus/result/lines_read
jobstatus/result/lines_written
jobstatus/result/lines_updated
jobstatus/result/lines_rejected
jobstatus/result/lines_deleted
jobstatus/result/nr_errors
jobstatus/result/nr_files_retrieved
jobstatus/result/entry_nr
jobstatus/result/result
jobstatus/result/exit_status
jobstatus/result/is_stopped
jobstatus/result/log_channel_id
jobstatus/result/log_text
jobstatus/result/elapsedTimeMillis
jobstatus/result/executionId
```

So now we see that we have to fetch `jobstatus/result/log_text`:

```bash
% curl -s "http://cluster:cluster@localhost:8081/kettle/jobStatus/?name=job-write-to-log&id=${PDI_JOB_ID}&xml=Y" | xml sel -t -m 'jobstatus/result/log_text' -v . -n
2020/04/02 19:20:45 -  - =================================
2020/04/02 19:20:45 -  - 
2020/04/02 19:20:45 -  - Oh, just woke up ... did I miss something?!
2020/04/02 19:20:45 -  - 
2020/04/02 19:20:45 -  - =================================
2020/04/02 19:19:15 -  - =================================
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - *** It's such an exciting day ***
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - *** I am executed via Airflow ***
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - =================================
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  -   _    _                            
2020/04/02 19:19:15 -  -  | |  | |                           
2020/04/02 19:19:15 -  -  | |__| |_   _ _ __ _ __ __ _ _   _ 
2020/04/02 19:19:15 -  -  |  __  | | | | '__| '__/ _` | | | |
2020/04/02 19:19:15 -  -  | |  | | |_| | |  | | | (_| | |_| |
2020/04/02 19:19:15 -  -  |_|  |_|\__,_|_|  |_|  \__,_|\__, |
2020/04/02 19:19:15 -  -                                __/ |
2020/04/02 19:19:15 -  -                               |___/ 
2020/04/02 19:19:15 -  - 
2020/04/02 19:19:15 -  - Ok, that's enough ... let me sleep a bit now ...
```

> **Note**: The log text will be only available once the execution of the job is finished.

So now we've got the basics ready - we shall write a simple script now.

A PDI job can have following **status**:

- Running
- Finished
- Finished (with errors)

A very basic script could look like this (`execute-job-via-carte.sh`):

```bash
#!/bin/bash

# vars that should be passed as parameter to this script at some point
CARTE_USER=cluster
CARTE_PASSWORD=cluster
CARTE_HOSTNAME=host.docker.internal
CARTE_PORT=8081
PDI_JOB_PATH=/Users/diethardsteiner/git/airflow-example/pdi/jobs-and-transformations/job-write-to-log.kjb
PDI_LOG_LEVEL=Basic
SLEEP_INTERVAL_SECONDS=5

# local vars
set PDI_JOB_ID
set PDI_JOB_STATUS
CARTE_SERVER_URL=http://${CARTE_USER}:${CARTE_PASSWORD}@${CARTE_HOSTNAME}:${CARTE_PORT}

# start PDI job and get job id
PDI_JOB_ID=$(curl -s "${CARTE_SERVER_URL}/kettle/executeJob/?job=${PDI_JOB_PATH}&level=${PDI_LOG_LEVEL}" | xmlstarlet sel -t -m '/webresult/id' -v . -n)

echo "The PDI job ID is: " ${PDI_JOB_ID}

function getPDIJobStatus {
  curl -s "${CARTE_SERVER_URL}/kettle/jobStatus/?name=job-write-to-log&id=${PDI_JOB_ID}&xml=Y" | xmlstarlet sel -t -m '/jobstatus/status_desc' -v . -n
}

function getPDIJobFullLog {
  curl -s "${CARTE_SERVER_URL}/kettle/jobStatus/?name=job-write-to-log&id=${PDI_JOB_ID}&xml=Y" | xmlstarlet sel -t -m 'jobstatus/result/log_text' -v . -n
}

PDI_JOB_STATUS=$(getPDIJobStatus)

# loop as long as the job is running
while [ ${PDI_JOB_STATUS} = "Running" ]
do
  PDI_JOB_STATUS=$(getPDIJobStatus)
  echo "The PDI job status is: " ${PDI_JOB_STATUS}
  echo "I'll check in ${SLEEP_INTERVAL_SECONDS} seconds again"
  # check every x seconds
  sleep ${SLEEP_INTERVAL_SECONDS}
done 

# get and print full pdi job log
echo "The PDI job status is: " ${PDI_JOB_STATUS}
echo "Printing full log ..."
echo ""
echo $(getPDIJobFullLog)
```

This script will **execute a job** and check its **status** every 5 seconds while it's running. Once it's finished, it will print out the full **log**. Of course we should improve this script further, but for the purpose of this exercise it should do for now.



## Airflow via Docker Container

We will use an existing `Dockerfile` from [puckel](https://github.com/puckel/docker-airflow) but add the **XMLStarlet** dependency:

```bash
git clone https://github.com/puckel/docker-airflow.git
cd docker-airflow
vi Dockerfile
```

Change the end of `RUN` instruction:

In line 34 add (just after `&& apt-get upgrade -yqq \`):

```
&& apt-get install -y xmlstarlet \
```

Next let's **build** the **Docker image**:

```
docker build --rm -t ds/docker-airflow .
```

Let's start the **Docker container**:

```bash
# create docker container with volumes
docker run -d -p 8080:8080 \
  --name airflow-webserver \
  -v ~/git/airflow-example/airflow/:/usr/local/airflow/ \
  -v ~/git/airflow-example/pdi/:/usr/local/pdi/ \
  ds/docker-airflow webserver
# web ui will be available on http://localhost:8080/admin/
# check for running containers
docker ps
# jump into your running container’s command line
docker exec -it airflow-webserver bash
```

Let's make sure we can **connect** from **within the container** to **Carte** running on the **host machine**. You can reach the host IP from within the Docker container via `host.docker.internal`.  Your milage might vary here - I've only tested this on MacOS (see for more info [here](https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach)):

```bash
curl http://cluster:cluster@host.docker.internal:8081/kettle/status
```

If no connection error is returned, we are ready to progress.

Let's check that our schedule/DAG is valid:

Validate **syntax**:

```
cd dags
python kettle-test-schedule.py
```

If nothing is returned, all is good.

Let’s run a few commands to validate this script further.

```bash
# print the list of active DAGs
airflow list_dags

# prints the list of tasks the "KettlePrintLog" dag_id
airflow list_tasks KettlePrintLog

# prints the hierarchy of tasks in the KettlePrintLog DAG
airflow list_tasks KettlePrintLog --tree
```

Next let's do a **dryrun** for the first task of our DAG - this is very useful:

```bash
airflow test KettlePrintLog KettleWriteToLog 2020-03-03
```

Open [the web UI](http://localhost:8080/admin/) and you should see the **DAG**:

![](/images/airflow-example/airflow-1.png)

Our **DAG Python script** looks like this:

```python
from airflow import DAG
from airflow.operators.bash_operator import BashOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime(2020, 3, 1),
    'email': ['airflow@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG('KettlePrintLog', default_args=default_args, schedule_interval=timedelta(days=1))

t1 = BashOperator(
    task_id='KettleWriteToLog',
    bash_command='/usr/local/pdi/shell-scripts/execute-job-via-carte.sh ',
    dag=dag)
```

> **Note**: There has to be a **space** after the shell command (see `bash_command` line) otherwise the process will fail. The reason for this is explained [here](https://cwiki.apache.org/confluence/display/AIRFLOW/Common+Pitfalls).



I won't go into detail what these options all mean ... please consult [the official documentation](https://airflow.apache.org).

The important bit here is that since we mapped our local git repo via a **volume** to the **Docker container** it is available immediately and we can just easily reference it (see `bash_command` line).

You have two options now to actually run the process: either via the command line in the **Docker container** or using the **Web UI**. We will do the latter for now: Just toggle the On/Off button next to the DAG name:

![](/images/airflow-example/airflow-2.png)

We can also execute the job manually - In the **Links** section click on **Trigger Dag**:

![](/images/airflow-example/airflow-3.png)

Go ahead and explore the interface. E.g. Try to find the log of our process:

![](/images/airflow-example/airflow-4.png)

Please note that what we've produced so far is not the final solution - there is still quite some work to do to make this suitable for a production environment, however, I hope to got an idea on how to **schedule** a **PDI** process via **Apache Airflow**. Granted this was an extremely simple example we went through, however, it should give you a starting point to create more complex schedules.