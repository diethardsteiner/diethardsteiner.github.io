---
layout: post
title:  "Pentaho Dashboard Framework Basics"
date:   2014-06-25
categories: Dashboards
tags: Pentaho CDF CDE Dashboards
published: true
---

# Getting Started

## Installing the plugin

You can conveniently install the C-Tools (CDF, CDA, CDE ...) from the **Pentaho User Console** (which is the web-interface of the **Pentaho BA Server**) using the **Marketplace**.

If you are more a friend of running scripts, you can download the **c-tools-installer** from [Pedro Alve's Github account](https://github.com/pmalves/ctools-installer).

## Folder strucutre of plugin

In the `pentaho-solutions/system/pentaho-cdf` directory you find following interesting files and folders:

folder or file | description
---------------|------------
css | includes *CSS* files for **blueprint** and **bootstrap**
js | contains files of various *JavaScript* frameworks (i.e. **bootstrap** and **jquery**) and also custom files of **Components** and **Dashboards.js**. Inside the **components** subfolder you can find the code for the *core* (often used dashboard components), *maps* and *navigation* components in example.
lib | contains the java binary files.
resources | contains various other resources like *images*.
resources\*.properties | list all *JavaScript* and *CSS* resources that will be included in the dashboard HTML document.
template-dashboard.html and template-dashboard-\*.html | The first one is the standard **outer/document template** which gets applied to your dashboard content. Other templates have to be named following this pattern: template-dashboard-\*.html

If you want to have a deeper understand of which external frameworks a special component is using, this folder is the right place to start your search.

The `pentaho-solutions/system/pentaho-cdf-dd` folder is part of the **Community Dashboard Designer** (CDE).

## Files that make up your dashboard
If you are a purist and don't want to use the **Community Dashboard Editor**, you could as well code everything. You can create a folder inside the `pentaho-solutions` folder and include in the most basic form following files:

file | description
-----|------------
\*.xcdf | This is an XML file discribing the dashboard as well as defining the basic settings: The `template` tag references the **content template**, the `Style` tag references the **outer template**.
\*.html | This is a partial `html` document, sometimes referred to as the **Content Template**. The pure html part is basically a grid made out of div elements (or similar) with specific ids which will hold your dashboard components (plus possibly some static html content) and `JavaScript` object definitions of your dashboard components.

Here some examples (code taken from the Pentaho BI Server examples):
xcdf file:

```
<?xml version="1.0" encoding="UTF-8"?>
<cdf>
	<title>Pentaho Home Dashboard</title>
	<author>Webdetails</author>
	<description>Pentaho Home Dashboard</description>
	<icon></icon>
	<template>template.html</template>
	<style>mantle</style>
</cdf>
```

content template template.html (shortened for brefity):

```
<SCRIPT LANGUAGE="JavaScript">
function clickOnTerritory(value) {
	productLine = "null";
	Dashboards.fireChange('territory',value);
}
</SCRIPT>
<table align="center" style="border: 1px solid #000;">
	<tr>
		<td align="center" colspan="2"><p><div id="titleObject"></div></p></td>
	</tr>
	<tr>
		<td>
			<table>
				<tr>
					<td valign="top"><div id="territorySalesObject"></div></td>
				</tr>
				<tr>
					<td valign="top"><div id="productLineSalesObject"></div></td>
				</tr>
			</table>
		</td>
	</tr>
</table>

<script language="javascript" type="text/javascript">

var productLine = "null";
var territory = "null";

titleString = 
{
  name: "titleString",
  type: "text",
  title: "Top Ten Customers",
  listeners:["productLine", "territory"],
  htmlObject: "titleObject",
  executeAtStart: true,
  postExecution:function(){}
}

territorySales = 
{
  name: "territorySales",
  type: "xaction",
  path: "/public/plugin-samples/pentaho-cdf/20-samples/home_dashboard/territorySales.xaction",
  listeners:[],
  parameters: [],
  htmlObject: "territorySalesObject",
  executeAtStart: true,
  preExecution:function(){},
  postExecution:function(){}
}

productLineSales = 
{
  name: "productLineSales",
  type: "xaction",
  path: "/public/plugin-samples/pentaho-cdf/20-samples/home_dashboard/productLineSales.xaction",
  listeners:[],
  parameters: [],
  htmlObject: "productLineSalesObject",
  executeAtStart: true,
  preExecution:function(){},
  postExecution:function(){}
}

var components = [titleString, topTenCustomers, territorySales, productLineSales];
</script>
<script language="javascript" type="text/javascript">
function load(){
	Dashboards.init(components);
}
load();
</script>
```

If this scared you off now, don't you worry, there is a the **Community Dashboard Editor** available, which allows you to create a Dashboard via an easy to use GUI.

## Where to store your own files
Once you start creating your dashboard, you might create your own templates, CSS, JavaScript and images as well. Now it is not a good idea to store these artefacts within the plugin folder, because there is a good chance that they get wiped out with the next update.

You can put your resources directly under the `pentaho-solutions` folder.

# Customization

## Creating your own outer/document template

Take a look at any of the template files in `pentaho-solutions/system/pentaho-cdf`. It should be fairly easy to understand how to create your own templates based on this.
> **Outer/document templates** are used to define common artifacts that are used across dashboards. A typical example of this is a navigation, images, styles etc.

You basically prepare a skeleton of an HTML document and use placeholders for the CDF entities, in example: `{content}`, `{isAdmin}`, `{isLoggedIn}`.

The outer template can be referenced within the `.xcdf` file using the `Style` tag. Make sure you only use the costum name of your template: If your template is named `template-dashboard-dwh.html`, then reference `dwh` for the `Style`.

The outer template is complemented by the inner/**content template**. An example was shown further up in the article.

What you read here is these days mostly relevant as background info. Use the **Community Dashboard Editor** to design your dashboards in an effective manner.
