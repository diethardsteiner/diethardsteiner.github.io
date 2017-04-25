---
layout: post
title: "Pentaho Data Integration: Automatically source Metadata for Metadata Injection"
summary: This article discusses various approaches on how to automatically source metadata from files and database tables to inject it later on into transformation templates
date: 2017-04-21
categories: PDI
tags: PDI, Metadata
published: false
---  


So PDI these days supports **Metadata Injection** for a variety of transformation steps. 

> **Metadata Injection**: In layman's terms ... Imagine you have to ingest 10 different spreadsheets. All have a different structure. Traditionally you would create 10 ETL transformations for this purpose. When the 11th spreadsheet comes along, you would have to create just another one. Well, that's not fun! Instead, these days you create a transformation template and just tell it what structure your spreadsheet is in (all the column names and types etc) - the metadata so to speak. So you have one transformation and can then import an indedinite number of different spreadsheets. This was just an example. This can be applied to anything (well, all the steps that support Metadata Injection in PDI, of which there are luckily many these days). Pure efficiency you might say!

That's all well and good - you might say - but I still have to manually supply the metadata?! I still can't build a fully automated solution, throw any content at it and it will process it? Like pure magic?! Yes, you can!

PDI comes bundled with a few steps that allow you to extract metadata from file or database tables. There are also several steps available on the Marketplace to cater for this need. We will discuss a few of these steps here:


## How to get file metadata

`Get File Names`


`File Metadata` (from the Marketplace)

[Apache Tika](https://tika.apache.org/) "toolkit detects and extracts metadata and text from over a thousand different file types (such as PPT, XLS, and PDF)".

## How to get database table metadata


`Get tables names from database`: Returns **Catalogs**, **Schemas**, **Tables**, **Views**, **Procedures** and/or **Synonyms**. The screenshot below shows the list of schemas return from a PostgreSQL database. Note that we disabled the retrieval of tables and views here as you cannot do this at the same time:

![](pdi-get-metadata-1.png)


A very classic approach has been to use a combination of the `Table input` step and `Metadata from Stream` step to get the most essential metadata. You would configure the **Table input** step to just source one record, something like this:

```sql
SELECT
  *
FROM myTable
LIMIT 1
```

This works fine and is the default approach in older versions of PDI.

`JDBC Metadata` (from the Marketplace)
