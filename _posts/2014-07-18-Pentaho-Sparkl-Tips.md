---
layout: post
title:  "Pentaho Sparkl Tips"
summary: This article is a list of various important points when working with Sparkl
date:   2014-08-10
categories: Review
tags: Pentaho
published: true
---

# Where to store you custom Sparkl files

## Where to put your CSS, Image and JavaScript files

If you are creating any custom **JavaScript**,**CSS** and/or **Image** files for your **Sparkl** project, you can store them under `<sparkl-plugin-root>/static/custom`. Within this directory you find follwing subfolders: `css`, `js` and `img`.

From your dashboard(s), you can reference the file by using the **External Resource File**. Specify following path:

```
/pentaho/api/repos/<sparkl-plugin-name>/static/custom/
```

In example you create your own css file called `my.css`, which you can store that in:

```
<sparkl-plugin-root>/static/custom/css/my.css
```

... and in your **CDE** dashboard you can reference this file as an **External Resource File** like this:

```
/pentaho/api/repos/<sparkl-plugin-name>/static/custom/css/my.css
```

## Template

### Creating your own template

The main template `cpk.html` is located in `<sparkl-plugin-root>/resources/styles`. When you open this file, you will see placeholders like @HEADER@, @CONTENT@ and @FOOTER@. These placeholds will be replaced by the actual Sparkl content when your plugin is build, so do not remove them. 

You can replace the `cpk.html` with your own template. To start with, just take a copy of this file so that you have it as a reference in case you need it. Open the new file and remove (in example) everything within the `<div class="cpkWrapper">` section apart from the content and footer sections and add your own bits and pieces.

Let's add a simple **Bootstrap Navbar**:

        <nav class="navbar navbar-default" role="navigation">
          <div class="container-fluid">
            <!-- Brand and toggle get grouped for better mobile display -->
            <div class="navbar-header">
              <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </button>
              <a class="navbar-brand" href="#">Brand</a>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
              <ul class="nav navbar-nav">
                <li class="active"><a href="main">Main</a></li>
                <li><a href="config">Admin</a></li>
              </ul>
            </div>
          </div>
        </nav>

> **Referencing a dashboard**: In your navigation your want to link to the dashboards that you created. This is actually very easy to accomplish: Instead of specifying a path, you only have to specify the name (whithout file extension) of the dashboard. In the above example I reference the dashboards *main* and *config*.


> **Note**: If you are using **Bootstrap** for your dashboard as well, there is no need to reference dependencies here as well because they will be automatically included.


If your template has any external dependencies, they can be referenced by using a path similar to the one shown below:

    <link rel="icon" type="image/x-icon" href="../../../api/repos/<sparkl-plugin-name>/static/custom/img/test.ico">
    <link rel="stylesheet" type="text/css" href="../../../api/repos/<sparkl-plugin-name>/static/custom/css/test.css" />

    
Also keep in mind that you can reference any of the **JavaScript** files and libraries **Pentaho CDF** makes use of:

    <script language="javascript" type="text/javascript" src="../../../../../../content/pentaho-cdf/js/daterangepicker/daterangepicker.jQuery.js"></script>
    
Or even reference **JavaScript** files etc. from **Pentaho CDE**:

    <script language="javascript" type="text/javascript" src="../../../../../../plugin/pentaho-cdf-dd/api/resources/get?resource=/js/cdf-dd-base.js"></script>




### Applying your template

So now that we have got our template ready, how do we apply it? **Templates** are applied on a per **dashboard** basis. 

First make sure your refresh the plugin files by clicking the **Refresh** icon on the **Sparkl Editor** page.

From the **Sparkl Editor** page open up one of your dashboards to **edit** and click on **Settings** once the new page opens:

![CDE Main Menu](/images/cde_main_menu_settings.png)

At the bottom of the **Settings** pop-up window you find a **Style** picker:

![Dashboard Settings](/images/cde_settings_style.png)

Choose the template we just created and click **Save**.


## Where to store your data

In case your plugin has to persist some config data, the best way to do this currently is to use a folder outside the plugin folder. In example, just create a folder under `<biserver>/pentaho-solutions/system`.

# Sparkl and Kettle

## Referencing steps

### How to reference a step from a transformation

First read [the existing Sparkl docu](https://github.com/webdetails/cpk#specifying-from-where-to-get-results).


> **Note**: In some scenarios it might be necessary to have more than one output step. You can define several steps to return data to the **Sparkl** endpoint. Prefix any **job entry** or **step** name with *upper-case* **OUTPUT**, in example **OUTPUT**step1, **OUTPUT**step2, but NOT outputStep3.

You can preview the output of your endpoint via your favourite web browser:

    http://<server-ip>:<server-port>/pentaho/plugin/<plugin-name>/api/<endpoint-name>?stepName=<step-name>&bypassCache=true

Example result set:

```javascript
{"resultset":[["SampleData"],["test"]],"queryInfo":{"totalRows":2},"metadata":[{"colIndex":0,"colType":"String","colName":"connectionName"}]}
```

### How to reference a step from a transformation which is called by the Metadata Injector step

Sometimes things get a bit more complex and you want to use the **Metadata Injector** step to be extremly dynamic. In this case, just feed the output of the called transfromation back to this step (this can be configured within the **Metadata Injector** step settings). From here the approach is the as mentioned above.

### How to reference a step from a job

> **Note**: A job endpoint only accepts one output (in contrast to the transformation endpoint). See [this Jira case](http://jira.pentaho.com/browse/SPARKL-66) for more details. Guilherme Raimundo explains: " If you want something like that \[more than one output\] you will need to implement it in the logic of your job. One way of doing it is to pass in a query string parameter (e.g. "paramMyParam") and have your job logic set different results in the job according to the parameter."

To summarize the concept: In a job you can use as many transformations as necessary. For the transformation which returns the data:

1. Use a **Copy rows to results** step in the end
2. Prefix the job entry name with **OUTPUT**. **Note**: You do not have to prefix the transformation or job name itself, just the job entry name. 

To test the output of your job:

```
http://<server-ip>:<server-port>/pentaho/plugin/<plugin-name>/api/<endpoint-name>?bypassCache=true
```

The default output is a summary:

```javascript
{"result":true,"exitStatus":0,"nrRows":2,"nrErrors":0}
```

To see the actual data and metadata, request JSON output:

```
http://<server-ip>:<server-port>/pentaho/plugin/<plugin-name>/api/<endpoint-name>?bypassCache=true&kettleOutput=Json
```

Sample output:

```javascript
{"resultset":[[422,"Fred","Oyster"],[334,"Franz","Ferdinand"]],"queryInfo":{"totalRows":2},"metadata":[{"colIndex":0,"colType":"Integer","colName":"id"},{"colIndex":1,"colType":"String","colName":"firstname"},{"colIndex":2,"colType":"String","colName":"lastname"}]}
```

Currently (2014-08-01), you have to explicitly define the CDE Kettle endpoint output format as Json as well for job endpoints - something you don't have to do for transformation endpoints. I asked that this behaviour is standardized (see [this Jira case](http://jira.pentaho.com/browse/SPARKL-66)).

# Upgrading the CPK/Sparkl core for your plugin

Once you create a **Sparkl plugin**, the project folder structure and required base files get created for you automatically. Required libraries are stored in the `lib` folder.

Sometimes it might be necessary to upgrade to a newer library version (in example when some bugs get fixed or new features become available). To to this, run in the root folder of **your plugin** following command:

```bash
ant resolve
```

Once the ant script finished, you should see in the log `BUILD SUCCESSFUL`.

Next restart the server so that you can start using this new library.

Update 2014-08-10: The latest build of Sparkl provides this functionality via the Web UI.

I am planning to add more info to this article over time, so watch out this space!


