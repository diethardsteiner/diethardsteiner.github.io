---
layout: post
title:  "The ultimate guide to using CCC and CGG with PRD"
summary: This article explains how you can use CCC charts with Pentaho Reporting solutions.
date:   2015-02-10
categories: PRD
tags: CCC, CDE, CGG, PRD
published: true
---

While other report designers come with fancy new chart engines, **Pentaho Report Designer** still uses **JFreeCharts**. However, there is an alternative to using the JFreeChart library, and that is making use of the bridge to **CCC** via **CGG**.

If your only task is to design a report, then this is certainly not the most straight forward way. However, if your primary task is to design a dashboard and the secondary task is to offer a pixel perfect report for it, then it all comes together nicely.

Ideally **Pentaho Report Designer** needs native support for CCC Charts (see [this JIRA ticket](http://jira.pentaho.com/browse/PRD-5366)). Thomas Morgner, lead developer on **Pentaho Reporting**, started an initiative into this direction (see [his blog post](https://www.on-reporting.com/blog/libcgg-how-to-render-ccc-charts-without-a-server/) on this topic), but unfortunatly this never made it into the main branch. If you are only designing a report, having native support in PRD will make the whole workflow a lot easier. Furthermore it should then offer the option as well to export the charts separately to the server in case you want to use them in a dashboard later on. What would make sense here really would be to treat the charts as well as independent modules that can be used by various tools (CDE, PRD, etc).

Or alternatively PRD should at least offer a simple GUI to add CGG/CCC charts from the server. A simple dialog to reference the file and the parameters would do (a bit similar to how you can add Kettle data sources). I submitted [this JIRA case](http://jira.pentaho.com/browse/PRD-5365).

# What is CGG

The Community Dashboard Editor (**CDE**) allows you to build dashboards in an easy fashion, which are based on the Community Dashboard Framework (**CDF**). **CDE** enables you to create charts based on the Community Charts Components (**CCC**), which at its core is based on the **Protovis** charting library (with several extensions).
The Community Graphics Generator (**CGG**) was added later on to allow users to use the **CCC** chart output in other output formats like Pentaho Reporting (**PRD**). In a nutshell, **CGG** is a service which generates a static image (PNG, SVG) of **CCC** charts.

# How to generate CGG files

The **CCC** Charts are not really a modular concept as such on the biserver. You create them when designing you dashboard via **CDE** and the charts are part of your dashboard. In **CDE** you can press the keyboard shortcut **SHIFT+G** and a dedicated **CGG JavaScript** file for each of the **CCC** charts will be created. When pressing the keyboard shortcut you will also see a dialog, which displays the URLs to access each of these CGG **JavaScript** files. 

This URL is made up of the following parts:

```
http://<biserver-domain-and-port>/pentaho/plugin/cgg/api/services/draw?script=<path-to-solution-folder>/<dashboard-name>_<ccc-chart-name>.js&outputType=<output-type>&param<parametername=value>
```

Possible output formats are `png` and `svg`. Ideally you want to use `svg`. If your charts require any parameter values, make sure you prefix the parameter name in the URL with `param`. So e.g. if you defined the parameter `startdate` in your dashboard, in the URL list it as `&paramstartdate=2015-01-01` in example.

# Workflow

If you want to have the best possible charting options in **Pentaho Report Designer**, use **CCC** with **CGG**. Full stop. 

Now the question is how can you use this charting engine that was originally developed for **CDE** inside **PRD**? Luckily the groundwork to do just this has been done and I'll walk you through the process:

## Create a dashboard with the CCC Charts

Just create your standard CDE dashboard with some CCC chart components. There are many tutorials on the internet on how to do this, so I will not cover it here.


## Dynamically scaling the charts

Sometimes you will require the charts to be in a different size then originally specified. If using the native PRD scaling function with the option to keep the aspect ratio is not enough (which might be the case when you only can scale the height or the width OR the resultion of the chart just doesn't allow scaling), then you can send the width and height as a parameter to the server like this e.g.:

```
&paramwidth=1000&paramheight=800
```

Awesome, isn't it?!

Keep in mind though that other features of the charts like labels, title etc should scale as well respecitively. Here is an example on how to scale the bar chart value label using an extension point (`label_font`). In my scenario only the height of the chart could change:

```javascript
function(){
    if(typeof cgg === 'undefined'){
        //console.log('-----');
        //console.log(this);
       return '10px Verdana';
    } else {
        //return (this.panel.height / 50) + 7 + 'px Verdana';
        return (this.chart.root.height / 50) + 7 +'px Verdana';
    }
}
```

The reason why I add 7 is because I do not want the font to be any small than 7px in any case.

Ideally you create a dedicated function for this and save it to dedicated file (e.g. `relativeCustomFontSize.js`):

```javascript
function relativeFontSize(chartObject, standardFontSize, minFontSize){
    if(typeof cgg === 'undefined'){
       return standardFontSize + 'px Verdana';
    } else {
        //return (chartObject.panel.height / 50) + 7 + 'px Verdana';
        var myFontSize = (chartObject.chart.root.height / 50) + minFontSize + 'px Verdana';
	return myFontSize;
    }
}
```

or you can just use it for both CGG and dashboards:

```javascript
function relativeFontSize(chartObject, minFontSize){
	var myFontSize = (chartObject.chart.root.height / 50) + minFontSize + 'px Verdana';
	return myFontSize;
} 
```

You might want to adjust the logic of this function depending on your requirements - this is not one function which works for all scenarios.

Upload this file into the same folder as your dashboard is located in and then add it as a JavaScript resource to your dashboard. For CGG you will have to load this JavaScript file seperately, as it will not include any dependencies automatically. I'll describe how to do this further down this article. 

In your chart settings you can now reference our new functions like this:

```javascript
function(){
	var myFont = relativeFontSize(this, 10);
    return myFont;
}
```

Extension points to consider for scaling (in brackets the standard properties, which these extension points will override):

- label_font (ValuesFont)
- orthoAxisLabel_font 
- orthoAxisTitleLabel_font (orthoAxisTitleFont)
- baseAxisLabel_font (baseAxisTitleFont)
- legendLabel_font (legendFont)

For some standard properties you can, however, define the a relative size using percentage instead of pixels:

- titleSize
- baseAxisTitleSize
- orthoAxisTitleSize
- etc

## Create CGG JavaScript files

This is very simple: Just press the keyboard shortcut `SHIFT + G` to generate the charts.

> Best Output-Format for PRD: Use SVG!!!

## CGG Restrictions

Sometimes you use external javascript libraries with CCC, in example `sprintf()` to format the axis tick labels.

Some of this functionality is already covered by built-in functions as shown below, so it is recommended to use the functions provided by the c-tools.

> **Duarte Cunha Leão**, lead developer of CCC, explains: About the "sprintf", if what you want is to format a number, I advise you to use CCC's own formatter. You can create a new formatter using: `var formatter = cdo.numberFormat("0.##"); fomatter(123.456) === "123.46"`. Or you can use the chart's default number formatter: `var fomatter = this.chart.format.number();`. [..] The "cdo" namespace should exist - that's either a bug or you're using a version that didn't have it, yet. [..] Anyway, try using `pvc.data` instead - they're synonyms. You might wanna know that we are baking this numberFormat (and accompanying stuff, like the FormatProvider, dateFormat, etc) into CDF (see [this jira case](http://jira.pentaho.com/browse/CDF-458)). We have a [separate issue](http://jira.pentaho.com/browse/CDF-355) for handling dates. Included is direct support for choosing number/date, ... masks and styles according to the current culture/language.

Also you can check if the chart is rendered via CGG or as a normal dashboard by using this:

```javascript
function(){
    if (typeof cgg != 'undefined'){ 
       
    } 
} 
```

So following the above example we will load the `relativeCustomFontSize.js` file, which we uploaded to the biserver to the same folder as the dashboard. Specify this in the **PreExecution** function of the chart component:

```javascript
function(){
    if(typeof cgg != 'undefined'){
        load('relativeCustomFontSize.js');
    } 
}
```

Theoretically you can add this code snippet to any of these functions: `postFetch` or `postExecution` or `preExecution`.

> **Pedro Vale** explains: If you want to load an external js, you should call `load('<relative path to js starting from your dashboard folder>)`. So if your dashboard is located at `/home/admin/myDashboard` and you want to load sprintf.js that is located at `/home/admin`, you'd do `load('../sprintf.js')`.

## Enable authentication via URL parameters

Before we can reference the CCC/CGG charts, we have to enable authentication via URL parameters in biserver v5.

> **Note**: Currently, even if you call the CGG API from within the biserver, you still require authentication. So imagine you store your report in the biserver repository and then run it - even in this case, authentication is required by the CGG API.

The default installation of biserver v5 does not accept userid and password as parameters of the URL: see [this JIRA case](http://jira.pentaho.com/browse/BISERVER-10708). The solution is to change the 
`pentaho-solutions/system/applicationContext-spring-security.xml` file. Replace the api filter chain in line 22 with this:

```
/api/repos/.wcdf/=securityContextHolderAwareRequestFilterForWS,httpSessionPentahoSessionContextIntegrationFilter,httpSessionContextIntegrationFilter,requestParameterProcessingFilter,basicProcessingFilter,anonymousProcessingFilter,exceptionTranslationFilterForWS,filterInvocationInterceptorForWS
/api/*=securityContextHolderAwareRequestFilterForWS,httpSessionPentahoSessionContextIntegrationFilter,httpSessionContextIntegrationFilter,basicProcessingFilter,anonymousProcessingFilter,exceptionTranslationFilterForWS,filterInvocationInterceptorForWS
```

> **Pedro Vale** explains: "The first line is specifying the filter chain for all calls to CDE dashboards (notice the wcdf in the pattern).
The second line is the original `/api/**` line that ships with 5.0.4. This means that calls to CDE dashboards will hit the first pattern (and allow url authentication). Any other call to `/api/repos` (the reporting ones, for instance) will hit the original filter chain."

It works for the dashboard then:

```
http://localhost:8080/pentaho/api/repos/%3Apublic%3ADashboards%3AQuarterly-Report%3AQuarterly-Report.wcdf/generatedContent?userid=admin&password=password
```

To get this working for CGG we have to alter the plugin filter: Just add `requestParameterProcessingFilter,` to the listed filters. So this should be similar to this one then:

```
/plugin/**=securityContextHolderAwareRequestFilterForWS,httpSessionPentahoSessionContextIntegrationFilter,httpSessionContextIntegrationFilter,requestParameterProcessingFilter,basicProcessingFilter,anonymousProcessingFilter,exceptionTranslationFilterForWS,filterInvocationInterceptorForWS
```

Now requesting the chart with userid and password works:

```
http://localhost:8080/pentaho/plugin/cgg/api/services/draw?script=/public/your/path/your-chart.js&outputType=svg&userid=admin&password=password
```

## Reference the charts in PRD

In the simplest case just use an **image** element to reference the chart with the URL that we just tested before. In my case, PRD took a fair amount of time to source this chart.

In most cases, however, you might want to use the **image-field** element to dynamically create a more complex URL. Use one or more formulas (**Data** tab > formula) to create the URL dynamically and then reference the final formula result in the **image-field** Attributes > Value.

Example:

As I was referencing several CCC/CGG charts, I create a few "global" formulas:

```
url_host ="http://localhost:8080"
url_params ="&outputType=png&paramparam_year="&[PARAM_YEAR]&"&paramparam_quarter="&[PARAM_QUARTER]&"&paramparam_client="&URLENCODE([PARAM_CLIENT])
url_chart_1y=[url_host]&"/pentaho/plugin/cgg/api/services/draw?script=/public/your/paht/chart1.js"&[url_params]
url_chart_2 =...
``` 

## Why does my PRD CCC/CGG chart look so small on the biserver?

You might want to consider the strategy outlined below to make your charts scale gracefully:

Use the following rule and all your charts will scale gracefully:

- use **SVG**
- set Style > Object > **Keep-aspect-ratio** to true
- set Style > Object > **Scale** to true

> **Thomas Morgner** recommends:  Use SVG as CGG output type! Treat sizes 1pt = 1px when using SVG, and you will be ok. Thats it.

By following this advice you also do not have to adjust the device output resultion, which can be configured on a per report basis in **File > Configuration** (e.g. in `output-pageable-pdf` and `output-table-html` the `DeviceResolution`).

If you still have problems with the picture scaling in the HTML output:

> **Thomas Morgner** also explains: Did you by any chance set the DPI to a value other than 96 DPI? If you did, please reverse that change, then try again. Browsers use 96 to 100 dpi as convention to convert between points (absolute size measure) and pixels (relative size measure). (Unix/X11 used to use 100DPI, Windows and Mac use 96 DPI, and I'm pretty sure that all cross-platform browsers use 96 dpi to bow to the market share of the majority operating systems.) Scale and keep-aspect ratio [settings] only matters during layouting, once layouted the reporting engine exports the images to match the
computed layout - this minimizes cross-browser problems. So if your PDF works well, your layout works and you don't have to worry about scale or keep-aspect-ratio at all.
