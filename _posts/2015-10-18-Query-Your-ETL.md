---
layout: post
title: Run SQL Queries on your ETL output
summary: 
categories: PDI
tags: PDI
published: true
---

Usually when you write an **ETL process** the output is stored somewhere (in a file or in a database). Some time ago Pentaho opened up a whole lot of new possibilites by letting you define the last step of your ETL process as a **Data Service**. What does this mean exactly?

There are many Business Analysis / Intelligence tools available that connect to a data source via a **JDBC** driver. Ususally an engine within these tools will generate some **SQL** statements and send it to a database using this JDBC driver. **Now what if you could just query your ETL directly?** 

Imagine you want to join various data sources, be it results from a database query with the latest exchange rate (sourced via a REST API call) and make the result available without much work. Some time ago **Pentaho** developed a thin **JDBC** driver for **Pentaho Data Integration** processes (available since version 5.0) ([Wiki Entry](http://wiki.pentaho.com/display/EAI/The+Thin+Kettle+JDBC+driver)). This means that you only have to add this driver to your BI tool and point the connection string to your ETL transformation. 

This feature was available in the Enterprise Edition only, but as of version 6, which was released just a few days ago, this feature is available in the **Community Edition** as well!

## How it works

You first provide a transformation which provides the data. This transformation has to be stored on the **Pentaho DI Server** or **Carte Server**, so that it is accessible from anywhere. When the **client** tool, which has the Kettle **JDBC Driver** installed, fires off a query, the PDI process on the **server** translates the SQL statement into a Kettle transformation on the fly: So basically it generates a Kettle transfromation, which will e.g. aggregate, sort and filter the data coming from the ETL transformation you defined as **Data Service**. You can actually see which kind of transformations PDI generates by looking at the **Carte** log or via the **slave monitor** in **Spoon**. ([Wiki Entry](http://wiki.pentaho.com/display/EAI/Architecture+of+the+Thin+Kettle+JDBC+Driver)).

To define a transformation step as a **Data Service**, simply open up the **Transformation Properties** and click on the **Data Service** tab. There you can define a **Service Name** and choose  a **Service step**. Once you save this transformation, an entry will be created in the local metastore (located in `~/.pentaho/metastore`) or in case of the EE version in the DI server metastore (located usually in `/etc/metastore`). Because of this **Carte** and the **DI Server** will automatically pick up these data services.

## Configuration

### Configuring the Carte Server

I will focus on the Community Edition here, instruction for the EE can be found [here](http://wiki.pentaho.com/display/EAI/Configuration+of+the+Thin+Kettle+JDBC+driver).

### Configuring the client