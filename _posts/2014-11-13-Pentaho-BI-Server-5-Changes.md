---
layout: post
title:  "Migrating from Pentaho BI Server v4 to v5: Path and API changes"
summary: This article is not meant to be a migration guide, but more a collection of various aspects of this migration that I thought might be worth noting and which I have not seen mentioned elsewhere.
date:   2014-11-13
categories: biserver
tags: Pentaho, Server
published: true
---


This article is not meant to be a migration guide, but more a collection of various aspects of this migration that I thought might be worth mentioning. 

## Repository Synchronizer

The Repository Synchronizer plugin allows you to upload, download or synchronize files from JCR to the good old file system.

Where do you put files that should be uploaded:
`pentaho-solutions/repositorySynchronizer`

For those people upgrading from biserver 4.8 or earlier, you will have to reorganize your folder structure a bit to go in line with the JCR structure. Create a `home` and a `public` folder (lower case!) on your file system and move your Pentaho solution files and folders into these directories. Note that the `home` directory should contain user specific folders whereas `public` should contain all the files, which are accessible to everyone. Then copy them into `pentaho-solutions/repositorySynchronizer` and you can now upload them via PUC **Tools > Repository Synchronizer** *Transfer to JCR from File System* option.

Once you uploaded the files, you might wonder why you cannot see any **CSS**, **JavaScript** etc files (non Pentaho file formats basically). As an **Admin** user you might want to see them. The way to achieve this is to active this via the main menu: **View > Show hidden files**. Also, not all files are supported out-of-the-box (see further down on how to add new data types).

## Import-Export Command Line Utility

An alternative way to upload (and also download) files is to use the `import-export.sh` located in the biserver root directory.

Example: Uploading files using the command line utility

```
./import-export.sh --import --url=http://localhost:8080/pentaho --username=admin --password=password --charset=UTF-8 --path=/public --file-path=/home/dsteiner/myfolder --overwrite=true --permission=true --retainOwnership=true
```

> **NOTE**: You can upload files, folders or zip files.

If you have to export something later on, you can use something similar to this:

```
./import-export.sh --export --url=http://localhost:8080/pentaho --username=admin --password=password --charset=UTF-8 --path=/public/Test --file-path=/home/dsteiner/myArchive.zip --overwrite=true --permission=true --retainOwnership=true
```

## CDE

Relative paths methods (all accepted):
`${res:/public/myFolder/myFile.css}`
`${solution:/public/myFolder/myFile.css}`


Path to a file in the solutions folder:

```
"path": "/public/project/dashboards-new/test.css"
```

Path to the CDF system folder:

```
path:'/pentaho/api/repos/pentaho-cdf-dd/<file>'
```					

In case you get following error:

```
Could not load dashboard: exception while getting data for file with id "7040380b-f476-44e3-9b92-94852f6f84ee" Reference number: 880a2d9e-3315-43df-9ddf-ab3cb9e9e871.
```

Harris suggested changing the **style** in the dashboard **Settings**. This prooved to be the right solution. If you want to reapply the same style, just click on another one and the on the old one again and only then save the file.

## CDA

Referencing Mondrian OLAP Schemas works now like this:

```
<Catalog>mondrian:/myOlapSchema</Catalog>
```

So do not use the full path any more! Instead the name defined in `pentaho-solutions/system/olap/datasources.xml` is used as the reference point.


## Paths

### Accessing Pentaho Solution files within the JCR repo

New way:

```
/pentaho/api/repos/:public:test:image.png/content
```


### Referencing Files in CDE Dashboards inside HTML snippets

Here is an example referencing an image in your CDE HTML code:

This still works:

```
<img src="res/test/image.png">
```

But there is a new way as well:

```
<img src="/pentaho/api/repos/:public:test:image.png/content”>
```

To access various files in your solution folder you can use the new **REST API**. 

Notice how above the actual solution path uses `:` instead of `/` and you have to add a suffix called `/content`.

**IMPORTANT NOTES**:

Imagine you create an extremly simple dashboard with only a layout structure defined, which holds an HTML element and you populate it like this:

```
<link rel="stylesheet" type="text/css" href="res/public/Test/test.css" />
<script type="text/javascript" src="res/public/Test/test.js"></script>
<img src="res/public/Test/test.png"></img>
<div class="title" onclick="test()">Hello</div>
```

Relative paths starting with `rel` will eventuelly end up looking like this one: 

```
http://localhost:8080/pentaho/plugin/pentaho-cdf-dd/api/resources/public/Test/test.png
```

If you do a **Preview** within **CDE** you will not see the styles applied nor will the JavaScript work. This works however, if you go to the **Browse Files** page, mark you dashboard file and choose **Open in new window**.

If you want the dashboard to display and work correctly in both **CDE Preview** and **standalone Preview**, then use the REST API references:

```
<link rel="stylesheet" type="text/css" href="/pentaho/api/repos/:public:Test:test.css/content" />
<script type="text/javascript" src="/pentaho/api/repos/:public:Test:test.js/content"></script>
<img src="/pentaho/api/repos/:public:Test:test.png/content"></img>
<div class="title" onclick="test()">Hello</div>
```

This rule doesn't seem to apply to JavaScript though. So if you've got some funky JavaScript that adds a CSS to your dashboard, you can see the effect only when you open the dashboard in a separate window. So both of these approaches have the same effect:

```
var cssUrl = "res" + Dashboards.context.path.replace(".wcdf",".css");
$("head").append("<link rel='stylesheet' href='" + cssUrl + "'>");
```

```
var cssUrl = "/pentaho/api/repos/" + Dashboards.context.path.replace(".wcdf",".css").replace(/\//g,":") + "/content";
$("head").append("<link rel='stylesheet' href='" + cssUrl + "'>"); 
```

### CDF Navigation Result

Old one:

```
/pentaho/api/repos/pentaho-cdf/JSONSolution?mode=contentList&solution=project&path=dashboards
```

New one: 

```
/pentaho/plugin/pentaho-cdf/api/getJSONSolution?mode=contentList&path=/public/project/dashboards
```

How did I figure this out? Take a look at `pentaho-solutions/system/pentaho-cdf/js/components/navigation.js` and search for `contentList`. I compared the file from v4.8 with the one in v5.2. The `solution` parameter was gone and the start of the URL was different: Now `wd.cdf.endpoints.getJSONSolution()` is used. So I opened a dashboard in a web browser and ran this function, which returned the required path. It's always good to have access to the source code ;)

You can also make use of this:

```
var path = wd.cdf.endpoints.getJSONSolution() + "?mode=contentList" + "&path=/public/project/" + path;
```

### Saiku Files

Old one:

```
/pentaho/content/saiku/pentaho/repository?type=saiku&path=project/analysis
```

New one:

```
/pentaho/plugin/saiku/api/pentaho/repository?type=saiku&path=/public/project/analysis
```

### Saiku View

```
/pentaho/content/saiku-ui/index.html?biplugin5=true&dimension_prefetch=false#query/open/<path-to-file>
```

e.g.:

```
/pentaho/content/saiku-ui/index.html?biplugin5=true&dimension_prefetch=false#query/open/%3Apublic%3Aproject%3Aanalysis%3A%3Atest.saiku
```

If you create these URLs dynamically in JavaScript, you can make use of `encodeURIComponent()` to encode the repository path portion.


### CDA Query

New one:

```
pentaho/plugin/cda/api/doQuery?path=/public/project/dashboards/dashboard.cda&dataAccessId=query1
```


### Refreshing Saiku Cash

Old one:

```
/pentaho/content/saiku/existinguser/discover/refresh
```

New one:

```
/pentaho/plugin/saiku/api/existinguser/discover/refresh
```

### Clear Mondrian Cache

Old one:

```
/pentaho/ViewAction?solution=admin&path=&action=clear_mondrian_schema_cache.xaction
```

New one:

```
/pentaho/api/system/refresh/mondrianSchemaCache
```

### Rendering a Dashboard

Old way:

```
/pentaho/content/pentaho-cdf-dd/Render?solution=/public/project&path=common&file=Saiku.wcdf&link=
```

New way:

```
/pentaho/api/repos/:public:project:common:Saiku.wcdf/generatedContent
```

### Referencing JavaScript, CSS, etc files from your solutions

This is an easy to use URL scheme: 

```
/pentaho/plugin/pentaho-cdf-dd/api/resources/<path-to-your-file>
```

e.g.:

```
/pentaho/plugin/pentaho-cdf-dd/api/resources/public/project/dashboards/myDashboard.css
```

### Referencing files from CDF

```
/pentaho/api/repos/pentaho-cdf/js/cdf-bootstrap-script-includes.js
```

## Using some exotic file types?

The **Pentaho BI Server** is only configured to accept specific file types out-of-the box. But it is easy enough to extend this list:

In case you are using **JSON** files for your dashboard, you should know that the new JCR repo does not support this file type. When you upload these files via the web interface you get an error message indicating this. Also the import/export utility will not import them.

Latino Joel provided the solution to this: You have to change `pentaho-solutions/system/ImportHandlerMimeTypeDefinitions.xml` (add the JSON file type) and restart the biserver.

Just add following snippet e.g.:

```
      <MimeTypeDefinition mimeType="application/json" hidden="false">
        <extension>json</extension>
        <extension>saiku</extension>
      </MimeTypeDefinition>
      <MimeTypeDefinition mimeType="application/vnd.ms-fontobject" hidden="true">
        <extension>eot</extension>
      </MimeTypeDefinition>
      <MimeTypeDefinition mimeType="application/x-font-ttf" hidden="true">
        <extension>ttf</extension>
      </MimeTypeDefinition>
      <MimeTypeDefinition mimeType="application/font-woff" hidden="true">
        <extension>woff</extension>
      </MimeTypeDefinition>
      <MimeTypeDefinition mimeType="application/font-sfnt" hidden="true">
        <extension>otf</extension>
      </MimeTypeDefinition> 
```

By they way: If you change your mind later on about the visibility of the file type, you will have to reimport these files or change the visibility via the PUC! It's not updated dynamically! The visibility/hidden attribute sticks directly with the file!

# Wondering why you do not see the changes applied after uploading new files

If you change the CDE dashboard style template, make sure you reasign it via **CDE Settings**! 

Hm, this doesn't seem to be the case all the time ...
maybe it was just chached then.


    

