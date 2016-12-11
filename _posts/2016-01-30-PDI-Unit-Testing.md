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

### The extremely simple way

Download the jar file [from the latest release](https://github.com/mattcasters/pentaho-pdi-dataset/releases) and copy it into `<pdi-root-dir>/plugins/pentaho-pdi-dataset`. Job done.

### Building it yourself

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

You can now run the unit tests in Spoon! Command line options will be added at a later stage.

So when you install the latest version, you get a *Click for unit test options* label in the top left corner, but when you left click on it nothing happens. It turns out, actually by just right clicking anywhere on the canvas, you can access the **Unit tests** menu:

![](/images/pdi-datasets-p2-1.png)

Note: Adding Unit tests via the standard menu bar (**Tools > Datasets > Transformation unit tests**) is still not working.

The workflow should be:

1. Right click anywhere on the canvas and choose **Unit tests > Create unit test**.
2. Right click on the step which should be populated by the unit test input dataset and choose **Data set > Input data: Select a data set**.
3. Right click on the step which should be populated by the unit test output dataset and choose **Data set > Golden data: Select a data set**.
4. Run the transformation in Spoon and you should see the result of the unit test in the log:

![](/images/pdi-datasets-p2-2.png)

There have been quite a few changes. Matt published a new [video on YouTube](https://youtu.be/9bYIUURjf20) to speak you through the new features. 

# Update November 2016

Recent additions to the plugin are:

## New Unit Test Icon with Shortcuts

Now there is a new narrow neck flask icon in the top right hand corner of the canvas for each transformation which provides **shortcuts to unit testing features**:

![Screen Shot 2016-11-22 at 16.53.49](/images/Screen%20Shot%202016-11-22%20at%2016.53.49.png)

## Tweek Unit Testing Features

There is also a new option to **exclude specific steps** within a transformation **from the unit tests**: This can be configured by right clicking on a step and choosing **Unit test > Enable tweek: Remove step in unit test**. An X icon will then be shown on the top right hand side corner of the step icon:

![Screen Shot 2016-11-22 at 17.06.26](/images/Screen%20Shot%202016-11-22%20at%2017.06.26.png)

> **Note**: You might wonder what happens if you disable a step which is part of the main stream, imagine a step which calculates the value of the a new field? Here you have to keep in mind that for the **golden dataset** you also define a **mapping** between the real output dataset and the golden dataset. In case you disable the step which generates the calculated field, you simply do not specify the calculated field in the mapping and all will work fine.

## Execute Unit Tests Transformation Step

Finally, and most importantly, there is a new step to execute unit tests called **Execute unit tests**. It is super easy to use: There is no input required - it will automatically source all the **unit tests** from the **Metastore**. You can filter for certain **types** of tests (Conceptual, Development, Unit Test):

![Screen Shot 2016-11-22 at 20.49.15](/images/Screen%20Shot%202016-11-22%20at%2020.49.15.png)

Sample transformation with output shown:

![Screen Shot 2016-11-22 at 20.49.55](/images/Screen%20Shot%202016-11-22%20at%2020.49.55.png)

## Sort Order

When attaching a dataset to a step, you can now also define the **sort order**. This is particulary important for **MapReduce Input** and other steps which rely on a given sort order.

## Replace Database Connection

On per unit test level you can replace the original database connection(s) defined in the transformation with one for unit testing:

![Screen Shot 2016-11-24 at 22.01.38](/images/Screen%20Shot%202016-11-24%20at%2022.01.38.png)

This feature becomes available when you edit an existing unit test definition.

# Other Notes

## Quick Reference on how to set up a Unit Test

This approach uses the current input and output for the unit tests as well, which might not always be the best way. It works if you are developing locally with a sample dataset and want to use this one for your unit tests as well. Note: You can also just assign other/existing datasets as well. 

1. Click on the narrow narrow neck flask icon and choose **New**. Provide a name for the **unit test**, e.g. `AC1` (for acceptence test 1).

	![Screen Shot 2016-11-22 at 16.53.49](/images/Screen%20Shot%202016-11-22%20at%2016.53.49.png)

2. For the unit test datasets we will need a dataset group: **Tools > Data set > Data set groups > Add data set group**. Define the **name**, e.g. `ACs`, the **database connection** and the **schema name** (if required):
	
	![Screen Shot 2016-11-24 at 20.46.53](/images/Screen%20Shot%202016-11-24%20at%2020.46.53.png)

	
3. Right click on the main data input step and choose **Unit test > Define dataset with step output**. Provide a **name**, e.g. `AC1-Input`, a **table name**, e.g. `ac1_input` and choose a **dataset group**, e.g. `ACs`. Edit the **column names** so they are in line with the **field names**. Then click on **Create Table**, adjuste the DDL if necessary and click **Execute**. Click **Close** followed by **Ok**:
	
	![Screen Shot 2016-11-24 at 20.56.38](/images/Screen%20Shot%202016-11-24%20at%2020.56.38.png)
	
4.  Right click on the main data input step and choose **Unit test > Write data from step to dataset**, next pick the dataset and click **OK**. Next define the **mapping** (make sure you do this as otherwise PDI will write out empty records!) and thereafter the transformation will be executed and your database table should have the sample data now.
5. Right click on the main input data step, select **Unit test > Input data: select dataset** and pick the one we just created. Define the **Mapping** and **Sort Order**. Your input data step should be flagged with the unit test dataset now:
	
	![Screen Shot 2016-11-24 at 21.07.55](/images/Screen%20Shot%202016-11-24%20at%2021.07.55.png)
	
6. Repeat step 3 to 5 for the main output step, just instead of the input data you define the **golden dataset**. 
7. Your transformation should have these three elements for the unit test set now:
	
	![Screen Shot 2016-11-24 at 21.10.22](/images/Screen%20Shot%202016-11-24%20at%2021.10.22.png)

Sample files for this setup are available on [my Github folder](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/pdi/pentaho-pdi-dataset).


## How to disable Unit Tests for a given Transformation

Simply click on the narrow neck flask icon and choose **Disable**:

![Screen Shot 2016-11-22 at 18.47.21](/images/Screen%20Shot%202016-11-22%20at%2018.47.21.png)

## Investigating the ETL transformation generated for the Unit Test

Under the hood, and in memory, **PDI** generates a transformation for your unit test. Based on your specification certain steps of the original transformations will be replaced. In some sitatuations, especially while debugging, it might be worth checking the unit testing transformation PDI generates. This can be done by clicking on the narrow neck flask icon and choosing **Edit/Tweek**:

![Screen Shot 2016-11-22 at 20.38.55](/images/Screen%20Shot%202016-11-22%20at%2020.38.55.png)

In the next dialog you can specify the output location for the generated transformation:

![Screen Shot 2016-11-22 at 20.40.27](/images/Screen%20Shot%202016-11-22%20at%2020.40.27.png)

Sample generated transformation:

![Screen Shot 2016-11-22 at 20.43.02](/images/Screen%20Shot%202016-11-22%20at%2020.43.02.png)


## How to save the Unit Tests in a given Project Folder

By default the **unit test definitions** are stored in the **Pentaho Metastore** under the user's home directory:

```
~/.pentaho/metastore/pentaho/Kettle\ Transformation\ Unit\ Test
```

This might not be a convenient location in a lot of situations, as sometimes you might want to bundle all project related files in one place. Fortunately there is an environment variable called `PENTAHO_METASTORE_HOME`, which allows you to set a custom directory.

> **Note**: You might be familiar with the `KETTLE_HOME` variable, which stores `.kettle` in a convenient location. The metastore is by default stored in `~/.pentaho/metastore`. Using the `PENTAHO_METASTORE_HOME` variable makes it possible to define a custom location for this folder. Note tough that not the full folder hierarchy will be replicated (just `metadata` and not `.pentaho/metastore`).

You will have to change `spoon.sh` to take this variable into account. Simple add ` -DPENTAHO_METASTORE_FOLDER=$PENTAHO_METASTORE_FOLDER` to the end of the `OPT=` section:

Before:

```
OPT="$OPT $PENTAHO_DI_JAVA_OPTIONS -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2 -Djava.library.path=$LIBPATH -DKETTLE_HOME=$KETTLE_HOME -DKETTLE_REPOSITORY=$KETTLE_REPOSITORY -DKETTLE_USER=$KETTLE_USER -DKETTLE_PASSWORD=$KETTLE_PASSWORD -DKETTLE_PLUGIN_PACKAGES=$KETTLE_PLUGIN_PACKAGES -DKETTLE_LOG_SIZE_LIMIT=$KETTLE_LOG_SIZE_LIMIT -DKETTLE_JNDI_ROOT=$KETTLE_JNDI_ROOT"
```

After:

```
OPT="$OPT $PENTAHO_DI_JAVA_OPTIONS -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2 -Djava.library.path=$LIBPATH -DKETTLE_HOME=$KETTLE_HOME -DKETTLE_REPOSITORY=$KETTLE_REPOSITORY -DKETTLE_USER=$KETTLE_USER -DKETTLE_PASSWORD=$KETTLE_PASSWORD -DKETTLE_PLUGIN_PACKAGES=$KETTLE_PLUGIN_PACKAGES -DKETTLE_LOG_SIZE_LIMIT=$KETTLE_LOG_SIZE_LIMIT -DKETTLE_JNDI_ROOT=$KETTLE_JNDI_ROOT -DPENTAHO_METASTORE_FOLDER=$PENTAHO_METASTORE_FOLDER"
```

## It's possible to create assign a dataset to a step without setting up a unit test - is this valid?

Yes. The **PDI Datasets** plugin was not only create for unit testing. You can as well just assign a dataset to a step in scenarios where it is otherwise difficult to design the transformation because input data is missing, e.g. for Pentaho Map Reduce, where you could assign a dataset to a **MapReduce Input** step.

## Unit Tests, Datasets and Dataset Groups and their relationship to Transformations

A **Unit Test** is specific to **one** transformation.
A **Dataset** can be used in **various** transformations.
A **Dataset** is a child of a **Dataset Group**, which has the **Database Connection** defined for the related **Datasets**.




