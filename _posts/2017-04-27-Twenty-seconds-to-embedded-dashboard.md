---
layout: post
title: "20 Seconds to Embedding a CDE Dashboard and Fixing RequireJS Cache Problem"
summary: This article discusses how to embed a CDE dashboard in an external site
date: 2017-04-27
categories: CDE
tags: CDE
published: false
---  

While CDE dashboard can be displayed within the Pentaho User Console (the web interface), quite often the requirement is to embed the dashboard within an extrernal site. Luckily these days this is fairly easy to accomplish: We will have a look at how to achieve this using an extremely basic example. 

## C-Tools Config Changes

First off we have to change `pentaho-solutions/system/pentaho-cdf-dd/settings.xml` to allow cross-domain requests:

```
<allow-cross-domain-resources>true</allow-cross-domain-resources>
```

Add this just after the first `<settings>` tag.

Perform the same change for `pentaho-solutions/system/pentaho-cdf/settings.xml`.

## Create CDE Dashboard

Create your CDE dashboard as usual if you don't have one already.

Create a sample external html page which will allow us to reference the CDE dashboard.

## Configuring the MyCompany WebApp

We will create an extremely simple web app, which will allow us to embed the dashboard we created earlier on:

```
$ cd <pentaho-server-root>
$ mkdir -p tomcat/webapps/mycompany/scripts
```

Download requirejs (the minified version) from [here]() and copy it into the `scripts` folder.

Create an `index.html` file and save it inside the `mycompany` folder.

The most basic skeleton looks like this (read inline comments):

```html
<html>
  <head>
  <meta http-equiv="content-type" content="text/html" charset="utf-8">
    <title>Demo of embedded CDE content</title>
    <script src="scripts/require.js"></script>
    <script type="text/javascript" src="http://localhost:8080/pentaho/plugin/pentaho-cdf-dd/api/renderer/cde-embed.js"></script>
    
    <script type="text/javascript">
      require(
        // define the location of the dashboard
        ['dash!/public/embedded/demo.wcdf']
        // Demo is just a sample argument name, can be anything
        , function(Demo){
            // define div id where the dashboard should be rendered
            var demoDash = new Demo("myExternalDiv");
            demoDash.render();
          }
      )
    </script>
    
  </head>
  <body>
    <h1>Demo of embedded CDE content</h1>
    <!-- 
    Below is our placeholder - this is where we want to embed the CDE dashboard 
    Note that this div id gets referenced above in the 
    JavaScript `new Demo("myExternalDiv")`
    -->
    <div id="myExternalDiv"></div>
  </body>
</html>
```

## Adding JS resources

We follow the **RequireJS** approach outlined [here](http://redmine.webdetails.org/projects/cde/wiki/RequireJS).

Create you JavaScript file. Make sure it follows the **AMD module** convention. Simple example:

```javascript
define(function() {

  return {
    option1: 'Eternal sunshine',
    option2: 'Tropical thunderstorms'
  };
});
```

In your dashboard, refernce it as an external resource (as you usually would do, nothing changes here). The CDE **Property Name** that you define for the external resource will be the name of the variable accessible in the dashboard context. So in my case I called it `myOptions`:

![](/images/cde-embedded/cde-requirejs-1.png)

Once you render the dashboard, take a look at the resources in the dev tools of your web browser, and you should see the in the `generatedContents`

![](/images/cde-embedded/cde-requirejs-2.png)

Browser Debugging Tools: I am using Firefox here, but it will be fairly similar in other web browsers. You can get hold of info on your javascript file the following way:

- **Debugger > Sources**: `myOptions` shows up there:

  ![](/images/cde-embedded/cde-requirejs-3.png)

- **Network Manager**: Scroll down the list to find your file. Clicking on the name reveals more info.

  ![](/images/cde-embedded/cde-requirejs-4.png)

## Issue: Changes in files are not reflected client-side

This refers to [Jira Ticket CDE-848](http://jira.pentaho.com/browse/CDE-848): "In requireJS dashboard I have external js/css files (placed in repository, included as resources in CDE). Changes to those files in most cases are not respected until I clear the browser cache."

With standard JavaScript file adding a version number like discussed [here](http://stackoverflow.com/questions/32414/how-can-i-force-clients-to-refresh-javascript-files) is a fairly standard approach to circumvent the caching problem.

For RequireJS we can use this approach: [Prevent RequireJS from Caching Required Scripts](http://stackoverflow.com/questions/8315088/prevent-requirejs-from-caching-required-scripts), also take a look at [the Require Config Docu](http://requirejs.org/docs/api.html#config).

Ok, based on this we can change our external HTML file (where we embed the content) like so (only relevant section shown):

```html
    <script src="scripts/require.js"></script>
    <!-- prevent caching: start -->
    <script>
      require.config({
        urlArgs: "bust=" + (new Date()).getTime()
      });
    </script>
    <!-- prevent caching: end -->
    <script type="text/javascript" src="http://localhost:8080/pentaho/plugin/pentaho-cdf-dd/api/renderer/cde-embed.js"></script>
```

> **Note**: You will have to clear the cache once using `CTRL+R` after implementing this changes, but thereafter no cache-clearing should be necessary any more!


The approach shown above is ideal for developing, when you release your code you want to use a more static approach as we do not want to load all the dependencies each time in the client/web browser, e.g.:

```html
    <script src="scripts/require.js"></script>
    <!-- prevent caching: start -->
    <script>
      require.config({
        urlArgs: "bust=v2"
      });
    </script>
    <!-- prevent caching: end -->
    <script type="text/javascript" src="http://localhost:8080/pentaho/plugin/pentaho-cdf-dd/api/renderer/cde-embed.js"></script>
```

Naturally you could also have some release script set the version value for you.

If you wanted to add this code snipped directly in **CDE** ... [OPEN]