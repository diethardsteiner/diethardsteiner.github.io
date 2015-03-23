---
layout: post
title:  "Pentaho CDE: Global CCC Properties"
summary: This article explains how to create global properties for CCC Charts.
date:   2015-03-23
categories: CCC
tags: CCC, CDE
published: true
---


This article is a brief walkthrough on how to define global **CCC Chart Properties**. We start with some simple examples and work our way towards creating these global properties.

## Replacing CDE CCC Chart Properties (or: How to copy CCC JavaScript code directly into CDE)

Sometimes when you prototype your chart outside CDE in plain HTML you would like to just copy the JavaScript chart definition into CDE without having to fill out all the properties.

It is indeed possible to do this by overriding the `chartDefinition` object in the **PreExecution** function.

It is best that you familiarise yourself first with all the properties of the `chartDefintion` object using the **PreExecution** function:

```javascript
function(){
	console.log(this.chartDefinition);
}
```

You will also see that there are a few additional properties like `dataAcccessId` (to define the query), which are not available in standard CCC.

Our code in CCC JavaScript looks like this (partial extract only):

```javascript
...
new pvc.LineChart({
  canvas: "cccExample",
  width:   600,
  height:  200,
  animate: false,
  timeSeries: true,
  timeSeriesFormat: "%Y-%m-%d"
})
...
```

In this case in **CDE** we add a **CCC Line Chart** component (so later on we do not have to define the chart type in our code again). We set the standard properties like **Name**, **Datasource**, **HTMLObject**, **crosstabMode** and **seriesInRows**. Then we can add our CCC JavaScript in **CDE** to the **PreExecution** function like this:

```javascript
function(){
	// override the CDE chart definition
    this.chartDefinition = {
      width:   600,
      height:  200,
      animate: false,
      timeSeries: true,
      timeSeriesFormat: "%Y-%m-%d",
      dataAcccessId: 'qry_test'
    }; 

} 
```

> **Note**: CDE sets default values for quite some chart properties. This is why **we override the complete CDE chart definition**.

> Integrating **Extension Points** is a bit more work because they are stored as an **array** within **CDE** whereas in **CCC** it is defined as an **object**. We will take a look at this in the next section. **UPDATE**: You can define Extension Points on the same level as the standard properties (so very easily), but there is a small caveat to this (read more about it later on).

## Extending CDE Chart Properties

Instead of completely overriding the chart definition, we sometimes just want to extend it: If there are common properties, we want to override them, otherwise we want to keep the current value.

Integrating **Extension Points** is a bit more work because they are stored as an **array** within **CDE** whereas in **CCC** it is defined as an **object** (usually):

```javascript
function(){
	// get chart definition
	var cccOptions = this.chartDefinition;
	
	// standard ccc properties
	var myCccProperties = {
		titleFont: 'bold 16px Verdana, Geneva, sans-serif'
		, baseAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, legendFont: '14px Verdana, Geneva, sans-serif'
		, orthoAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, valuesFont: '10px Verdana, Geneva, sans-serif'	
	};
	
	// Add/override CCC options
	$.extend(cccOptions, myCccProperties);
	
	// extension points
	// are stored in CDE as array and not as an object like in CCC
	var extensionPoints = Dashboards.propertiesArrayToObject(cccOptions.extensionPoints);
	
	var myCccExtensionPoints = {
		orthoAxisTitleLabel_textStyle: 'white'
		, xAxisLabel_textStyle: 'white'
		, yAxisLabel_textStyle: 'white'
		, legendLabel_textStyle: 'white'
		, axisLabel_font: '12px Verdana, Geneva, sans-serif'
		, axisRule_strokeStyle: 'white'
		, axisOffset: 0
		, line_interpolate: 'monotone'
		, area_interpolate: 'monotone'	
	}
	
	// Add/override CCC options
	$.extend(extensionPoints myCccExtensionPoints);
	
	// Update the CDE extension points list with the new settings
	cccOptions.extensionPoints = Dashboards.objectToPropertiesArray(extensionPoints);
} 
```

However, an improvement was implemented some time ago, which allows you to define extension points as a property of the `chartDefinition` instead of using the CDE custom `chartDefinition.extensionPoints` property. Note that if there are any properties defined in CDE `chartDefinition.extensionPoints` (e.g. via the UI), these ones will have priority over the ones defined directly in `chartDefinition`:

```javascript
function(){
	// get chart definition
	var cccOptions = this.chartDefinition;

	// standard ccc properties
	var myCccProperties = {
		titleFont: 'bold 16px Verdana, Geneva, sans-serif'
		, baseAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, legendFont: '14px Verdana, Geneva, sans-serif'
		, orthoAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, valuesFont: '10px Verdana, Geneva, sans-serif'	
		// extension points
		, orthoAxisTitleLabel_textStyle: 'white'
		, xAxisLabel_textStyle: 'white'
		, yAxisLabel_textStyle: 'white'
		, legendLabel_textStyle: 'white'
		, axisLabel_font: '12px Verdana, Geneva, sans-serif'
		, axisRule_strokeStyle: 'white'
		, line_interpolate: 'monotone'
		, area_interpolate: 'monotone'	
	};
	
	// Add/override CCC options
	$.extend(cccOptions, myCccProperties);
}
```

## CDE: Global CCC Properties ##

When creating a dashboard with various charts, usually certain chart properties are the same across all the charts. You might also want to achieve a certain design across all your charts. In this case, it would be ideal to define these properties globally. 

Let's see how this can be done with **CCC** in **CDE**:

Create a JavaScript file with the basic settings used across all charts. Encapsulate this in a function. E.g.:

```javascript
function initGlobalCccProperties(chartDefinition){
	// get chart definition
	// var cccOptions = this.chartDefinition;
	var cccOptions = chartDefinition;

	// standard ccc properties
	var globalCccProperties = {
		titleFont: 'bold 16px Verdana, Geneva, sans-serif'
		, baseAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, legendFont: '14px Verdana, Geneva, sans-serif'
		, orthoAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, valuesFont: '10px Verdana, Geneva, sans-serif'	
		// extension points: A recent improvement allows you to
		// set them directly as a property of the chart definition
		// instead of using the custom CDE chartDefinition.extensionPoints
		// however if there are any properties defined in
		// chartDefinition.extensionPoints, they will take priority
		// over these ones.
		, orthoAxisTitleLabel_textStyle: 'white'
		, xAxisLabel_textStyle: 'white'
		, yAxisLabel_textStyle: 'white'
		, legendLabel_textStyle: 'white'
		, axisLabel_font: '12px Verdana, Geneva, sans-serif'
		, axisRule_strokeStyle: 'white'
		, axisOffset: 0
		, line_interpolate: 'monotone'
		, area_interpolate: 'monotone'	
	};

	// Add/override CCC options
	$.extend(cccOptions, globalCccProperties);	
} 
```

As noted previously, defining **extension points** as direct properties of `chartDefinition` will not override any extension points set in the CDE specific `chartDefinition.extensionPoints`. If you want to override these ones as well, then you can use this approach:

```javascript
function initGlobalCccProperties(chartDefinition){
	// get chart definition
	// var cccOptions = this.chartDefinition;
	var cccOptions = chartDefinition;
	
	// standard ccc properties
	var globalCccProperties = {
		titleFont: 'bold 16px Verdana, Geneva, sans-serif'
		, baseAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, legendFont: '14px Verdana, Geneva, sans-serif'
		, orthoAxisTitleFont: 'bold 14px Verdana, Geneva, sans-serif'
		, valuesFont: '10px Verdana, Geneva, sans-serif'	
	};
	
	// Add/override CCC options
	$.extend(cccOptions, globalCccProperties);
	
	// extension points
	// are stored in CDE as array and not as an object like in CCC
	var extensionPoints = Dashboards.propertiesArrayToObject(cccOptions.extensionPoints);
	
	var globalCccExtensionPoints = {
		orthoAxisTitleLabel_textStyle: 'white'
		, xAxisLabel_textStyle: 'white'
		, yAxisLabel_textStyle: 'white'
		, legendLabel_textStyle: 'white'
		, axisLabel_font: '12px Verdana, Geneva, sans-serif'
		, axisRule_strokeStyle: 'white'
		, axisOffset: 0
		, line_interpolate: 'monotone'
		, area_interpolate: 'monotone'	
	}
	
	// Add/override CCC options
	$.extend(extensionPoints, globalCccExtensionPoints);
	
	// Update the CDE extension points list with the new settings
	cccOptions.extensionPoints = Dashboards.objectToPropertiesArray(extensionPoints);	
}
```

In your **CDE** dashboard include this JavaScript file as an external resource.

Then we can add our function ( e.g. `initGlobalCccProperties`) in the **CDE CCC Chart Component** to the **PreExecution** function like this:

```javascript
function(){
    initGlobalCccProperties(this.chartDefinition); 
}
```

Now this chart component will make use of the global settings.

**Duarte Cunha Leão**, lead architect of **CCC**, provides following advice:

> Other caveats also exist in cases where the intended global style extension behaviour, or that of subsequent local styles, would be to "add" instead of "replace".
Extension points like `mark_event`, `mark_add`, `mark_call`, action handlers, like `clickAction`, `selectionChangedAction`, etc, are usually meant to have an "additive" behaviour. For extension points, there's a syntax that simultaneously supports extension points with more than one argument and calling them more than once:

```javascript
"mark_event": [
    ["point",     function() { }] // 2 arguments of 1st ep call
    ["unpoint", function() { }] // 2 arguments of 2st ep call
]
```

> So, you could, in principle, have an `chartDefinition` extend method that would be smart enough to combine these additive-like extension properties properly. For the action handlers, the only way is for you to wrap a function that calls the previous value, and the new value. However, because of multiple executions, note that what you set in the chart definition in one execution, is still there in the following execution. So you have to have additional care to not combine with previous values, or in branching situations where you set options in one case but not in the following - the previous execution's option values remain there - you might need to reset them.

Some more info about this can be found [here](http://forums.pentaho.com/showthread.php?150121-CCCv2-clickAction-How-to-handling-the-right-click-action&p=354722#post354722).

The basic idea is that an extension point can listen to more than one event (e.g. `mouseover` and `contextmenu`). This is achieved by using a special syntax for the extension point, basically defining an **array** with functions: 

```javascript
bar_event: [
    ['contextmenu', function(s) { alert('context ' + s.vars.category.value); }],
    ['mouseover',   function(s) { alert('over '    + s.vars.category.value); }]
]
```
