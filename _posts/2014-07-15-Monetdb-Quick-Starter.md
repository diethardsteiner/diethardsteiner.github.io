---
layout: post
title:  "Installing Columnar DB MonetDB"
summary: Very brief instructions on how to install MonetDB
date:   2014-07-15
categories: Pentaho
tags: Pentaho Database
published: true
---

In preparation to my presentation at the [Pentaho London User Meetup](http://www.meetup.com/Pentaho-London-User-Group/events/178590472/) on the 22nd of July 2014 I'd like to share some brief instructions on to set up **columnar** database **MonetDB**. In my example I set it up on my laptop which is running **Fedora**, so the first step might slightly differ for you depending on your OS.

**MonetDB** is an **open source project** and hence available free of charge. It is developed at the **Centrum Wiskunde & Informatica (CWI)** in the **Netherlands**. More info can be found on the [Wikipedia entry](http://en.wikipedia.org/wiki/MonetDB).

# Download and Install MonetDB

The instructions vary by OS, please find specific instruction on the [MonetDB website](https://www.monetdb.org/downloads/).

For **Fedora**:

```shell
sudo yum install http://dev.monetdb.org/downloads/Fedora/MonetDB-release-1.1-1.monetdb.noarch.rpm
sudo yum install MonetDB-SQL-server5 MonetDB-client
```

# Create DB farm

We start by creating our DB farm:

```shell
monetdbd create ~/myMonetDBFarm
monetdbd get all ~/myMonetDBFarm
```

Set the port number, check that this is reflected in the settings and then start the DB farm:

```shell
monetdbd set port=54321 ~/myMonetDBFarm
monetdbd get all ~/myMonetDBFarm
monetdbd start ~/myMonetDBFarm
```

# Create dedicated DB

Make sure you pay attention to the command line utility name: In this case we are using `monetdb` and not the deamon `monetdbd` ... notice the **d** at the end! This is quite often the first hurdle users are confronted with when installing **MonetDB**.

```shell
monetdb create sls
monetdb start sls
monetdb release sls
monetdb status
```

# Create dedicated user and schemata

We will create a user named *etl*, 3 schemata called *dma*, *agile* and *etl* and finally assign a default schema to user *etl*:

```shell
mclient -p54321 -umonetdb -dsls
```

When promted for the password, the default password is: *monetdb*

```
CREATE USER etl WITH PASSWORD 'etl' NAME 'etlsuperuser' SCHEMA sys;
CREATE SCHEMA dma AUTHORIZATION etl;
CREATE SCHEMA agile AUTHORIZATION etl;
CREATE SCHEMA etl AUTHORIZATION etl;
ALTER USER etl SET SCHEMA dma;
```

# MonetDB Client Utility 

And now we can create tables etc and query them by using the handy command line utility:

```shell
 mclient -p54321 -umonetdb -dsls
```

For more detailed info I recommend taking a look at the [MonetDB Documentation](https://www.monetdb.org/Documentation).
