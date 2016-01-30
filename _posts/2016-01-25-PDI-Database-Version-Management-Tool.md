---
layout: post
title: Database Version Management (DVM) Process Powered By Pentaho Data Integration
summary: 
categories: PDI
tags: PDI
published: true
---

# Database Version Management (DVM) Process Powered By Pentaho Data Integration

When creating a **data integration process**, we quite often have to create of some database tables as well to store intermediate or final data. We want to easily deploy the data integration process (including the tables) into a new environment (test, production etc) and also apply any new changes in an easy fashion once the project is live.

In regards to table definitions there are a few **open source database version management tools** out there which get the job done, however, quite a lot of them only **support specific databases**. Now this is kind of bad if you have to be flexible as with every new project you might be dealing with a different database. I have covered a few of theses tools in the past, but let's see if we can come up with something else:

So what do I want this tool to primarily do:

- Run **DDL** statements in a given order: Usually you start off with various `CREATE TABLE` statements, but throughout the project you might also have to apply changes, e.g. `ALTER TABLE`, `DROP TABLE` etc.
- Keep a log of the executed SQL scripts so that only new scripts are run on next execution.

Well, this isn't actually that complicated, is it? Our good old trusted **Pentaho Kettle** (aka **PDI**, Pentaho Data Integration) comes to mind to handle this challenge. We can use any **JDBC driver** with it, which basically allows us to connect to any database out there! And there is another big benefit straight away: It is the same tool that we use to create our data integration processes, so there is nothing new to be learnt! So how do we create this process?

I will briefly sketch out the process out here (and will not go into any details). You can download the files from [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/pdi/database-versioning-tool) to understand the process even better.

`jb_dvm_master.kjb`:

![](/images/jb_dvm_master.png)

The master job `jb_dvm_master.kjb` creates the **DVM log file** (which will store a list of successfully executed DDL SQL files) and starts the main transformation `tr_dvm_execute_new_dll_scripts.png`:

![](/images/tr_dvm_execute_new_dll_scripts.png)

This transformation checks a given folder for **SQL DLL files** and compares them to the already processes files from the **DVM log**. Only the new files remain in the stream and the list of filenames gets sorted by the incremental file number (version). The process writes the names of all these new files to the log and then loops over this list using the **Job Executor** step, which in turn executes `jb_dvm_execute_new_ddl_script`:

![](/images/jb_dvm_execute_new_ddl_script.png)

For each new **DDL SQL file** the `jb_dvm_execute_new_ddl_script` job executes the **SQL script** and upon successful execution writes the filename to the **DVM log** via the `tr_dvm_write_to_log` transformation:

![](/images/tr_dvm_write_to_log.png)


Now that we have implemented most features, it is time to test the process. Let's fire up our local PostgreSQL server and populate the shell file `run_jb_dvm_master` with the required parameters:

```bash
cd /Applications/Development/pdi-ce-6.0

sh ./kitchen.sh -file=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/jb_dvm_master.kjb \
-param:VAR_DB_DVM_JDBC_DRIVER_CLASS_NAME=org.postgresql.Driver \
-param:VAR_DB_DVM_JDBC_URL=jdbc:postgresql://localhost:5432/test \
-param:VAR_DB_DVM_PW=postgres \
-param:VAR_DB_DVM_USER=postgres \
-param:VAR_DDL_FILES_DIR=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/ddl \
-param:VAR_PROCESSED_DDL_FILES_LOG=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/processed/processed.csv \
> /tmp/jb_dvm_master.err.log
```

Let's create a few simple **DDL SQL** files:

`0_create_dma_schema.sql`:

```sql
CREATE SCHEMA dma;
```

`1_create_dim_date.sql`:

```sql
CREATE TABLE dma.dim_date (
  date_tk INT
  , the_year INT
  , the_month INT
  , the_date DATE
)
;
```


And then let's run it:

```bash
$ chmod 700 run_jb_dvm_master.sh
$ ./run_jb_dvm_master.sh
```

 Let's follow the log:
 
 ```bash
 $ tail -fn 200 /tmp/jb_dvm_master.err.log
 ```
 
Once the process finished successfully, we should have this in the **DVM** log:
 
```bash
$ cat processed/processed.csv
0_create_dma_schema.sql
1_create_dim_date.sql
```

This shows you the **list of SQL DLL scripts** which have been applied successfully to the specified database.


Ok, you might be still doubtful, so let's check what `psql` has to tell us:

```bash
$ psql -Upostgres -dtest
psql (9.4.1)
Type "help" for help.

test=# \dt dma.*
          List of relations
 Schema |   Name   | Type  |  Owner   
--------+----------+-------+----------
 dma    | dim_date | table | postgres
(1 row)

test=# \dt dma.dim_date
          List of relations
 Schema |   Name   | Type  |  Owner   
--------+----------+-------+----------
 dma    | dim_date | table | postgres
(1 row)

test=# \d dma.dim_date
       Table "dma.dim_date"
   Column    |  Type   | Modifiers 
-------------+---------+-----------
 date_tk     | integer | 
 the_year    | integer | 
 the_month   | integer | 
 the_date    | date    |
```

Ok, this looks pretty good. Now let's assume we want to add a new column to the existing table. We create a new SQL file and prefix the name with the sequence number:

`2_dim_date_add_quarter.sql`:

```sql
ALTER TABLE dma.dim_date ADD COLUMN the_quarter INT;
```

Let's run the process again:

```bash
$ ./run_jb_dvm_master.sh
```

```bash
$ cat processed/processed.csv
0_create_dma_schema.sql
1_create_dim_date.sql
2_dim_date_add_quarter.sql
```

> **Note**: Our process did only execute the new file `2_dim_date_add_quarter.sql` and not all of the files.

This simple test demonstrates quite nicely that our process is indeed working as intended. As highlighted before, we can run this process for any database which offers a **JDBC driver**. We only have to adjust the connection details and add the **JDBC driver** to PDI's lib folder.

When you deploy your project to a **new environment**, you can run this process upfront, so that all the required tables are in place for your **data integration process**. Should there be the **need to apply changes over time**, you can test them first in your test environment and deploy them to production in an easy fashion. And of course you store all your files on **Git** or a similar version control system, so you can sync files easily between environments.


