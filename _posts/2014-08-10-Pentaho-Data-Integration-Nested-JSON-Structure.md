---
layout: post
title:  "Creating nested JSON structures in Pentaho Data Integration"
summary: This article discusses creating nested JSON structures with Pentaho Kettle.
date:   2014-08-10
categories: JSON
tags: Pentaho
published: true
---

**Pentaho Data Integration** features dedicated **JSON** *Input* and *Output* steps. The **JSON Output** step currently only supports the output of a flat structure. I submitted a Jira Improvements case, so if we are lucky, **PDI** will soon support this feature.

In the meantime however, the situation is not hopeless. **PDI** offers various **scripting** steps, at least one of which is interesting for our adventure: The **Modified Java Script Value** step. You can think of it as a **server-side Javascript** executor. So if you are a bit familiar with **Javascript**, this will be a piece of cake. 

Imagine the output should look something like this:

	{
		"configId": "2067bd4f-6cec-4d8e-b1a5-b99a07b1b268",
		"dbConnection": "SampleData",
		"dbSchema": "PUBLIC",
		"dbTable": "OFFICES",
		"metadata": [
			{
			"colIndex": 1,
			"colType": "String",
			"colName": "OFFICECODE",
			"isVisible": true,
			"isEditable": false,
			"isPrimaryKey": true
			},
			{
			"colIndex": 2,
			"colType": "String",
			"colName": "CITY",
			"isVisible": true,
			"isEditable": true,
			"isPrimaryKey": false
			},
			{
			"colIndex": 3,
			"colType": "String",
			"colName": "PHONE",
			"isVisible": true,
			"isEditable": true,
			"isPrimaryKey": false
			}
		]
	}

This is basically the kind of configuration detail I want to save with my **Sparkl** project. **PDI** is ideal in this case, as I can run the logic on the server side.

So imagine that our transformation receives following parameters and values:


Parameter | Default Value | 
----------|---------------|
param_db_columns_is_editable | firstname,lastname | 
param_db_columns_is_primary_key | id | 
param_db_columns_is_visible | id,firstname,lastname | 
param_db_columns_name | id,firstname,lastname | 
param_db_columns_position | 1,2,3 | 
param_db_columns_type | Integer,String,String | 
param_db_connection | psqllocaltest | 
param_db_schema | public | 
param_db_table | employees | 

We source them in our tansformations using the **Get Variables** step:

Name | Variable | Type 
-----|----------|-------
dbConnection | ${param_db_connection} | String
dbTable | ${param_db_table} | String
dbSchema | ${param_db_schema} | String
dbColumnsName | ${param_db_columns_name} | String
dbColumnsType | ${param_db_columns_type} | String
dbColumnsPosition | ${param_db_columns_position} | String
dbColumnsIsVisible | ${param_db_columns_is_visible} | String
dbColumnsIsEditable | ${param_db_columns_is_editable} | String
dbColumnsIsPrimaryKey | ${param_db_columns_is_primary_key} | String

And then we can use a **User Defined Java Expression** step to create a **UUID** on the fly and finally the **Modified Java Script Value** to create the **nested JSON** structure:

```javascript
function findInArray(myValue, myArray){
	var myResult='';
	if(myArray.indexOf(myValue) > -1){
		myResult = true;
	} else {
		myResult = false;
	}
	return myResult;
}

var json = {};

// connection details
json.configId = uuid;
json.dbConnection = dbConnection;
json.dbSchema = dbSchema;
json.dbTable = dbTable;
json.metadata = []; // create array to store column definition in it later on

// table metadata
var dbColumnsNameArray = dbColumnsName.split(',');
var dbColumnsTypeArray = dbColumnsType.split(',');
var dbColumnsPositionArray = dbColumnsPosition.split(',');
var dbColumnsIsVisibleArray = dbColumnsIsVisible.split(',');
var dbColumnsIsEditableArray = dbColumnsIsEditable.split(',');
var dbColumnsIsPrimaryKeyArray = dbColumnsIsPrimaryKey.split(',');


for(i=0; i<dbColumnsNameArray.length; i++){
	var colDetails = {};
	colDetails.colIndex = dbColumnsPositionArray[i];
	colDetails.colType = dbColumnsTypeArray[i];
	colDetails.colName = dbColumnsNameArray[i];
	colDetails.isVisible = findInArray(dbColumnsNameArray[i], dbColumnsIsVisibleArray);
	colDetails.isEditable = findInArray(dbColumnsNameArray[i], dbColumnsIsEditableArray);
	colDetails.isPrimaryKey = findInArray(dbColumnsNameArray[i], dbColumnsIsPrimaryKeyArray);
	// add to metadata array
	json.metadata.push(colDetails);
}

myDocFinal = JSON.stringify(json);
```

The first function just checks if a given array contains a value of another array. The important bits here are that we just create a basic object called json using `var json = {};`. Then we add the attributes using the dot notation, e.g. `json.configId = uuid;`. We also add an array called *metadata*: `json.metadata = [];`.

Next we prepare the objects which will be stored in the metadata array and finally add them to the metadata array using `json.metadata.push(colDetails);`.

In the end we have to convert our **JSON object** to a **String** by using `JSON.stringify(json);`. This is the field we define as output of this step.

Easy right? If you know a bit of **JavaScript**, requirements like this can be fairly easily solved in Pentaho Kettle/PDI.

For my **Sparkl** project I also had to add the **JSON object** we created above to an existing document, which holds similar objects within an array.

In this case I just wanted to source the **JSON file** uninterpretated. One simple way of doing just this is to just use the **Modified Java Script Value** step. Insert the following code:

```javascript
// source existing JSON file
var workingDir = getEnvironmentVar('Internal.Transformation.Filename.Directory');
var existingJsonFilePath = workingDir + '/myFile.json';
var existingJson;

if(isFile(existingJsonFilePath)){
	var existingJson = JSON.parse(loadFileContent(existingJsonFilePath));
} else {
	existingJson = [];
}

existingJson.push(json);

var myDocFinal = JSON.stringify(existingJson);
```

E voila, we get a nested JSON document like this one:

```javascript
[
   {
      "configId":"2067bd4f-6cec-4d8e-b1a5-b99a07b1b268",
      "dbConnection":"SampleData",
      "dbSchema":"PUBLIC",
      "dbTable":"OFFICES",
      "metadata":[
         {
            "colIndex":1,
            "colType":"String",
            "colName":"OFFICECODE",
            "isVisible":true,
            "isEditable":false,
            "isPrimaryKey":true
         },
         {
            "colIndex":2,
            "colType":"String",
            "colName":"CITY",
            "isVisible":true,
            "isEditable":true,
            "isPrimaryKey":false
         },
         {
            "colIndex":3,
            "colType":"String",
            "colName":"PHONE",
            "isVisible":true,
            "isEditable":true,
            "isPrimaryKey":false
         }
      ]
   },
   {
      "configId":"e0c9d436-979d-449e-81ae-64cba6530062",
      "dbConnection":"psqllocaltest",
      "dbSchema":"public",
      "dbTable":"employees",
      "metadata":[
         {
            "colIndex":"1",
            "colType":"Integer",
            "colName":"id",
            "isVisible":true,
            "isEditable":false,
            "isPrimaryKey":true
         },
         {
            "colIndex":"2",
            "colType":"String",
            "colName":"firstname",
            "isVisible":true,
            "isEditable":true,
            "isPrimaryKey":false
         },
         {
            "colIndex":"3",
            "colType":"String",
            "colName":"lastname",
            "isVisible":true,
            "isEditable":true,
            "isPrimaryKey":false
         }
      ]
   }
]
```
