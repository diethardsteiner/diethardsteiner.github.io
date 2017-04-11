---
layout: post
title:  "MDX DRILLDOWNMEMBER and CDE Dashboard Tables"
summary: We discuss an elegent use case for DRILLDOWNMEMBER within a CDE table
date: 2017-04-09
categories: MDX
tags: MDX
published: false
---

I must admit I am lacking the time these days to always write in-depth, example based blog posts. There are quite a lot of ideas that pop up over time, quite some of which get discarded as I never get around writing them down. I think it is probably time to change the concept a bit and not always provide the full solution but in some instances just explain the main concepts briefly: Here we go, this is the first article of this series.

## CDE Table Component Setup and MDX Drilldownmember

We are discussing how to create a **Pentaho CDE** dashboard here, so I expect you are familiar with the concepts of CDE. I also expect that you have a good understanding of **MDX**.

**MDX** is an extremely **powerful query language**. Over the years I got exposed to various MDX functions, some of which I only started to appreciate later on, when I was facing a particular challenge. One of such function was `DRILLDOWNMEMBER`. I read about this function a long time ago but only recently used it in a project. 

A few days ago I was asked to put together a **CDE dashboard** for a client. The specs were really quite straight forward: Display a high level summary and allow us to drill down. Now you'd be inclined to think that this is quite a popular request and hence CDE has something in store to cover this scenario: Well, the **table component** has  an **Expand on click** feature, which displays an inline table, which is not as elegant for this purpose as a drill down in Analyer, Saiku or even JPivot. So here is how I tackled the challenge:

We will be using the **SteelWheelsSales** Cube, which ships with every **Pentaho Server**. For this example I am using the current version 7. Download it via [SourceForge](https://sourceforge.net/projects/pentaho/files/Business%20Intelligence%20Server/). We will provide drill down functionality for the **Product Dimension**. The product dimension has following level (top to bottom):

- Line
- Vendor
- Product

To familiarse yourself with the SteelWheelsSales Mondrian schema, you can play around with it using good old (shall I say very old and trusted) JPivot (via **File > New > JPivot View**) and you can also download the Mondrian Schema via **File > Manage Data Sources**.

In **JPivot** click the **MDX** button and run following query:

```sql
WITH
	MEMBER [Measures].[Member Full Path] AS
		[Product].CurrentMember.UniqueName
	MEMBER [Measures].[Member Ordinal] AS
		[Product].CurrentMember.Ordinal
SELECT
	NON EMPTY 
	  DESCENDANTS([Product].[Line].Members, 1, SELF_AND_BEFORE)
	ON ROWS
	, {
		[Measures].[Member Full Path]
		, [Measures].[Member Ordinal]
		, [Measures].[Sales]
	} ON COLUMNS
FROM [SteelWheelsSales]
WHERE [Time].[Years].[2004]
```

Click on the **Apply** button and JPivot will swiftly return the resultset. Partial screenshot below:

![](/images/mdx-drilldownmember-1.png)

Next create a **CDE dashboard** (**New > CDE Dashboard**). I will not go through all the details on how to create this, just mention a few main points. 

## Create the layout

In the **Layout** add a super simple layout: 3 rows with one column each. Set the columns to 12 (Extra Small Devices) and name them `html_title`, `html_date_picker`, `html_main_report`:

![](/images/mdx-drilldownmember-4.png)

## Creating the year picker

The end user should be able to choose the year. For this purpose we can simply use **Select component** and **parameter** called `param_year`.

To drive the **Select component** we use the following MDX query using a **mdx over mondrianJndi** data source (from the **Data Source Panel** called `qry_list_years`:

```sql
SELECT
  [Time].[Years].Members ON ROWS
  , {} ON COLUMNS
 FROM [SteelWheelsSales] 
```

![](/images/mdx-drilldownmember-2.png)

> **Note**: Once you save the dashboard, in the same folder you'll find a CDA file. Double click on it and you will be able to preview the data for each query you define in the dashboard. Ultra handy! Just make sure you refresh the tab once you define a new query:

![](/images/mdx-drilldownmember-5.png)

Next create a **Simple Parameter** (Generic > Simple Parameter) in the **Components Panel** and then add a **Select Component** (Selects > Select Component), call it `comp_year_picker`, assign the parameter `param_year`, the data source `qry_list_years` and the html object `html_date_picker`:

![](/images/mdx-drilldownmember-3.png)


For the drill down functionality, we create a custom parameter called `param_line` and set its default value to:

```
"DESCENDANTS([Product].[Line].Members, 1, SELF_AND_BEFORE)"
```

Just be warned, I start off by doing things in the wrong way and then gradually show you how to get it right, to perfectly illustrate best practices. There are always more than one way to do things, but not necessarily all approach are good ones.

This will display the overall summary (the `All` level) as will as the first level and its members.

Next define the MDX query called `qry_main_report` and register both parameters (`param_year` and `param_line`).

![](/images/mdx-drilldownmember-6.png)

> **Important**: Make sure that you do not define any default values for `param_line` when you pass down the parameters to a component or MDX query! So in example, when you define the parameter for the MDX query in CDE, leave the `Value` field empty.

The **MDX query** should look something like this:

```sql
WITH
	MEMBER [Measures].[Member Full Path] AS
		[Product].CurrentMember.UniqueName
	MEMBER [Measures].[Member Ordinal] AS
		[Product].CurrentMember.Ordinal
SELECT
	NON EMPTY 
	  ${param_line}
	ON ROWS
	, {
		[Measures].[Member Full Path]
		, [Measures].[Member Ordinal]
		, [Measures].[Sales]
	} ON COLUMNS
FROM [SteelWheelsSales]
WHERE [Time].[Years].[{param-year}]
```

Next, in the **Components Panel**, add a **table component** (Others > Table Component), name it `comp_main_report`, assign the query `qry_main_report` and the html object `html_main_report`. Notice the consistent naming convention here: It is always good practice! As **Listener** define `param_year` and as **Parameters** `param_year` and `param_line`.

![](/images/mdx-drilldownmember-7.png)

Next click on **Advanced Properties** set following properties to `False`:

- Show Filter
- Info Filter
- Length Change
- Paginate
- Sort Data
- Execute at start

Set **Style** to `Bootstrap`.

We will use all the columns return by the MDX query (including the full path column, which we will hide later on). We can use the `Member Ordinal` value later on to add conditional formatting for the table so that it is easier to read.

A very nice benefit of using `DRILLDOWNMEMBER` is that the amount of columns/fields is not going to change. If we **drill down**, only the children's names will be displayed. So we do not have to change the table layout for the drill down.

For the table component's **PostExecution** function we define the following:

```javascript
function(){
	$('#html_table_wit_table_level1Table > tbody > tr').on({
		click: function(e){
			// get text value user clicked on
			// in this case we want to pick up the full MDX path of the member
			// [OPEN] adjust/improve
			var chosenPath = e.originalEvent.originalTarget.firstChild.data;
			console.log("Requesting data for: " + chosenPath);
			var mdxFragment =
				"DRILLDOWNMEMBER(DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE),"
				+ chosenPath
				+ ")"
			;
			// update parameter value with chosen member
			dashboard.fireChange("param_wit", mdxFragment);
		}
	})
}
``` 

The above snippet will make sure that we pick up the full path / unique name of the member the end user want to drill down on. We then update the parameter value for `param_wit` with the chosen value. We make the **table component** listen to this very parameter, so it will auto-refresh once the parameter value gets changed.

Now while the above works, it is not a good idea! **There are two problems**:

1. The **MDX can be easily injected** because it is generated client-side. The logic should sit within the MDX query itself.
2. There is **no need for HTML scrapping**: CDE offers a **Click Action** event, which offers easy access to the required info (so there is no need to use jQuery here).

We can actually get hold of the underlying data itself instead of using **jQuery** to extract the values from HTML: There is a real separation between data and presentation. Open the dashboard (with the parameter in the URL `debug=true`) in Firefox, go to **Developer Tools** and then click on **Debugger**. In the **Sources** pane on the left find `generatedContent` and click on it. Find the `postExecution` function in the code that relates to your **table component** and just within the function definition right click on the line number and choose "**Add Breakpoint**". Reload the dashboard - at the bottom of the **Debug** tab you'll find a **Variables** pane which will allow you to inspect the `this` object (or alternatively type in the **Console** `this` and hit enter). The current `this` object will be returned (so `this` object generated within the `PostExecute` function where we set the **breakpoint**). If you explore the `this` object, you will see that it has a `rawData` property which in turn holds the `resultset`. So we can extract the required context info from there. Thanks to Nelson Sousa for showing me this and highlighting the problems mentioned above. 

Go ahead and play with the object a bit in the **Console** to get familiar with it, e.g.:

```javascript
this.rawData.resultset
// get the value of the second row and second column/field
this.rawData.resultset[1][1]
this.rawData.resultset.forEach(function(r, i){ console.log(r[0] + ", index: " + i);})
this.rawData.resultset.forEach(function(r, i){ if(r[0] == "Credits") console.log(i);})
```

In the **Table Component** set the **Column Formats** for the `Member Full Path` and `Member Ordinal` to `hidden`. This will remove the columns from the **DOM**, however, the data will still be available in the table component's **resultset**.

Another important improvement is to use the CDE **clickAction** instead of the **JQuery click event** we currently use in the **PostExecution** function - in fact, this is the recommended/best practice approach.

```
function(e){
	var chosenPath = e.rawData.resultset[e.rowIdx].[e.colIdx+1];
	console.log(chosenPath);
			var mdxFragment =
				"DRILLDOWNMEMBER(DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE),"
				+ chosenPath
				+ ")"
			;
			// update parameter value with chosen member
			dashboard.fireChange("param_wit", mdxFragment);
}
```

As you can see using the CDE **clickAction** is a lot easier, as the required contextual information like row and column index are available straight away.

For now we can test that this approach is working. However, as you can see, this still does not get rid of the **MDX Injection problem**. So instead of sending an MDX fragment as the parameter value, we should send something less sensitive, like just a simple value that cannot do any harm:

```
function(e){
	// pick up member name only - not whole path
	var chosenMember = e.rawData.resultset[e.rowIdx].[e.colIdx];
	// pick up level ordinal
	var reportLevel = e.rawData.resultset[e.rowIdx].[e.colIdx+2];
	// update parameter value with chosen member
	dashboard.setParameter("param_report_level", reportLevel);
	dashboard.fireChange("param_wit", chosenMember);
}
```

Create a new custom parameter call `param_report_level` and leave its default value unset. Remove the default value for parameter `param_wit`.

Adjust the **MDX query** to this new version:

```sql
WITH
SET WIT AS
	IIF(
		"${param_report_level}" = "" OR "${param_report_level}" = "0"
		, DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE)
		, DRILLDOWNMEMBER(
			DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE)
			, [WIT.WIT Hierarchy].[WIT].[${param_wit}]
		)
	)
	MEMBER [Measures].[Member Full Path] AS
		[WIT.WIT Hierarchy].CurrentMember.UniqueName
	MEMBER [Measures].[Member Ordinal] AS
		[WIT.WIT Hierarchy].CurrentMember.Ordinal
SELECT
	NON EMPTY WIT ON ROWS
	, {
		[Measures].[Member Full Path]
		, [Measures].[Member Ordinal]
		, [Measures].[MyOtherMeasure]
	} ON COLUMNS
FROM [myCube] 
```

Make sure your MDX query has `param_report_level` defined as additional parameter.

**Important considerations** for setting default value of parameter:

```
param_report_level = 0
```

Results in following error:

```
[info] WD: BuildQueryDefinition detected static parameter ...
The parameter will be used instead the parameter value
```

[This Jira case](http://jira.pentaho.com/browse/CDF-435) suggests tackling this problem like so:

```
param_report_level = function(){
	return 0;
}
```

No, this doesn't solve it either. Just don't define any default value and check in the MDX query against an empty value (always treat as String).


While we got the first **drill down** working now, we will have to add an additional parameter to make the second drill down working: If we drill down to the **second level**, we also have to **keep the context** of the **previously chosen first level member**. We introduce a new parameter called `param_sub_wit` with no default value.

Next we adjust the **clickAction** for the table component (only changed section shown below):

```
dashboard.getParameterValue("param_report_level", reportLevel);
if (reportLevel < 2){
	dashboard.getParameterValue("param_wit", chosenMember);
} else {
	// do not overwrite
	// dashboard.getParameterValue("param_wit", chosenMember);
	dashboard.getParameterValue("param_sub_wit", chosenMember);
}
```

Adjust the **MDX Query**:

```sql
WITH
SET WIT AS
	IIF(
		"${param_report_level}" = "" OR "${param_report_level}" = "0"
		, DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE)
		, IIF(
			"${param_report_level}" = "1"
		, DRILLDOWNMEMBER(
			DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE)
			, [WIT.WIT Hierarchy].[WIT].[${param_wit}]
		)
		, DRILLDOWNMEMBER(
		,DRILLDOWNMEMBER(
			DESCENDANTS([WIT.WIT Hierarchy], 1, SELF_AND_BEFORE)
			, [WIT.WIT Hierarchy].[WIT].[${param_wit}]
		)
		, [WIT.WIT Hierarchy].[${param_wit}].[${param_sub_wit}]
)
	)
	MEMBER [Measures].[Member Full Path] AS
		[WIT.WIT Hierarchy].CurrentMember.UniqueName
	MEMBER [Measures].[Member Ordinal] AS
		[WIT.WIT Hierarchy].CurrentMember.Ordinal
SELECT
	NON EMPTY WIT ON ROWS
	, {
		[Measures].[Member Full Path]
		, [Measures].[Member Ordinal]
		, [Measures].[MyOtherMeasure]
	} ON COLUMNS
FROM [myCube] 
```
 
## Making the data readable

We add some **JavaScript** to tag the rows with CSS properties according to MDX level. Add to table component's **Draw Function**:

```javascript
function(){
	var myData = this.rawData.resultset;
	// check if resultset is not empty
	// otherwise we will get an error and no empty table will be displayed
  if(myData.length > 0){
  	$("#" + this.htmlObject).find("tbody > tr").each(
  		function(i, d){
  			$(d).addClass("drill-down-level-" + myData[i][2]);
  			$(d).find("td:first").addClass("drill-down-level-" + myData[i][2] + "-node");
  		}
  	)
  }
}
```

> **Note**: Did you realise how we use the underlying dataset to extract the current level ordinal? True separation of content and display.

Finally we add some very basic CSS properties to **Layout > Inline CSS**:

```css
.numeric{
  text-align:right;
}

th{
  font-size:0.9em;
  padding-left:0.2em;
  background-color:gold;
  border-bottom: 1px solid white;
}

td{
  font-size:0.9em;
}

tbody > tr > td.column0:hover{
  background-color:lightblue;
}  

.drill-down-level-0-node {
	padding-left: 0em;
}

.drill-down-level-1-node {
	padding-left: 0.4em;
}

.drill-down-level-2-node {
	padding-left: 2em;
}

.drill-down-level-3-node {
	padding-left: 3em;
}

.drill-down-level-0 {
	background-color:#9999ff;
}

.drill-down-level-1 {
	background-color:#b2b2ff;
}

.drill-down-level-2 {
	background-color:#ccccff;
}

.drill-down-level-3 {
  background-color:#e5e5ff;
}
```





