---
layout: post
title:  "Setting up a Hadoop Dev Environment for Pentaho Data Integration"
summary: This article explains how to set up a vanilla Hadoop Distribution and configure Pentaho Data Integration to access it
date:   2015-06-06
categories: PDI
tags: PDI, Hadoop
published: true
---

Recently I realized that I hadn't written a blogpost about **Pentaho Data Integration** (Kettle) for a long time, so it's time to focus on this again: 

[Dan Keeley](https://dankeeley.wordpress.com) published an interesting blogpost on installing the **Cloudera Hadoop** distribution some time ago to illustrate a way to test **PDI** with **Hadoop** on an environment with limited resources. 

In this article I'd like to explain how to set up **Vanilla Hadoop** and configure **PDI** for it. For our development evironment we might only need HDFS and Hive. Although setting up Hadoop might sound like an extermely complex task, the reality is that it is usually fairly straight forward (unless you hit a few bugs). We will first install **Apache Hadoop**, then **Apache Hive** and finally configure **Pentaho Data Integration** to access these services. **Pentaho Data Integration** allows you to import data to Hadoop from a variate of sources (databases, text file etc), create **MapReduce jobs** and also export the data to a variaty of destinations, all this via a simple to use GUI (without any coding). All this functionality is available in the **open source** edition! I bet you are really motivated by now, so let's see how this is done: 

# Apache Hadoop and Hive Installation: Minimal Dev Environment

This article focuses on setting up a minimal Hadoop dev environment. There are certainly easier ways, e.g. using one of the VMs supplied by one of the major commercial Hadoop vendors: If you have a machine with **enough memory** this is certainly the most convenient way to go. However, you might not want to sacrifice a vast amount of **RAM** or not have such a high spec machine, so setting up **Hadoop** natively on your machine is the way to go. It might not be the easiest way, but you'll certainly learn a few interesting details on the way - that's what any great journey is about!

If you very interested in Hadoop, I can strongely recommend the excellent book **Hadoop - The Definitive Guide** by Tom White.

## Installing HDFS

### Downloading the required files

You can download the files directly from the [Apache Hadoop](https://hadoop.apache.org/releases.html) website or they might be available via your packaging system (e.g. on Fedora it is available via yum or dnf). The instructions here will mainly follow my setup on **Mac OS X** - your milage may vary.

On **Mac OS X** you can alternatively install Hadoop via **Homebrew**: `brew install hadoop`, in which case all the files will be located in `/usr/local/Cellar/hadoop/<version>`.

I assume you have **JDK** already installed: Check that you are using the correct [supported Java version](http://wiki.apache.org/hadoop/HadoopJavaVersions).

I added the hadoop files under `/Applications/Development/Hadoop`, you can choose any other suitable directory.

Add the following to `~/.bash_profile` (adjust if required):

```
## HADOOP

export HADOOP_HOME=/Applications/Development/Hadoop/hadoop-2.6.0
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
```

`JAVA_HOME` must be set in `~/.bash_profile` already.

Next we have to enable passwordless login:

In regards to **ssh** there is not much to do on **Mac OS X**: Just make sure that you enable **Remote Login** via the Mac OS X **System Settings** > **Sharing**. There is no need to install anything else. **Linux Users** might have to install ssh and openssh-server.

Generate a new **SSH key** with an **empty passphrase**:

```bash
ssh-keygen -t rsa -P '' -f ~/.ssh/hadoop_rsa
cat ~/.ssh/hadoop_rsa.pub >> ~/.ssh/authorized_keys
```

Make sure you add your key via `ssh-add` (required on **Mac OS X**, on Linux only if you are running ssh-agent), e.g.:

```bash
ssh-add ~/.ssh/hadoop_rsa
```

Only this makes the password-less login possible!

Now test if you can login without a password:

```
ssh localhost
```

You should not be prompted for a password!

Next **configure** Hadoop: 

All the config file are located in `<HADOOP_HOME>/etc/hadoop>`.

For **pseudo-distributed** node add this:

`core-site.xml`:

```xml
<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://localhost</value>
  </property>
</configuration>
```

> **Note**: The default port for HDFS (the namenode) will be `8020`. You can however set the port as well in the config above like so: `hdfs://localhost:9000`.

`hdfs-site.xml`:

**Option 1**: data gets stored in `/tmp` directory.

```xml
<configuration>
  <property>
    <name>dfs.replication</name>
    <value>1</value>
  </property>
</configuration>
```

**Option 2**: Dedicated **permanent data directory**. You do not have to create the directories on the file system upfront - it will be created automatically for you.

```xml
<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://localhost</value>
  </property>
  <property>
    <name>dfs.data.dir</name>
    <value>/Users/diethardsteiner/hdfs/datastore/data</value>
  </property>
  <property>
    <name>dfs.name.dir</name>
    <value>/Users/diethardsteiner/hdfs/datastore/name</value>
  </property>
</configuration>
```

Next:

`mapred-site.xml`:

```xml
<configuration>
  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
  </property>
</configuration>
```

And finally:

`yarn-site.xml`:

```xml
<configuration>
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>localhost</value>
  </property>
  <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
  </property>
</configuration>
```

Now we can format the namenode:

```
hdfs namenode -format
```

Pay attention to the last few lines of the log output, where you will find the root directory of your HDFS file system, e.g.: `/tmp/hadoop-diethardsteiner/dfs/name`.

To start **HDFS**, **YARN** and **MapReduce**, run the below commands (this can be run from any directory):

> **IMPORTANT**: Before you start Hadoop, make sure that **HDFS** is formatted. Even after the original setup it might be necessary to do this upfront as the HDFS directory might be located in `/tmp` (and hence gets lost each time you restart your machine).

```
start-dfs.sh
start-yarn.sh
mr-jobhistory-daemon.sh start historyserver
```

Check that the following websites are accessible:

- [Namenode](http://localhost:50070/)
- [Resource Manager](http://localhost:8088/)
- [History Server](http://localhost:19888/)

Create user directory:

```
hadoop fs -mkdir -p /user/$USER
```

Browse the file system:

```
hadoop fs -ls /
```

Inside the Hadoop folder try to run this map reduce job to check everything is working (amend version number). Note the first command will put the file directly into the current user's **HDFS** directory (so make sure it exists):

```
hdfs dfs -put etc/hadoop input
# check that the input dir got created
hadoop fs -ls /user/diethardsteiner/input
hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.6.0.jar grep input output 'dfs[a-z.]+'
# check for output files
hadoop fs -ls /user/diethardsteiner/output
```

If the map reduce fails, go to the **Resource Manager** website, click on the **application id link**, then on the logs for one of the attempts, then on the stderr. In my case it said: `/bin/bash: /bin/java: No such file or directory`. This is an [issue](https://issues.apache.org/jira/browse/HADOOP-8717) particular to **Mac OS X** and [the workaround](http://blog.godatadriven.com/local-and-pseudo-distributed-cdh5-hadoop-on-your-laptop.html) is to amend the `$HADOOP_HOME/libext/hadoop-config.sh` file:

Replace this:

```
 # On OSX use java_home (or /Library for older versions)
   if [ "Darwin" == "$(uname -s)" ]; then
       if [ -x /usr/libexec/java_home ]; then
           export JAVA_HOME=($(/usr/libexec/java_home))
       else
           export JAVA_HOME=(/Library/Java/Home)
       fi
   fi
    ...
```

with this:

```
   # On OSX use java_home (or /Library for older versions)
    if [ "Darwin" == "$(uname -s)" ]; then
        if [ -x /usr/libexec/java_home ]; then
            export JAVA_HOME=$(/usr/libexec/java_home)
        else
            export JAVA_HOME=/Library/Java/Home
        fi
    fi
```

Now restart Yarn: `stop-yarn.sh && start-yarn.sh`. And try to run the MapReduce example again.


To stop them, run:

```
mr-jobhistory-daemon.sh stop historyserver
stop-yarn.sh
stop-dfs.sh
```

Now that we know that everything is working, let's fine-tune our setup. Ideally we should keep the config files (all `*site.xml` files) separate from the install directory (so that it is e.g. easier to upgrade). I copied these files to a dedicated Dropbox folder and use the `HADOOP_CONFIG_DIR` env variable to point to it. Another benefit is that now I can use the same config files for my dev environment on another machine.

> **Note**: It seems like you have to copy all files in this conf directory, as I saw error messages that e.g. the slaves file could not be found.

Add the below e.g. to `.bash_profile`:

```
export HADOOP_CONF_DIR=/Users/diethardsteiner/Dropbox/development/config/hadoop
```

To conveniently start and stop HDFS, we will create dedicated scripts which we store outside the install directory:


**Option 1**: HDFS data directory is specified as `/tmp` (this is the default):

`start-hadoop.sh`:

```
hdfs namenode -format
start-dfs.sh
start-yarn.sh
mr-jobhistory-daemon.sh start historyserver
```

`stop-hadoop.sh`:

```
mr-jobhistory-daemon.sh stop historyserver
stop-yarn.sh
stop-dfs.sh
```

**Option 2**: HDFS data directory is permanent 

`start-hadoop.sh`:

```
start-dfs.sh
start-yarn.sh
mr-jobhistory-daemon.sh start historyserver
```

`stop-hadoop.sh`:

```
mr-jobhistory-daemon.sh stop historyserver
stop-yarn.sh
stop-dfs.sh
```

Then source the bash profile file.

## Installing Hive ##

### Initial Setup

Some sources mention to first create the dedicated Hive directory on HDFS, however, this is not really necessary, as once you create a managed Hive table, Hive will automatically create this directory:

```
hadoop fs -mkdir /user/hive/warehouse
hadoop fs -chmod g+w /user/hive/warehouse
```

You will also need a permanent HDFS directory, because otherwise the Metastore will get confused (as the HDFS data gets delete with every restart of the machine).

Download **Hive** from [here](http://hive.apache.org/downloads.html) and extract it in a convenient location. 

Add this e.g. to `.bash_profile`:

```
export HIVE_HOME=/Applications/Development/Hadoop/apache-hive-0.14.0-bin
export PATH=$PATH:$HIVE_HOME/bin
```

Source `.bash_profile` and then you can start the **Hive** shell like this:

> **WARNING**: When issuing the below command, the Hive **Metastore** will be created in the same directory as the command was issued from. For this reason make sure that you are in a suitable directory before issuing this command the first time. The Megastore is stored in a directory called `metastore_db`. For this mode an embedded Derby database is used, to which **only one user at a time can connect**.

```
hive
```

Now issue SQL commands like `SHOW TABLES` etc.

### Setting up a local metastore

Let's set up the **local Metastore** now so that **more users can connect** to Hive. In local mode all the metadata will be stored in a good old **relational database** of your choice. 

Note it is also possible to set up Derby to handle more than one connection: [Instructions](https://cwiki.apache.org/confluence/display/Hive/HiveDerbyServerMode)

> **Note**: The metastore will not be created until the first SQL query hits it.

Create the database upfront (e.g. for PostgreSQL):

```sql
CREATE DATABASE hive_metastore_db;
```

Configuration ([hive-site.xml](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfiguringHive))

Add the respective JDBC driver to `$HIVE_HOME/lib` directory. In the `$HIVE_HOME/conf` directory take a copy of `hive-default.xml.template` and rename it to `hive-site.xml`. Open this file and delete everything between the `<configuration>` tags and then add this inside it (adjust to your settings):

This is an example for PostgreSQL (although when starting hive it showed some weird exception ` INFO metastore.MetaStoreDirectSql: MySQL check failed, assuming we are not on mysql: ERROR: syntax error at or near "@@"` - so I chose to go with MySQL instead - find config details further down):

**PostgreSQL**:

```xml
<configuration>
<property>
  <name>javax.jdo.option.ConnectionURL</name>
  <value>jdbc:postgresql://localhost:5432/hive_metastore_db</value>
  <description>JDBC connect string for a JDBC metastore</description>
</property>

<property>
  <name>javax.jdo.option.ConnectionDriverName</name>
  <value>org.postgresql.Driver</value>
  <description>Driver class name for a JDBC metastore</description>
</property>

<property>
  <name>javax.jdo.option.ConnectionUserName</name>
  <value>postgres</value>
  <description>username to use against metastore database</description>
</property>

<property>
  <name>javax.jdo.option.ConnectionPassword</name>
  <value>postgres</value>
  <description>password to use against metastore database</description>
</property>
  <property>
    <name>datanucleus.fixedDatastore</name>
    <value>true</value>
    <description></description>
  </property>
  <property>
    <name>datanucleus.autoCreateSchema</name>
    <value>false</value>
    <description></description>
  </property>
  <property>
    <name>hive.metastore.uris</name>
    <value>thrift://127.0.0.1:9083</value>
    <description></description>
  </property>
</configuration>
```

**MySQL**: Make sure the DB user has a password otherwise it will not work.

```xml
<configuration>
  <property>
    <name>javax.jdo.option.ConnectionURL</name>
    <value>jdbc:mysql://localhost:3306/hive_metastore_db?createDatabaseIfNotExist=true</value>
    <description>JDBC connect string for a JDBC metastore</description>
  </property>
  
  <property>
    <name>javax.jdo.option.ConnectionDriverName</name>
    <value>com.mysql.jdbc.Driver</value>
    <description>Driver class name for a JDBC metastore</description>
  </property>
  
  <property>
    <name>javax.jdo.option.ConnectionUserName</name>
    <value>root</value>
    <description>username to use against metastore database</description>
  </property>
  
  <property>
    <name>javax.jdo.option.ConnectionPassword</name>
    <value>root</value>
    <description>password to use against metastore database</description>
  </property>
  
  <property>
    <name>hive.metastore.warehouse.dir</name>
    <value>/user/hive/warehouse</value>
    <description>location of default database for the warehouse</description>
  </property>
  <property>
    <name>datanucleus.fixedDatastore</name>
    <value>true</value>
    <description></description>
  </property>
  <property>
    <name>datanucleus.autoCreateSchema</name>
    <value>false</value>
    <description></description>
  </property>
  <property>
    <name>hive.metastore.uris</name>
    <value>thrift://127.0.0.1:9083</value>
    <description></description>
  </property>
</configuration>
```

> **Note**: These are the correct settings as well for using **HiverServer2**. The last 3 properties I copied from the CDH VM Hive settings.

Now start the Hive CLI like this to see the log output in the terminal:

```
hive --hiveconf hive.root.logger=INFO,console
```

For MySQL I got following error:

```
ERROR DataNucleus.Datastore: Error thrown executing CREATE TABLE `SERDE_PARAMS`
(
    `SERDE_ID` BIGINT NOT NULL,
    `PARAM_KEY` VARCHAR(256) BINARY NOT NULL,
    `PARAM_VALUE` VARCHAR(4000) BINARY NULL,
    CONSTRAINT `SERDE_PARAMS_PK` PRIMARY KEY (`SERDE_ID`,`PARAM_KEY`)
) ENGINE=INNODB : Specified key was too long; max key length is 767 bytes
com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Specified key was too long; max key length is 767 bytes
```

To fix this run:

```sql
ALTER DATABASE hive_metastore_db character set latin1;
USE hive_metastore_db;
CREATE TABLE `SERDE_PARAMS`
(
    `SERDE_ID` BIGINT NOT NULL,
    `PARAM_KEY` VARCHAR(256) BINARY NOT NULL,
    `PARAM_VALUE` VARCHAR(4000) BINARY NULL,
    CONSTRAINT `SERDE_PARAMS_PK` PRIMARY KEY (`SERDE_ID`,`PARAM_KEY`)
) ENGINE=INNODB
;
```

You should be able to start Hive CLI without problems now.

### HiveServer2

To connect to Hive via JDBC, we have to set up **HiveServer2**. 

> **Note**: These days **Hive** supports transactions (ACID) and also the `UPDATE` and `DELETE` command. These features rely on dedicated locking tables in the metastore tables, which are managed by at least one **Zookeeper** instance ([Source](http://www.cloudera.com/content/cloudera/en/documentation/cdh4/v4-3-1/CDH4-Installation-Guide/cdh4ig_topic_18_5.html)). For our setup, we do not require any of these features and hence also not **Zookeeper**.

Configuring **HiveServer2**: For the very basic setup no configuration is required. If you require transaction etc, then you will have to amend the configuration details.

Next let's start HiveServer2:

```
hiveserver2
```

Or to output the log to the console use (RECOMMENDED):

```
hiveserver2 --hiveconf hive.root.logger=INFO,console
```

I found following error message in the log output:

```
ERROR DataNucleus.Datastore: An exception was thrown while adding/validating class(es) : Specified key was too long; max key length is 767 bytes
```

To fix this I first dropped the DB and recreated it:

```sql
DROP DATABASE hive_metastore_db;
CREATE DATABASE hive_metastore_db character set latin1;
```

Then I executed `scripts/metastore/update/hive-schema-0.14.0.mysql.sql`. After restarting the HiveServer2 the error message were gone.

Now shutdown hiveserver2. 


**Validate Hive Installation**

[Source](http://docs.hortonworks.com/HDPDocuments/HDP1/HDP-1.3.3/bk_installing_manually_book/content/rpm-chap6-5.html)

1. Make sure the hive database (e.g. MySQL) is up and running

2. Start the metastore service: `nohup hive --service metastore>/tmp/hive.out 2>/tmp/hive.log &`

3. Test Hive:

	```
	hive
	show databases;
	create table test(col1 int, col2 string);
	show tables;
	exit;
	```

4. Start HiveServer2:

	```
	nohup hiveserver2 > /tmp/hiveserver2.out 2> /tmp/hiveserver2.log &
	tail -f /tmp/hiveserver2.log
	```

5. Open Beeline command line shell to interact with HiveServer2.

	```
	beeline
	!connect jdbc:hive2://localhost:10000  
	```

You will be asked for:
	
 - username: currently logged in user, e.g. `diethardsteiner` 
	
 - password: `password` (actually it works without specifying a password at all)

More info: [Apache Wiki Reference](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2)

### Creating Start and Stop Scripts ###

If everything worked as expected, we can create the following shell files to conveniently start and stop the Hive services (adjust the DB commands).

In a convenient folder outside the Hive install directory place these start and stop scripts (and amend if necessary). You should place them in the same folder as you put the HDFS start and stop scripts.

`start-hive.sh`:

```bash
mysql.server start
nohup hive --service metastore>/tmp/hive.out 2>/tmp/hive.log &
nohup hiveserver2 > /tmp/hiveserver2.out 2> /tmp/hiveserver2.log &
```

`stop-hive.sh`: This this one on your OS - this is my custom version for Mac OS X.

```bash
ps -ef | grep hive | awk '{print $2}' | head -n2 | xargs kill -9
mysql.server stop
```

Find some more interesting info on the [Hortonworks Wiki](http://docs.hortonworks.com/HDPDocuments/HDP1/HDP-1.3.9/bk_reference/content/reference_chap3_2.html).

More info on [Hive Clients](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients).

# Pentaho Data Integeration (PDI) and Hadoop

## Installing PDI

It's extremely easy to install PDI: Just download the latest version from [Sourceforge](http://sourceforge.net/projects/pentaho/files/Data%20Integration/), unzip the folder and place it in a convientent location (e.g. Applications folder).

## Configurating PDI 

### Any Hadoop Distro except Apache

This section is a general overview for any Hadoop distro - if you followed the previous Hadoop tutorial, the next section will be the ideal fit!

In order to tell PDI which Hadoop distro you are using, you have to adjust the properties file in `data-integration/plugins/pentaho-big-data-plugin/plugin.properties`.

Amend `active.hadoop.configuration` to your respective Hadoop setup (suitable values can be found in the folder names in `data-integration/plugins/pentaho-big-data-plugin/hadoop-configurations`). If you are using any additional Kettle plugins list them in `pmr.kettle.additional.plugins` so that they are distributed across the **HDFS cluster** as well.

If you are using **CDH**, PDI expects a secured/kerberised install. If you install is unsecured, amend `data-integration/plugins/pentaho-big-data-plugin/cdh*/plugin.properties`: comment out all the lines containing: `activator.classes=<anything>`. (This seems to be EE only)

Next follow [these instructions](http://wiki.pentaho.com/display/BAD/Additional+Configuration+for+YARN+Shims) to configure the YARN access for the shim - This doesn't seem to be necessary if you are using the vanilla hadoop 2.0 shim.

Pentaho provides a few examples to get started in the *Next Steps* section on [this page](http://wiki.pentaho.com/display/BAD/Configuring+Pentaho+for+your+Hadoop+Distro+and+Version).

Sources:

- [Getting Started with Hadoop and Pentaho](https://dankeeley.wordpress.com/2015/01/19/getting-started-with-hadoop-and-pentaho/), 
- [Penatho Wiki Entry](http://wiki.pentaho.com/display/BAD/Configuring+Pentaho+for+your+Hadoop+Distro+and+Version)

### Apache Hadoop OR Latest Hadoop Version Not Supported

If you are using the vanilla **Apache Hadoop** installation (so not Cloudera or similar), then there is one important point to consider: PDI ships with an ancient Big Data shim for this **Apache Hadoop**. The default PDI shim is **hadoop-20** which refers to an Apache Hadoop 0.20.x distribution ([Source](http://stackoverflow.com/questions/25043374/unable-to-connect-to-hdfs-using-pdi-step)), so don't be fooled that the `20` suffix refers to the Hadoop 2.x.x release you just downloaded.

Matt Burgess suggested using the Hortenworks (HDP) shim instead (for the reason that **Hortonworks** feeds most of the in-house improvements back into the open source projects). This works fine out-of-the-box if you are only using **HDFS** directly (so copying files etc). To use MapReduce, we have to make a few amendments:

**How to figure out which version of Hadoop a particular shim uses**: Go into the `<shim>/lib/client` folder and see which version number the `hadoop*` files have. This will help you understand if your vanilla Hadoop distro version is supported or not. In my case I had vanilla Apache Hadoop 2.6.0 installed and required a shim that supported just this version. PDI-CE-3.5 ships with the **hdp21** shim, which seems to support Hadoop 2.4. Luckily Pentaho had already a newer **Hortonworks** shim available: You can browse all the available [Shims on Github](https://github.com/pentaho/pentaho-hadoop-shims) and download a compiled version from [Shims on CI](http://ci.pentaho.org/view/Big%20Data/job/pentaho-hadoop-shims-5.3/) (note this is PDI version specific, see notes further down also for detailed download instructions). The shim that supported the same version was **hdp22**.

Once download, I added this file to the other shims and extracted it. Then I adjusted following shim config files in  `pdi-ce-5.3/plugins/pentaho-big-data-plugin/hadoop-configurations/hdp22` (only amended sections mentioned):

`mapred-site.xml`:

```xml
  <property>
    <name>mapreduce.jobhistory.address</name>
    <value>localhost:19888</value>
  </property>
```

`yarn-site.xml`:

```xml
  <property>

  <!--DS: Use this once you start using the external hadoop config dir
    <value>$HADOOP_CONF_DIR:$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/hdfs/*:$HADOOP_HOME/share/hadoop/hdfs/lib/*:$HADOOP_HOME/share/hadoop/yarn/*:$HADOOP_HOME/share/hadoop/yarn/lib/*
    </value>
  -->
      <value>$HADOOP_HOME/etc/hadoop:$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/hdfs/*:$HADOOP_HOME/share/hadoop/hdfs/lib/*:$HADOOP_HOME/share/hadoop/yarn/*:$HADOOP_HOME/share/hadoop/yarn/lib/*
      </value>

  </property>

  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>localhost</value>
  </property>

  <property>
    <name>yarn.resourcemanager.address</name>
    <value>${yarn.resourcemanager.hostname}:8032</value>
  </property>
```

Then don't forget to adjust `pdi-ce-5.3/plugins/pentaho-big-data-plugin/plugin.properties` so that the correct shim is used:

```
active.hadoop.configuration=hdp22
```

All the configuration details are set up now.

For more details on how to adjust the PDI Shin to specific distributions see [here](http://wiki.pentaho.com/display/BAD/Additional+Configuration+for+YARN+Shims).

Info about Shims:

- [Precompiled additional shims (Not up-to-date)](http://wiki.pentaho.com/display/BAD/Configuring+Pentaho+for+your+Hadoop+Distro+and+Version), see *Check for Pentaho Hadoop Distribution Support* section
- [Shims on Github](https://github.com/pentaho/pentaho-hadoop-shims)
- [Shims on CI](http://ci.pentaho.org/view/Big%20Data/job/pentaho-hadoop-shims-5.3/): There are several shim sections, 5.3 is just one of them, so search around in the **Big Data** section and try to find the right one. Then click on **Last Successful Artifacts**, on the next page choose the shim you want from the folder. On the next page click on the `dist` folder. Then finally all the files are shown. Choose the package file to download, e.g. `pentaho-hadoop-shims-hdp22-package-5.3-SNAPSHOT.zip`.


## Using HDFS ##

### How Do I Connect to HDFS? ###

To connect to Hadoop you connect directly to the **HDFS namenode**. How do you find out the exact port number? Either inspect `core-site.xml` or you go to the HDFS Namenode website, usually found on `http://localhost:50070/dfshealth.html#tab-overview`. The first heading will reveal the full URL with the port number, e.g.:

```
Overview 'localhost:8020' (active)
```

### Specifying the Hadoop Cluster ###

Under **View > Jobs > Hadoop Clusters** right click and choose **New**.

You can specify connection settings for HDFS, JobTracker (this is a MR1 term, if you run MR2/Yarn this will be the ResourceMananger), Zookeper and Oozie. Only fill out the entries for the components you have installed and delete any defaults for the other components.

So my settings were:

**HDFS**:

- Hostname: localhost
- Port: 8020
- Username:
- Password:

Just to be clear (for our tutorial): there is no username or password to be specified!

JobTracker (which is really the **ResourceMananger** in MR2):

- Hostname: localhost
- Port: 8032

If you are not sure about the default Yarn Port, take a look at [this reference](http://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-common/yarn-default.xml).

These settings can be used in job entries like **Pentaho MapReduce**. Note that settings somehow do not get updated in the job if you change the cluster settings. In this case, open the **Pentaho MapReduce** job entry, click on the **Cluster** tab and then **Edit** next to the **Connection** input box. Just click **OK** to confirm the settings and save the job.

### How to Test the Hadoop Connection ###

The Hadoop Cluster config panel doesn't have a check connection option. However, the **Hadoop Copy Files** job entry has a **Browse** feature for **File/Folder Destination**, which will allow you to browse HDFS directories and hence will confirm if your connection details are correct.

### Importing Data to HDFS

You have following options:

- **Hadoop Copy Files** job entry: This is similar to the `hadoop fs -put` command, so this should only be used to import file to HDFS but not to move files within one HDFS cluster!
- **Text Output** step: using VFS
- **Hadoop File Output** step: The **VFS** originally didn't support HDFS hence this step was developed. It looks very similar to the **Text Output** step and there is no particular reason why you would use this step over the **Text Output** step.
- **Sqoop** job entry

Use these steps as well for Hive and Impala, just in this case you will have to let the **Metastore** know about the new data files. Do not use the **Table Output** step for importing data as it is not supported.

> **Note**: **Hadoop Copy Files** and **Hadoop File Output** only support text (and CVS) files. Use **Sqoop** to store files directly as **Parquet** etc.

### Reading HDFS Files and Outputting to HDFS

The **Hadoop File Input** and **Hadoop File Output** steps are designed to read data from and output data to HDFS. These steps only support the text and CSV data types.

> **Important**: Do not use the **Hadoop File Input** to process data within Hadoop, as it will export all the data from Hadoop to PDI. Instead create a **Pentaho MapReduce** job or use Hive etc.

**Example**: Use the **Hadoop File Input** to prepare some data for a **PRD** report or export it to a relational database.

### File Management Job Entries

File Management job entries all work with HDFS, although not all of them offer a **Browse HDFS** function. You can, however, specify the Apache VFS (virtual file system) path manually like so:

```
hdfs://<username>:<password>@<namenode>:<port>/
<folderpath>
```

### What to Do Next ###

What these excellent introduction videos:

- [Loading Data into Hadoop](http://www.youtube.com/watch?v=Ylekzmd6TAc)
- [Introduction to Pentaho MapReduce](https://www.youtube.com/watch?v=KZe1UugxXcs)
- [Pentaho Hadoop Tutorials](http://infocenter.pentaho.com/help48/topic/pdi_user_guide/topic_big_data_tutorials_hadoop.html) or [this more up-to-date version](http://wiki.pentaho.com/display/BAD/Hadoop)

## Using Hive

### Specifying the Connection Details

> **Note**: Normally you have to add a dedicated JDBC driver to the PDI lib directory in order to be able to connect to a certain DB. This is not required for **Hive**, as the respective driver is already part of the **Big Data Shim**.

In **Spoon** create a new database connection:

- **Connection Type**: Hadoop HiveServer2
- **Host Name**: localhost
- **Port Number**: 10000
- **Username**: currently logged in user ?? or user running the hive server2 process?? e.g. diethardsteiner
- **Password**: password

![](./img/pentaho-pdi-hadoop-1.png)

Click the **Test** button to make sure the connection details are correct. The database connection test should return a message similar to this one: `Connection to database [hive-localhost] is OK.`. Click **OK** twice. Then right click on the connection we just created and choose **Share**:

![](./img/pentaho-pdi-hadoop-2.png)

Next right click again on the connection name and choose **Explore**. This will open a new dialog window which will allow you to see the existing schemas and tables (among other things). You can e.g. right on a table name and see the first 100 rows, which is a very useful feature to get a bit accustomed to the available data.

Now we are all set to start exploring the exciting world of Hadoop and Hive!

### What's Not Supported

Hive does not support SQL Insert, Update or Delete Statements: Pentaho Table Output, Update and Delete steps do not work. 
Import files into the HDFS for the Hive tables using **Hadoop Copy Files** or use the **Hadoop File Output** step to create a file for Hive. Use other file management steps to create or move a file for Hive.

## Notes on PDI Job entries and Steps

### Hadoop Copy Files

> **Note**: This job entry should be only used to copy local data to a HDFS cluster or from cluster to cluster. Do not use it to copy files within a cluster (because it would stream data through PDI) - use the file management job entries or shell scripts instead.

> **Important** ([Source](http://wiki.pentaho.com/display/EAI/Hadoop+Copy+Files)): When not using Kerberos security, the Hadoop API used by this step sends the username of the logged in user when trying to copy the file(s) regardless of what username was used in the connect field. To change the user you must set the environment variable `HADOOP_USER_NAME`. You can modify spoon.bat or spoon.sh by changing the OPT variable:

```
OPT="$OPT .... -DHADOOP_USER_NAME=HadoopNameToSpoof"
```