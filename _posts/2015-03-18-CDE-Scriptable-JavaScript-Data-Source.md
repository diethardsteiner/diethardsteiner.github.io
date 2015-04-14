---
layout: post
title: "CDE Prototyping: Scriptable JavaScript JSON Data Source"
date:   2015-03-18
categories: CDE
tags: CDE, CDA
published: true
---


For prototyping purposes I usually prepare small datasets. With **CCC** you can just prototype everything in a standard HTML file (which requires no connection to the Pentaho BA Server or any database): Here we just define an inline JSON style dataset (well, actually a JavaScript object to be precise). This works extremely well. This inline dataset looks exactly the same as the resultset a **CDA** query would return.

In **CDE** we have the **Scriptable** data source, which allows you to create an inline dataset using **Beanshell** (Java for the ordinary folks). This feature is mainly provided by the **PRD** libraries, so it works in **CDE** as well as in **Pentaho Report Designer**. The big drawback, however, is that the declaration is very verbose and will alienate anyone not familiar with **Java**.

Another point that interfered with my workflow was that I could not just copy the inline dataset from my **CCC** testing files to **CDE** (to continue prototyping there). This was the reason why I created the [Scriptable JavaScript Data Source](http://jira.pentaho.com/browse/CDA-119) Jira improvement case. And this request was implemented very quickly, this is why I write this article about this brand new feature.

We will build **CDA** from source as this feature is currently (2015-03-18) only available in the master branch. Execute the commands below (this assumes you have all the build tools installed):

```bash
git clone https://github.com/webdetails/cda.git
cd cda 
ant clean-all resolve publish-local
```

This will take some time to run. Brew yourself a fresh cup of coffee in the meantime. 

Once finished, you should see this close to the end of the log output:

```bash
BUILD SUCCESSFUL
```

Next run:

```bash
cd cda-pentaho5
ant clean-all resolve dist
```

Now we can add it to our developement server (this reads: do not use this in production for now!):

1. Delete **cda** folder in `<ba-server>/pentaho-solutions/system/`
2. Copy `<cda-source>/cda-pentaho5/dist/cda-TRUNK-SNAPSHOT.zip` into `pentaho-solutions/system/` and unzip it.
3. Restart the BA-Server and you are done!

In **CDE** under **Data Sources Panel > SCRIPTING Queries > jsonScriptable over scripting** we define the inline data source like this:

Set the **Language** in the **Properties** panel to `beanshell` and the **Query** to:

```javascript
{
	"resultset": [
		["2006 Q1", 242],
		["2006 Q2", 410],
		["2006 Q3", 340],
		["2006 Q4", 353]
	],
	"metadata": [
		{ "colIndex": 0, "colType": "String", "colName": "Year-Quarter" }
		,
		{ "colIndex": 1, "colType": "Numeric", "colName": "Value" }
	]
}
```

And that's it. You can now use this data source in your dashboard! Extremely useful for prototyping!