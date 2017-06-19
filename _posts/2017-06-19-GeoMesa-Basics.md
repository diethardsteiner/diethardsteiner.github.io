---
layout: post
title: "Big Data Geospatial Analysis with Apache Spark, GeoMesa and Accumulo - Part 2: Basics"
summary: This article walks you through the basics of Accumulo and GeoMesa.
date: 2017-06-19
categories: Geospatial
tags: Geospatial, Spark
published: true
---  

In **Part 1** we had a look at how to set up **GeoMesa**. Now we can have a look at some practical examples. Note that some sections were copied directly from the **GeoMesa** and other help pages. My main aim was to provide a consistent overview.

## Accumulo

### Basics

[Source](https://github.com/acordova/accumulo-examples)

Typically applications map existing data or programmatic objects to **Key** and **Value** objects. Keys are broken into the following components, in order:

    - Row
    - Column Family
    - Column Qualifier
    - Column Visibility
    - Timestamp

> **Note**: These are all the fields that make up one record! There can be more than one record for a given key, the attributes must be different then ... so it is basically like transforming a normal relational record into several key-value pairs. E.g. Record: id:1, fruit:banana, sales: 12  would be transformed into `[Key: (1, attr:banana), Value: 0],  [Key: (1, attr:sales), Value: 12]`.

All **Keys** that have the same **Row** are considered to be one **logical row**, and are stored next to each other in a table. Thus reading all the information in a single row can be done by looking up the first **Key** in the row and scanning the following Key Value pairs that share the same Row.

Key Value pairs are sorted by Key, and Keys in a table are sorted by comparing the Row, then Column Family, and so on.

To write information to a table in Accumulo, changes to a single Row are packaged up into a **Mutation**. Changes are added to a Mutation via the `put()` method.

> **Namespace**: the concept of table namespaces was created to allow for logical grouping and configuration of Accumulo tables. By default, tables are created in a default namespace which is the empty string to preserve the feel for how tables operate in previous versions. One application of table namespaces is placing the Accumulo root and metadata table in an Accumulo namespace to denote that these tables are used internally by Accumulo.

### Accumulo Shell

Let's connect to the **Accumulo Shell** and use a few commands:

```
accumulo shell -u root -p password
# leave password empty
tables
namespaces
# select * from table
scan <tableName>
# show config
config -t <tableName>
# change to table
tables
table <tableName> 
# show a single record
select <row> -st -t <tableName>
selectrow <row> -st -t <tableName>
```

Typical table search ([Source](https://github.com/acordova/accumulo-examples)):

```
# switch to table
table <tableName>
# show all records
scan
# show one record 
scan -r <key>
# show records for a particular attribute
scan -c attribute:name
```

## GeoMesa Basics

Let's try to understand the basic concepts:

- A **feature** has a set of attributes and associated types based on the `SimpleFeatureType` **specification** (resembles a DDL) 
- In the **GeoMesa context**, when you define a feature with its attributes, you create a **schema**. 
- A **feature** gets stored in a **catalog table**, which holds the metadata for the spatial data. 
- A **catalog table** can store **one or more features**. 
- When you store data in **GeoMesa**, there is not only one table (as in a traditional relational database), but several ones, each one with their dedicated purpose (one for metadata, one for records and then one for each index). All these tables get **prefixed** with the **catalog table** name. 
- Tables can be grouped into a **Namespace**, which is basically a synonym for Database Schema.
- The data gets **indexed** in different ways, for each index there is one table. 

By default, GeoMesa creates three indices ([docu](http://www.geomesa.org/documentation/1.2.3/user/data_management.html)):

- **Z2**: This index is used to answer queries **with a spatial component but no temporal component**.
- **Z3**: This index is used to answer queries **with both a spatial and temporal component**.
- **Record**: The record index stores features by feature ID. It is used for any query by ID.

The following indices can also be created ([docu](http://www.geomesa.org/documentation/current/user/datastores/index_structure.html)):

- **Attribute**: The attribute index uses attribute values as the primary index key. This allows for fast retrieval of queries **without a spatio-temporal component**.
- **XZ2**: This index will be created if the feature type has a **non-Point geometry**. This is used to efficiently answer queries with a spatial component but no temporal component.
- **XZ3**: This index will be created if the feature type has a **non-Point geometry** and has a time attribute. This is used to efficiently answer queries with both spatial and temporal components.

[This section](http://www.geomesa.org/documentation/user/architecture.html#geomesas-index) in the official docu provides a bit more background info on how the **GeoMesa Index** is created on Accumulo.

When looking at **GeoMesa** resources you quite often come across **GeoTrellis** as well. So what is the difference? "**GeoMesa** is primarily **focused on vector data**.  For large volume of data, the Accumulo datastore is great.  An example dataset would be GDELT (which is on the 'small' side with ~200 million data points).  If you have streaming data, check out the GeoMesa Kafka datastore.  Streaming sensor data would make sense for this datastore. **GeoTrellis** is **focused on raster data**; using it for climatology predictions or landsat imagery would be typical." [Source](https://dev.locationtech.org/mhonarc/lists/geomesa-users/msg01821.html)


## Command Line

- [Command Line Examples](http://www.geomesa.org/documentation/user/accumulo/examples.html)

### Creating a feature type 

To begin, let's start by creating a new **feature type** in **GeoMesa** with the `create-schema` command (used to be the `create` command). The `create-schema` command takes three arguments and one optional flag: 

**Required** 

- `-c` or `\--catalog`: the name of the catalog table
- `-f` or `\--feature-name`: the name of the feature 
- `-s` or `\--spec`: the `SimpleFeatureType` specification

**Optional** * `\--dtg`: the default date attribute of the `SimpleFeatureType` Run the command:             

```
$ geomesa create-schema \
    -u root \
    -c cmd_tutorial \
    -f feature \
    -s fid:String:index=true,dtg:Date,geom:Point:srid=4326 \
    --dtg dtg
``` 

This will create a new **feature type**, named "feature", on the GeoMesa catalog table `cmd_tutorial`. The catalog table stores metadata information about each feature, and it will be used to prefix each table name in Accumulo.

If the above command was successful, you should see output similar to the following:

```
Creating 'cmd_tutorial_feature' with spec 'fid:String:index=true,dtg:Date,geom:Point:srid=4326'. Just a few moments...
Feature 'cmd_tutorial_feature' with spec 'fid:String:index=true,dtg:Date,geom:Point:srid=4326' successfully created.
```

Now that you've seen how to create feature types, create another **feature type** on **catalog table** `cmd_tutorial` using your own first name for the `\--feature-name` and the above schema for the `\--spec`.

```
$ geomesa create-schema \
    -u root \
    -c cmd_tutorial \
    -f dst \
    -s fid:String:index=true,dtg:Date,geom:Point:srid=4326 \
    --dtg dtg
``` 

You should see the following:

```
INFO  Creating 'dst' with spec 'fid:String:index=true,dtg:Date,*geom:Point:srid=4326'. Just a few moments...
INFO  Created schema 'dst'
```

### Listing known feature types

You should have two feature types on catalog table `cmd_tutorial`. To verify, we'll use the `get-type-names` command. 

The `get-type-names` command takes one flag: 

- `-c` or `\--catalog`: the name of the catalog table 

Run the following command:

```
$ geomesa get-type-names -u root  -c cmd_tutorial
```

### Show attributes of a feature type 

To find out more about the attributes of a feature type, we'll use the `describe-schema` command. 

This command takes two flags: 

- `-c` or `\--catalog`: the name of the catalog table 
- `-f` or `\--feature-name`: the name of the feature type 

Let's find out more about the attributes on our first feature type. Run the command:

```
$ geomesa describe-schema -u root  -c cmd_tutorial -f feature
```

### Other examples

```
# list all known feature types in a GeoMesa catalog:
geomesa get-type-names -u root -c myNamespace.gdelt
# describe feature
geomesa describe-schema -u root -c myNamespace.gdelt -f gdelt
# List all known feature types in a GeoMesa catalog:
$ geomesa get-names -u username -p password -c test_catalog
```
