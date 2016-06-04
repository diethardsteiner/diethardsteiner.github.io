---
layout: post
title:  "Unit testing Pentaho Data Integration jobs and transformations"
summary: This article explains how to unit test Pentaho Data Integration jobs and transformations
date: 2016-01-30
categories: Big Data
tags: Big Data, PDI
published: true
---

About a year ago **Matt Casters**, lead developer of **Pentaho Data Integration** (PDI), managed to get the approval by **Pentaho** to open source one of his internal projects focusing on adding **unit testing** functionality to PDI. This is a first look at what is available so far (as of Feb 2016 it is not feature complete), which should provide you a brief understanding of what has been achieved so far and hopefully get you on board to support this project!

## Preparing the Sample

Start **Spoon** and create a new **transformation**.

> **Note**: There is currently a bug with the `pentaho-pdi-dataset` plugin which will make the **Preview** feature in **Spoon** fail (see [bug report](https://github.com/mattcasters/pentaho-pdi-dataset/issues/1)). Hence we will for now refrain from installing it as we have to prepare an example transformation. Once you are done with this tutorial, simply remove the plugin and all should be fine again.

For demonstration purposes, let's create an extremely simple **transformation**. Add the following steps in the specified order and connect them:

1. Table Input
2. Calculator
3. Select Values
4. Table Output

![](/images/pdi-unit-testing-4.png)

In a database of your choice (I use **PostgreSQL** for this example) create two tables as shown below (adjust SQL to your DB dialect):

```sql
CREATE TABLE test.inventory (
	item_name VARCHAR(120)
	, item_count INT
)
;

INSERT INTO test.inventory
VALUES
	('headphones', 20)
	, ('microphones', 17)
;

CREATE TABLE test.inventory_adjusted (
	item_name VARCHAR(120)
	, item_count INT
)
;
```

Back in **Spoon** create a new database connection so that we can access these new tables. 

In the **Table input** step specify this database connection and specify the following query:

```sql
SELECT * FROM test.inventory;
```

In the **Calculator** step simply add `1` to `item_count` and store the result in `item_count_new`. In the **Select values** step keep the `item_name` and `item_count_new` fields - rename the latter one to `item_count`. Finally, in the **Table output** step specify `inventory_adjusted` as the target table.


## Installation of the plugin

Close **Spoon**.

Clone the **Git** project in a convenient folder, e.g. `~/git`:

```bash
$ git clone https://github.com/mattcasters/pentaho-pdi-dataset.git
```

Then build the plugin:

```bash
$ cd pentaho-pdi-dataset
$ ant
```

Once the build process finished successfully, you will find a file called `pentaho-pdi-dataset-TRUNK-SNAPSHOT.jar` in the `dist` folder. Next create a folder inside your **PDI** installations plugin folder and copy this file there (example only, adjust accordingly):

```bash
$ cd /Applications/Development/pdi-ce-5.4/plugins
$ mkdir pentaho-pdi-dataset
$ cp ~/git/pentaho-pdi-dataset/dist/pentaho-pdi-dataset-TRUNK-SNAPSHOT.jar .
```

Start **Spoon**. Right click on a step on the canvas and you should see the **Data Set** option in the **context menu**:

![](/images/pdi-unit-testing-1.png)

## Creating a Data Set Group

> **Important**: For the next step to work, you have to **share** the database connection (otherwise the database connection pull down menu in the *Data Set Group* dialog will not be populated). Click on the **View** panel, expand the **Database connections** node and right click on the database connection you just created. Choose **share**:

![](/images/pdi-unit-testing-3.png)

Next choose **Tools > Data set > Add data set group**:

![](/images/pdi-unit-testing-2.png)

Provide the required information in the **Data Set Group** dialog:

![](/images/pdi-unit-testing-5.png)

> **Note**: The menu options to **Edit** or **Delete Data Set Groups** are currently not working. This doesn't mean, however, that you cannot do this at all. The workaround is to navigate on the **Terminal/Command Line** to the **Metastore** as shown below. The **Data Set Groups** are stored as **XML files** there. This way you can easily edit or even remove them:

```bash
$ ls -la  ~/.pentaho/metastore/pentaho/Kettle\ Data\ Set\ Group/
total 16
drwxr-xr-x  4 diethardsteiner  staff  136  6 Feb 15:51 .
drwxr-xr-x  4 diethardsteiner  staff  136  6 Feb 15:38 ..
-rw-r--r--  1 diethardsteiner  staff  214  6 Feb 15:38 .type.xml
-rw-r--r--  1 diethardsteiner  staff  570  6 Feb 15:51 inventory.xml
$ cat ~/.pentaho/metastore/pentaho/Kettle\ Data\ Set\ Group/inventory.xml 
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<element>
  <id>inventory</id>
  <value/>
  <type>String</type>
  <children>
    <child>
      <id>database_name</id>
      <value>psql-localhost-test</value>
      <type>String</type>
    </child>
    <child>
      <id>schema_name</id>
      <value>test</value>
      <type>String</type>
    </child>
    <child>
      <id>description</id>
      <value/>
      <type>String</type>
    </child>
  </children>
  <name>inventory</name>
  <security>
    <owner/>
    <owner-permissions-list/>
  </security>
</element>
```

## Creating a Data Set

You can create a new data set either via **Tools > Data Set > Data sets > Add data set** or by right clicking on **PDI step** and choosing **Data set > Define data set with step output**. The last option is quite convenient, as it not only allows you to define the columns of the data set, but also source the data from the step.

### Define Data Set with Step Output

Now **right click** on the **Table input** step and choose **Data set > Define data set with step output**:

![](/images/pdi-unit-testing-6.png)

In the **Data Set** dialog provide the name of the the data set, the **Table name** (`qa_inventory`) and the choose the **Data Set Group** (`inventory`). Now we have two options: If we had already created the table `qa_inventory`, we could press **Get Metadata**. In our case though, we want to first make sure that table with the field and column names etc is filled out correctly and then we can press **Create table**. The DDL / `CREATE TABLES` statement will be shown - if you are happy with it press **Execute**. Note that you can adjuste the DDL to your liking before executing it. Once the DDL was executed successfully, click **Close** and then **OK** in the **Data Set** dialog.

![](/images/pdi-unit-testing-7.png)

Finally, you have to link this data set to one particular step: Right click on the **Table output** step and choose **Data Set > Select an input data set**:

![](/images/pdi-unit-testing-11.png)

In the next dialog choose the data set we created earlier on. Then you should see an indicator next to the **Table input** step:

![](/images/pdi-unit-testing-12.png)

### Define a Data Set based on existing Table and Data

The previous approach is very useful if you already have some sample data in your dev environment. Sometimes you might, however, prepare your test data upfront, in which case you will have to define the dataset in a different way. Let us do just this for the final output - the **golden** dataset:

Execute the **SQL** statements below in your favourite SQL client (adjust to your DB dialect if required):

```sql
CREATE TABLE test.qa_inventory_adjusted (
	item_name VARCHAR(120)
	, item_count INT
)
;

INSERT INTO test.qa_inventory_adjusted
VALUES
	('headphones', 21)
	, ('microphones', 18)
;
```

From the main menu choose **Tools > Data Set > Data sets > Add data set**:

![](/images/pdi-unit-testing-8.png)

Provide all the required details in the **Data Set** dialog. Make sure that this time you do not click **Create Table** but instead click **Get Metadata**. If you are interested in what your testing data looks like, click **View**. Finally click **OK**:

![](/images/pdi-unit-testing-9.png)

## Create a Unit Test

Now there is just one task left to do: Create a **Unit Test**. Right
click on the the **Table Output** step and choose **Data Set > Create unit test**:

![](/images/pdi-unit-testing-13.png)

In the **Transformation unit test** dialog provide a name and choose the **Data set**. Then click **OK**:

![](/images/pdi-unit-testing-10.png)

The **Table output** step should have an indicator now:

![](/images/pdi-unit-testing-14.png)

And this is where it stops right now ... there is no GUI right now to run a unit test, but as per discussions with **Matt Casters** in Feb 2016 work on implementing this has begun, so I am really looking forward to trying this out! Matt has also fixed the preview bug, however, currently this code is not yet on Github. **Unit Testing** is a very important part of the development workflow, so please show this project some support!

OK, you have been real patient :-)

# Update May 2016

https://youtu.be/9bYIUURjf20  (still processing the full HD version right now)

https://github.com/mattcasters/pentaho-pdi-dataset/ (still open source but wait for labs initiative for press releases etc)

https://s3.amazonaws.com/kettle/pentaho-pdi-dataset-TRUNK-SNAPSHOT.jar (throw in plugins/)

Warning: not ready for prime time, yada yada, may kill you or blow up your nuclear reactor, etc.