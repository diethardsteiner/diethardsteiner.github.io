---
layout: post
title:  "PDI Data Services"
summary: This provides detailled instructions on how to set up PDI Data Services
date: 2016-05-02
categories: PDI
tags: PDI, Docker
published: false
---

Questions to Edwin:

- MetaStore: Where can I find a documentation for this? / Based on which reference did you create the files?


Some time ago I started writing on a blog post about **Pentaho Data Services**, but then never came around to finish it. A few weeks ago **Edwin Weber** (of PDI Data Vault fame) gave a presentation on this particular topic at Pentaho Netherlands? Meetup. I kindly asked him to send me the slides and discovered that he had made quite some interesting findings, some of which I will describe below, mainly for the reason that information on Pentaho Data Services in the Community Edition seems to be extremely scarce. 


# Setting up the Dev Environment

## Docker Containers

### MySQL

[Docker Percona MySQL](https://hub.docker.com/_/percona/)

```bash
$ docker pull percona:5.7
$ docker run \
--name myPerconaContainer \
-p 3306:3306 \
-e MYSQL_ROOT_PASSWORD=test \
-e MYSQL_DATABASE=test \
-e MYSQL_USER=test \
-e MYSQL_PASSWORD=test \
-d percona:5.7
```

> **Note**: Make sure to provide a port mapping when starting the container, so external clients (so clients on your host system) like PDI can connect to it.

To check if the port is correctly exposed, run this:

```bash
$ docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED              STATUS              PORTS                    NAMES
d3425237b274        percona:5.7         "/docker-entrypoint.s"   About a minute ago   Up About a minute   0.0.0.0:3306->3306/tcp   myPerconaContainer
```

Alternatively, if you have **Nmap** installed run this:

```bash
$ docker-machine ps
192.168.99.100
$ sudo nmap -sT -O 192.168.99.100
Password:

Starting Nmap 7.01 ( https://nmap.org ) at 2016-05-03 19:32 BST
Nmap scan report for 192.168.99.100
Host is up (0.00048s latency).
Not shown: 998 closed ports
PORT     STATE SERVICE
22/tcp   open  ssh
3306/tcp open  mysql
MAC Address: 08:00:27:42:BB:F7 (Oracle VirtualBox virtual NIC)
Device type: general purpose
Running: Linux 3.X|4.X
OS CPE: cpe:/o:linux:linux_kernel:3 cpe:/o:linux:linux_kernel:4
OS details: Linux 3.2 - 4.0
Network Distance: 1 hop

OS detection performed. Please report any incorrect results at https://nmap.org/submit/ .
Nmap done: 1 IP address (1 host up) scanned in 1.99 seconds
```

As you can see, the port is correctly exposed (note that we asked docker machine for the IP - this is only necessary on Mac OS X and Windows).

Check that MySQL DB is actually working (this creates another container which links to our previous one, you'll land in the mysql command line client):

```bash
docker run -it --link some-percona:mysql --rm percona sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'
```

Also, use a local tool to cross check the connection:

```bash
$ mysql -utest -h192.168.99.100 -P3306
```

### MongoDB

[Docker Mongo](https://hub.docker.com/_/mongo/)

```bash
$ docker pull mongo:3.3
$ docker run \
--name myMongoContainer \
-p 27017:27017 \
-d mongo:3.3
```

Use the mongodb command line tool or an app like [Robomongo](https://robomongo.org) to check if the connection is working.

With the data store services ready, we can now shift our focus to PDI and TEEID, which we will install locally.

> **Note**: If at any point you want to stop the containers, just run `docker stop <containerName>`. And if you want to start time later on again: `docker start <containerName>`.

### PDI

Download the latest version from **Sourceforge** and extract it in a convenient location. Then download the MySQL JDBC driver and add it to the PDI `lib` folder.

### TEIID

# Making Data available as a Service

For most of the example I will reuse the transfromations shown at the Meetup by Edwin Weber - a big thanks for him for sharing them!

Ok, let's get started:

1. Download the example files from [here]().
2. Extract them in a convenient directory.
3. Inside the extracted directory you'll find a `my-version` directory. On the command line, move into this directory.
4. Next run `vi config/dev/.kettle/kettle.properties` and adjust the database settings to your local setup.
5. Next adjust the `KETTLE_HOME` path: `vi shell-scripts/set-project-variables.sh`. Then execute the same file.
6. Navigate to your PDI install folder. Start **Spoon**.
7. In **Spoon** connect to the file repository [[OPEN]] open all the jobs and transformations located in our project folder in the `di` folder.
8. Execute the `job_load_data_for_virt_demo` job.


> **Important**: For this exercise we will work with a **PDI file based repository**, which we will register with **Carte**, so that all **Data Services** are available in an easy fashion.

[OPEN]: There are some artefacts in ~/.pentaho like the metastore, which we should ideally have in the project files

## Web Services (Carte REST service)

This feature has been available in PDI for quite some time and is quite easy to set up: Just tick the *Pass output to servlet* option in the **XML Output**, **JSON Output** and **Text Output** steps. When you run the transformation on a **Carte** server, the step's output will be exposed.


See for more info [here](http://wiki.pentaho.com/display/EAI/PDI+data+over+web+services).

**Example**:

In a new **Terminal** window  and set the `KETTLE_HOME` environment variable using our shell script (see instructions further up).

Navigate to the **PDI** install directory and start the **Carte** server (adjust project path):

```bash
sh ./carte.sh /Users/diethardsteiner/Dropbox/development/code-examples/pentaho/PDI/PDI-and-Teiid-data-virtualization/my-version/config/dev/carte.config.xml
```

The supplied Carte config also registers a PDI file repo, so that jobs and transformations are accessible.


```bash
http://localhost:9081/kettle/executeTrans/?rep=pdi-data-services-example&user=admin&pass=password&trans=trf_demo_virt_karate_members
```

> **Important**: When working with a repository, you must not include the file extension in the transformation name (in the above URL)!

Paste this into your favourite web browser and have fun!

![](./img/pdi-data-services-1.png)

**Alternatively**, if you were **not using a repository**, the approach is the following:

Open `trf_demo_virt_karate_ranks`, right click on the canvas and choose **Properties**. Copy the whole **Transformation Filename** and paste it into a good text editor. Mine looks like this:

```
/Users/diethardsteiner/Dropbox/development/code-examples/pentaho/PDI/PDI-and-Teiid-data-virtualization/my-version/di/trf_demo_virt_karate_ranks.ktr
```

Next replace the forward slashes with `%2F` to urlencode the path:

```
%2FUsers%2Fdiethardsteiner%2FDropbox%2Fdevelopment%2Fcode-examples%2Fpentaho%2FPDI%2FPDI-and-Teiid-data-virtualization%2Fmy-version%2Fdi%2Ftrf_demo_virt_karate_ranks.ktr
```

Finally we combine the above path with the web service call:

```
http://cluster:cluster@localhost:9081/kettle/executeTrans/?trans=%2FUsers%2Fdiethardsteiner%2FDropbox%2Fdevelopment%2Fcode-examples%2Fpentaho%2FPDI%2FPDI-and-Teiid-data-virtualization%2Fmy-version%2Fdi%2Ftrf_demo_virt_karate_ranks.ktr
```

## Data Services (Kettle JDBC Thin Driver)

Previously known by the name **Kettle JDBC Thin Driver**, this feature was first announced around 2013 (see [my initial blog post](http://diethardsteiner.blogspot.co.uk/2013/01/creating-federated-data-service-with.html)) and was shortly after only available in the EE version of PDI. At the end of 2015 **Pentaho** decided to make this feature available in the CE version.

### The Metastore

The **Data Service** configuration details are saved in the **Metastore**. The [Metastore](https://github.com/pentaho/metastore) is Pentaho's attempt to create a generic and *flexible metadata, data and configuration information store*, which can be used by anyone for various purposes.

The **XML** file struture is composed of:

- A namespace
- A top level `<element>` node, containing the basic `<id>`, `<value>`, `<type>` and `<name>` nodes.
- The `<element>` node moreover has a `<children>` node, which in turn can have nested `<child>` and `<children>` elements. A `<child>` node contains the same basic nodes as the top level element (`<id>`, `<value>`, `<type>` and `<name>`).
- The `<element>` node also has a `<security>` node, which defines the `<owner>` as well as the `<owner-permissions-list>`, explaining who has which permission to use the element. 

In regards to the metastore file for the **Pentaho Data Service** there seems to be currently a bug/or limition. Edwin Weber notes: "When I worked on a file, not in a file repository!, the metastore file got created when defining a step as a data service. Then I continued in a repository, creating these metastore files by hand."

### Defining a Pentaho Data Service

In your transformation, right click on a step and choose **Data Services > New**. In the **Data Service** dialog provide a sensible **Service Name** (which will be the name of the *Virtual Table*) and decide if you want to make use of any of the available options like **Cache**, **Query Pushdown** and **Parameter Pushdown**. 

Pentaho **Data Services** have gained a few new features over the last few releases: 

- **Query Pushdown**: A constrain (`WHERE` clause) can be applied to the **Table Input** as well as **MongoDB Input** steps.
- **Parameter Pushdown**: Can be applied to any step and supports simple constrains (e.g. `WHERE fruit = 'apple'`).
- **Test Data Services**: This dialog allows you to test/run any query against your data service, check the results and understand if it can be optimised.
- **Driver Details**: A very convenient dialog (new as of PDI v6.1) which list the JDBC class name and JDBC URL and also allows you to download the **JDBC Driver**.

The **Data Service** config will be stored in an XML file of the same name as the transformation in following directory: 

```bash
~/.pentaho/metastore/pentaho/Data\ Service\ Transformation/
```

Note that once you define a **Data Service** for a given step, this step will show the Data Service icon:

![](./img/pdi-data-services-2.png)

## Connecting to the Pentaho Data Source from 3rd Party Clients

As we are working with a PDI repository (which was registered with Carte), we can see all available **Data Services** by calling following **REST service**:

```
http://localhost:9081/kettle/listServices/
```

![](./img/pdi-data-services-3.png)

So, let's see if we can query our tables from a **SQL Client**. For this example I'll use **NetBeans**' built-in SQL client, but the approach should be fairly similar with any other JDBC SQL client:


1. In **Spoon** open any transformation which has a **Data Service** defined. Right click on the step for which you set up the **Data Service** and choose **Data Service > Edit**. Click on **Driver Details** and then click **Get Driver**. Save the **JDBC driver** in a convenient directory and unzip the file.
2. In **NetBeans** click on the **Services** tab, then right click on **Databases** and choose **New Connection**.
3. Define a new Driver and then add the all the required files from the extracted **JDBC Driver** folder:

	![](./img/pdi-data-services-4.png)
	
	Basically just add all the jar files from the extracted JDBC folder to the Driver definition! Click **Next**.

4. Next provide the connection details: The hostname and port as well as user name and password will be the ones you specified for the **Carte** server, so in my case this translates to: `jdbc:pdi://hostname:port/kettle`. Other SQL clients might also require the JDBC Driver class name: `org.pentaho.di.trans.dataservice.jdbc.ThinDriver`.

	![](./img/pdi-data-services-5.png)
	
	Click **Finish** and the connection should work now.
	
5. Expand the new connection node in the **Services** panel to see the tables:

	![](./img/pdi-data-services-6.png)

6. Right click on the new connection node in the **Services** panel and choose **Execute command**, which will open the SQL Editor. Write and run a query against any of our tables:

	![](./img/pdi-data-services-7.png)
	
So far so good, what happens, however, if you try to join two tables? It won't work! We shall solve this in a bit based on a solution suggested by Edwin Weber.

## Connecting to the Pentaho Data Source from within PDI

I added the following connection details to our `kettle.properties`:

```
# PDI Data Services Connection
pdi_ds_host_name=localhost
pdi_ds_port_number=9081
pdi_ds_jdbc_driver_class_name=org.pentaho.di.trans.dataservice.jdbc.ThinDriver
pdi_ds_user_name=cluster
pdi_ds_password=cluster
```

Copy the **JDBC Driver** files to the `pdi/lib` directory.

Edwin Weber hightlighted that currently the connection type **Pentaho Data Service** does not work and hence the **generic type** should be used! This has mainly to do with the fact that for the **Pentaho Data Service** connection type the **Pentaho EE DI Server** is required.

The connection details look like this:

![](./img/pdi-data-services-9.png)

Open the `tr_test_data_service_connection` transformation and preview the output of the **Table Input** step:

![](./img/pdi-data-services-8.png)


## Teiid - The Virtualisation Layer

### Introduction and Installation

As we previously learnt, joining two Kettle virtual tables in a SQL Client results in an error. In his meetup talk, Edwin Weber demoed **Teiid** to solve just this issue. The open source project [Teidd](http://teiid.jboss.org) (sponsored by RedHat) is billed as "a data virtualization system that allows appliations to use data from multiple, heterogenous data stores". There is also a dedicated [Teiid Designer](http://teiiddesigner.jboss.org) available for creating and maintaining Teiid metadata (virtual tables). There is even a [Teiid Dashboard](https://github.com/teiid/teiid-dashboard) web application (which is a customised version of [JBoss Dashboard Build](https://docs.jboss.org/drools/release/6.0.0.CR5/dashboard-builder-docs/html_single/)) available. [These slides](http://redhat.slides.com/kennethwpeeples/dvprimer-introduction#/) provide a good introduction to RedHat Data Virtualisation.

![](./img/pdi-data-services-10.png)

Download the following from [here](http://teiid.jboss.org/downloads/):

- Teiid Runtime
- JDBC Driver


Download [Teiid Designer](http://teiiddesigner.jboss.org/designer_summary/downloads.html). Add the bottom of this page you find a section called **Install into Eclipse 4.5 (Mars)** - follow these instructions (requires separate download of Eclipse Mars).

Then consult the [Teiid Documentation](https://docs.jboss.org/author/display/teiidexamples/Home); we are especially interested in the [Installation Guide](https://docs.jboss.org/author/display/TEIID/Installation+Guide). There is also a very good [Examples](https://docs.jboss.org/author/display/teiidexamples/Examples) page with instructions on how to connect to various DBs, XML, Flat files, web services etc.

[The Basics](http://teiid.jboss.org/basics/) are that **Teiid** consists of following components:

- Virtual Databases
- Models
- Connectors
- Data Services
- SOAs

Let's get started:

Extract Teiid Runtime zip folder, which is basically just a **JBoss** server (or **WildFly** server as it is called these days). Note that the `docs/teiid` folder contains a huge amount of examples to get started.

Now, on your command line navigate into this folder and run the following:

```bash
sh ./bin/standalone.sh -c=standalone-teiid.xml
```

To access the Admin Panel go to following ULR `http://127.0.0.1:9990`. You will see a notice that the server is running but that no user has beeen set up. To remedy this, open a new terminal window/tab, navigate to the server folder and create an **Admin User** by running the following:

```bash
$ sh ./bin/add-user.sh
```

On the first prompt just hit enter, next provide a user name and then a password. The user does not belong to any group. When asked if this new user going to be used for one AS process to connect to another AS process, answer yes. 


[[ REMOVE ]]

Is this new user going to be used for one AS process to connect to another AS process? 
e.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.
yes/no? yes
To represent the user add the following to the server-identities definition <secret value="c2lzdGVyNDc=" />

[[/ REMOVE ]]

Once this is done, in your web browser, click on the *Try again* link on the WildFly page. You should then be asked for a user name and password.

A good quick starter guide is available [here](https://docs.jboss.org/author/display/teiidexamples/Hello+World+Teiid+Data+Federation+Example).

Now, we also add an **application user**, which we will use for the JDBC access:

```bash
$ sh ./bin/add-user.sh 

What type of user do you wish to add? 
 a) Management User (mgmt-users.properties) 
 b) Application User (application-users.properties)
(a): b

Enter the details of the new user to add.
Using realm 'ApplicationRealm' as discovered from the existing property files.
Username : diethardsteiner
Password recommendations are listed below. To modify these restrictions edit the add-user.properties configuration file.
 - The password should be different from the username
 - The password should not be one of the following restricted values {root, admin, administrator}
 - The password should contain at least 8 characters, 1 alphabetic character(s), 1 digit(s), 1 non-alphanumeric symbol(s)
Password : 
WFLYDM0102: Password should have at least 1 non-alphanumeric symbol.
Are you sure you want to use the password entered yes/no? yes
Re-enter Password : 
What groups do you want this user to belong to? (Please enter a comma separated list, or leave blank for none)[  ]: odata
About to add user 'diethardsteiner' for realm 'ApplicationRealm'
Is this correct yes/no? yes
Added user 'diethardsteiner' to file '/Applications/Development/teiid-8.13.4/standalone/configuration/application-users.properties'
Added user 'diethardsteiner' to file '/Applications/Development/teiid-8.13.4/domain/configuration/application-users.properties'
Added user 'diethardsteiner' with groups odata to file '/Applications/Development/teiid-8.13.4/standalone/configuration/application-roles.properties'
Added user 'diethardsteiner' with groups odata to file '/Applications/Development/teiid-8.13.4/domain/configuration/application-roles.properties'
Is this new user going to be used for one AS process to connect to another AS process? 
e.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.
yes/no? no
```

Make sure that you assign the user the group `oadata`.

### Adding the Pentaho Data Services JDBC Driver as a Core Module

Source: [Install a JDBC Driver as a Core Module](https://access.redhat.com/documentation/en-US/JBoss_Enterprise_Application_Platform/6/html/Administration_and_Configuration_Guide/Install_a_JDBC_Driver_as_a_Core_Module1.html)

The next step is to add the **Pentaho Data Services JDBC Driver** to **WildFly**: Copy the `pentaho-data-services` folder into `<wildfly-root>/modules`. 


Create following folder structure:

```
cd <wildfly-root>/modules
mkdir -p org/pentaho/di/trans/dataservice/jdbc/ThinDriver/main
```

We will have to add a Wildfly specific `module.xml` to this folder (you can copy this file from one of the other folders in this subdirectory and adjust it or just copy the contents shown below):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.0" name="org.pentaho.di.trans.dataservice.jdbc.ThinDriver">
    
    <resources>
        <resource-root path="commons-codec-1.5.jar"/>
        <resource-root path="commons-httpclient-3.1.jar"/>
        <resource-root path="commons-lang-2.6.jar"/>
        <resource-root path="commons-logging-1.1.1.jar"/>
        <resource-root path="commons-vfs2-2.1-20150824.jar"/>
        <resource-root path="guava-17.0.jar"/>
        <resource-root path="javassist-3.12.1.GA.jar"/>
        <resource-root path="kettle-core-6.1.0.1-196.jar"/>
        <resource-root path="kettle-engine-6.1.0.1-196.jar"/>
        <resource-root path="log4j-1.2.16.jar"/>
        <resource-root path="pdi-dataservice-client-plugin-6.1.0.1-196.jar"/>
        <resource-root path="scannotation-1.0.2.jar"/>
    </resources>

    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
```

Next add all the JDBC driver files to this directory.

> **Important**: It seems like it is best practice to stop the server now and make the config changes. When I was changes `standalone-teeid.xml` while the server as running and later on restarted the server, my changes to the file seemed to have gone.


### Creating Virtual Tables the Manual Way

#### Creating Connections and Drivers

Now we will create a **Virtual Database (VDB)** in **Teiid**, which will access the **Pentaho Virtual Tables** and allow us to query them (both individually as well as in a JOIN). We could also create a table/view in **Teiid** to based on these source tables. 

As the documentation states, the usual workflow is the following:

1.	Configuring the connections to the data sources.
2.	Creating the VDB file
3.	Start the WildFly/Teiid Server
4.	Deploy the VDB file
5.	Access the VDB using JDBC application. (ODBC is left as exercise to the user)

First off, we will **create the connection/data source**: Open the `<wildfly>/standalone/configuration/standalone-teiid.xml` file. When accessing a standard SQL Database, just add a  `<datasource>` within the `<datasources>` section:

```xml
  <datasource jta="true" jndi-name="java:/pdi-dataservice-karate-connector" pool-name="pdi-dataservice-karate" enabled="true" use-ccm="true">
      <connection-url>jdbc:pdi://localhost:9081/kettle</connection-url>
      <driver>pdi-dataservice-client-driver</driver>
      <security>
          <user-name>cluster</user-name>
          <password>cluster</password>
      </security>
  </datasource>
  ```

Just a little bit further down you can find the `<drivers>` section. Add the following:

```xml
<driver name="pdi-dataservice-client-driver" module="org.pentaho.di.trans.dataservice.jdbc.ThinDriver">
    <driver-class>org.pentaho.di.trans.dataservice.jdbc.ThinDriver</driver-class>
</driver>
```

[OPEN] WHY ARE THERE TWO DRIVERS DEFINED??? (one is for the web service I think)
[OPEN] 2nd driver is not available as a module, or is it? ADD!

> **Important**: The `module` name must be the same as the one you defined when adding the JDBC driver in the `module.xml`.

#### Creating the Virtual Database File

Next let's create the **Virtual Database** file: [[ ADD SOME MORE DESCRIPTION ]]
https://docs.jboss.org/author/display/teiidexamples/Hello+World+Teiid+Data+Federation+Example

```xml
<vdb name="Karate" version="1">
  
    <description>The Karate VDB</description>
  
    <model name="Karate" type="PHYSICAL">
        <source name="pdi-dataservice-karate" translator-name="jdbc-ansi" connection-jndi-name="java:/pdi-dataservice-karate-connector"/>
    </model>
  
</vdb>
```

Save this file as `teiid-vdb-karate.xml`.

#### Deploying the Virtual Database File

Copy the `karate-vdb.xml` to `<wildfly>/standalone/deployments`. You should see a file in the same folder with the extension `deployed`. Also, the logs should show following message: `deployed "teiid-vdb-karate.xml"`

If you started WildFly/JBoss in debug mode, you will see following error messages:

```bash
19:22:32,319 ERROR [stderr] (Worker0_async-teiid-threads0)  Caused by: org.pentaho.di.core.exception.KettleFileException: 
19:22:32,319 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,319 ERROR [stderr] (Worker0_async-teiid-threads0)  Unable to get VFS File object for filename 'plugins' : Could not find file with URI "/Applications/Development/teiid-8.13.4/plugins" because it is a relative path, and no base URI was provided.
19:22:32,319 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,319 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,320 ERROR [stderr] (Worker0_async-teiid-threads0)  	at org.pentaho.di.core.vfs.KettleVFS.getFileObject(KettleVFS.java:158)
19:22:32,320 ERROR [stderr] (Worker0_async-teiid-threads0)  	at org.pentaho.di.core.vfs.KettleVFS.getFileObject(KettleVFS.java:106)
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  	at org.pentaho.di.core.vfs.KettleVFS.getFileObject(KettleVFS.java:102)
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  	at org.pentaho.di.core.plugins.PluginFolder.findJarFiles(PluginFolder.java:124)
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  	... 39 more
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  org.pentaho.di.core.exception.KettleFileException: 
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  Unable to list jar files in plugin folder '/Users/diethardsteiner/.kettle/plugins'
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  Unable to get VFS File object for filename '/Users/diethardsteiner/.kettle/plugins' : Could not find file with URI "/Users/diethardsteiner/.kettle/plugins" because it is a relative path, and no base URI was provided.
19:22:32,321 ERROR [stderr] (Worker0_async-teiid-threads0)  
```

And further down:

```bash
19:22:32,719 WARN  [org.teiid.RUNTIME] (Worker0_async-teiid-threads0)  TEIID50036 VDB Karate.1 model "Karate" metadata failed to load. Reason:TEIID11010 java.sql.SQLException: org.pentaho.di.core.exception.KettleValueException: 
Unexpected conversion error while converting value [NULLABLE Integer] to an Integer
java.lang.Integer cannot be cast to java.lang.Long
: org.teiid.translator.TranslatorException: TEIID11010 java.sql.SQLException: org.pentaho.di.core.exception.KettleValueException: 
Unexpected conversion error while converting value [NULLABLE Integer] to an Integer
java.lang.Integer cannot be cast to java.lang.Long

	at org.teiid.translator.jdbc.JDBCExecutionFactory.getMetadata(JDBCExecutionFactory.java:297)
	at org.teiid.translator.jdbc.JDBCExecutionFactory.getMetadata(JDBCExecutionFactory.java:68)
	at org.teiid.query.metadata.NativeMetadataRepository.getMetadata(NativeMetadataRepository.java:92)
	at org.teiid.query.metadata.NativeMetadataRepository.loadMetadata(NativeMetadataRepository.java:60)
	at org.teiid.query.metadata.ChainingMetadataRepository.loadMetadata(ChainingMetadataRepository.java:55)
```

#### Connecting to the Teiid Virtual DB from a SQL Client

```
jdbc:teiid:karate.1@mm://localhost:31000
```

The JDBC URL starts with `jdbc:teiid:`. This is followed by the VBA Name and version, so `<VBA-NAME>.<VERSION>`. In our example, in the VBA XML we defined this: `<vdb name="Karate" version="1">`, which translates to `Karate.1` for the JDBC URL. Also note that you have to use the **Application user** credentials (not the **Admin user** credentials).

![](./img/pdi-data-services-11.png)

#### How to resolve problems

If you are having troubles establishing a connection, it is best you stop the server and start it in debug mode to see more detailed error messages:

```bash
sh ./bin/standalone.sh -c=standalone-teiid.xml --debug
```

You might find error messages like this one:

```bash
21:01:43,445 INFO  [org.teiid.RUNTIME] (MSC service thread 1-5)  TEIID50029 VDB Karate.1 model "Karate" metadata is currently being loaded. Start Time: 10/05/16 21:01
21:01:43,453 WARN  [org.teiid.RUNTIME] (Worker0_async-teiid-threads0)  TEIID50036 VDB Karate.1 model "Karate" metadata failed to load. Reason:TEIID31097 Metadata load requires a connection factory: TEIID30481 Failed to find the Connection Factory with JNDI name java:/pdi-datasource-karate-connector. Please check the name or deploy the Connection Factory with specified name.: org.teiid.translator.TranslatorException: TEIID31097 Metadata load requires a connection factory
	at org.teiid.query.metadata.NativeMetadataRepository.loadMetadata(NativeMetadataRepository.java:55)
	at org.teiid.query.metadata.ChainingMetadataRepository.loadMetadata(ChainingMetadataRepository.java:55)
```

#### Final Remarks

As you can see, it is fairly easy to set up Virtualised Tables in Teiid. We only took a look at defining JDBC data sources. In his Meetup Demo Edwin also demoed accessing the PDI Web Service via Teiid, effectively eliminating the need to copy JDBC drivers to the web server. 

### Using Teiid Designer

There is also a very good [Examples](https://docs.jboss.org/author/display/teiidexamples/Examples) page with instructions on how to connect to various DBs, XML, Flat files, web services etc. We will follow [this guide](https://developer.jboss.org/wiki/HowToModelJDBCDataSource):

Open **Teiid Designer**


