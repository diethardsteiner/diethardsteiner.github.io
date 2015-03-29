---
layout: post
title:  "The ultimate guide to Pentaho CCC Context Charts (aka Viewfinder or Sub-Charts)"
summary: In this article we will take a look at how to create context charts with Pentaho CCC.
date:   2015-03-23
categories: CCC
tags: CCC, CDE
published: true
---

In this article we will take a look at how to create context charts with **Pentaho CCC**:

> **Definition**: A **context chart** is an accompanying smaller chart to the main chart, which allows you to narrow down the selection of data (usually by pulling sliders). 

[A live demo is available here](http://bl.ocks.org/diethardsteiner/9e8af30e6c67469b3f00).

There are at least two scenarios, where context charts come in handy:

- The main chart is extremely busy and a context chart would help the end user to reveal some of the finer details. In this case all the data is already loaded and with any zoom action the data can be read from cache. On load both the context and main chart would display the same data and only then when the end user makes a selection via the context chart, the main chart would show the finer details.

- There is just too much data: Imagine you have tracking data by seconds and you want to show the end user the performance throughout the day, but allow them to narrow down the selection to interesting periods. Loading all the data for one day would just overload the browser. So you could just display the data let's say on a half hour granularity for the context chart and set a default selection window. Then the main chart would only have to display the per second details of the selection window. And with each change of the selection window, the main chart would fire off a new query to the the database to retrieve the relevant per second data.

**CCC** doesn't have a built-in support to automatically add a context/sub chart, but you can just create two charts and make one behave like a context chart. 

> **Important**: Make sure that the main chart is executed before the context chart by setting the **priority** to a lower figure, otherwise the context chart might not be shown most of the time: If you enable CCC debugging, you will see an error message like this one: `Error: null is not an object (evaluating 'comp.chart.options')`.

**Context charts** can be used with time series line and bar charts. **CCC** provides specific features for **context charts**. Currently **CDE** doesn't expose some of these features. However, you can just set them in the **preExecution** function. For example:

```javascript
function() {
    this.chartDefinition.selectionMode = "focuswindow";
    this.chartDefinition.focusWindowChanged = function() {
        var fwb = this.chart.focusWindow.base;
        // bypass CDF lifecycle:
        var comp = Dashboards.getComponentByName("render_comp_chart_viewers");
        
        comp.chart.options.baseAxisFixedMin  = fwb.begin;
        comp.chart.options.baseAxisFixedMax = fwb.end;
        comp.chart.render(
		/*bypassAnimation:*/
		true, 
		/*recreate:*/
		true, 
		/*reloadData:*/
		false
	);
    };
} 
```

The `timeSeries` property (Note the captial **S**, otherwise it will be ignored) of the **charts** has to be set to `true`. `baseAxisFixedMin` and `baseAxisFixedMax` are only supported by **timeseries/continuous scales charts**.

An important point to note here is that by using `comp.chart.render()` we bypass the standard **CDF** lifecycle (preExection -> preFetch -> postFetch -> execution -> postExection). You will use this approach if all your data is already cached. Otherwise you will want to set values for the parameters that the main chart is listening to (so that a new query is initiated), e.g.:

```javascript
function() {
    this.chartDefinition.selectionMode = "focuswindow";
    this.chartDefinition.focusWindowChanged = function() {
        var fwb = this.chart.focusWindow.base;
	Dashboards.setParameterValue('startDate', fwb.begin);
	Dashboards.fireChange('endDate', fwb.end);
	);
    };
} 
```

You can also set the beginning and end of the focus window programmatically as well as disable resizing and moving the focus window:

```javascript
focusWindowBaseBegin: new Date('2011-07-01'),
focusWindowBaseEnd:   new Date('2011-08-01'),
focusWindowBaseResizable: false,
focusWindowBaseMovable:   false,
```

Next we will take a look at the `focusWindowBaseConstraint`, which allows restricting the focus behaviour to certain intervals. The **object** passed to this function has following properties:

- **target**: 'begin' or 'end', tells you which focus handle was pulled
- **value**: postition of the focus handle in milliseconds
- **min**: begin handle value
- **max**: end handle value
- **minView**: min possible beginn handle value to stay in view
- **maxView**: max possible beginn handle value to stay in view
- **type**: 
	- new: on first call and whenever the user clicks outside the focusWindow  (target = 'begin') 
	- resize-begin: pulling left handle (target = 'begin')
	- resize-end: pulling right handle (target = 'end')
	- move: moving the window (target = 'begin')
- **length**: The window length (can be changed when type = 'new')
- **length0**: The previous window length

It's best to test the focus restriction behaviour just in this simple form: Let's say we just want to allow selection of full days:

```javascript
var myDate = new Date('2015-01-01T12:12:00');
var tim = pvc.time;
var interval = tim.weekday;
var finalDate = interval.closestOrSelf(tim.withoutTime(myDate));
console.log(finalDate);
> Thu Jan 01 2015 00:00:00 GMT+0000 (GMT)
```

If we want to restrict the focus to each Monday:

```javascript
var myDate = new Date('2015-01-01T12:12:00');
var tim = pvc.time;
var interval = tim.weekday;
var swd = 1; // Start of Week Day is Monday
var finalDate = interval.closestOrSelf(tim.withoutTime(myDate), swd);
console.log(finalDate);
> Mon Dec 29 2014 00:00:00 GMT+0000 (GMT)
```

Once you have the base logic working, it's time to integrate it into the main cod:

```javascript
// Round dates to Day.
focusWindowBaseConstraint: function(oper) {

    // Duarte's bug workaround
    // Only for when focusWindowBaseBegin/End are not specified.
    if(oper.type === 'new' && oper.length0 === oper.length) {
        var len    = (oper.max - oper.min) / 4;
        var middle = ((+oper.max) + (+oper.min))/2;
        oper.value  = new Date(middle - len / 2);
        oper.length = len;
    }

    // Set time namespace
    var tim = pvc.time;
    var interval = tim.weekday;

    // testing
    // var myDate = new Date('2015-01-01T12:00:00');
    // var finalDate = interval.closestOrSelf(tim.withoutTime(myDate));
    // console.log(finalDate);

    // Set min length
    // pvc.time.intervals.* returns amount of milliseconds for interval
    var minLen = tim.intervals.d;
    var sign = oper.target === 'end' ? -1 : +1;
    var t0 = +oper.value;

    // Round to closest day
    var t = interval.closestOrSelf(tim.withoutTime(oper.value));

    // Don't let value go below the minimum value in the view
    oper.min = interval.previousOrSelf(tim.withoutTime(oper.minView));

    // Don't let value go above the maximum value in the view
    oper.max = interval.previousOrSelf(tim.withoutTime(oper.maxView));

    // Ensure minimum length
    if(oper.type === 'new') {
        oper.value  = t;
        oper.length = Math.max(oper.length, minLen);
        return;
    }

    var l = +oper.length;
    var o = t0 + sign * l;
    l = sign * (o - t);
    if(l < minLen) {
        t = o - sign * minLen;
    }

    oper.value = t;
}
```

<s>With `off_focusWindowBaseConstraint` you can specify a function to handle the logic when a user makes a new selection outside the existing one.</s> Update: This doesn't actually exist: Duarte just prefixed the property to disable it.

Following **events** are available: `focusWindowChanged` and `selectionChangedAction`. The last event is triggered when you stop dragging/moving the focusWindow, and might be preferable in some situations to only refresh the focus chart in here, instead of in `focusWindowChanged`.

Additionally you have the following **Extension Points** at your disposal:

Property | Sample Value | Description
-----|------|-----
`focusWindowBg_fillStyle` | 'rgba(100, 0, 0, 0.2)' | Focused Window Bg (behind elements) [pv.Bar]
`focusWindow_fillStyle` | 'rgba(100, 0, 0, 0.2)' | Focused Window Fg (in front of elements) [pv.Bar]
`focusWindowBaseCurtain_fillStyle` | 'transparent' | Begin and End out of focus areas [pv.Bar]
`focusWindowBaseGripBegin_fillStyle` | 'red' | Start Grip/Handle color
`focusWindowBaseGripEnd_fillStyle` | 'green' | End Grip/Handle color

> **Note**: The same principles can be applied to a **bar chart** as well!

Finally a special thanks to **Duarte Cunha Leão**, lead architect of **CCC**, for providing a lot of this information.