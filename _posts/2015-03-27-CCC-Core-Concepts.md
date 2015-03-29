---
layout: post
title:  "Pentaho CCC Core Concepts"
summary: In this article explains readers, dimensions and visual roles.
date:   2015-03-29
categories: CCC
tags: CCC, CDE
published: true
---

## Pentaho CCC Core Concepts ##

This article is an attempt to describe the basic layers of **Pentaho CCC**. To my knowledge there is currently no concise  documentation, which provides an overview of the essential terms and structure of **CCC** and as a result this topic can be rather confusing to newcomers and even seasoned dashboard developers.

A lot of information for this article has been provided by **Duarte Cunha Leão**, lead architect of **CCC**, for which I want to thank him a lot.

**Data Layer**

Term | Definition  
 ------	| ------
**Data** | a table; contains a collection of columns and one of rows. It's, in general, an hierarchical table. Contains rows and columns, but also, it is a row itself (has an atoms property) and has child data: childNodes property.
**Dimension** | a column in a table (nothing to do with MDX dimensions); In the object model, it contains the subset of atoms present in its data.
**Datum** | a row/record; contains the values for all columns.
**Atom** | like a spreadsheet cell, holds the value and label (and key, ...) of a column; however, atoms are shared by all rows having the same value in a given column. This allows: fast object comparison and formatting values only once, and is akin to "string interning".


**Metadata Layer**

This relates to the input data.

Term | Definition  
 ------	| ------
**Complex type** | defines the structure of a kind of table/record; essentially a collection of column types.
**Dimension type** | defines a column type.

**Visual Layer**

Term | Definition  
 ------	| ------  
**Scene** | describes one instance of visual representation; contains a collection of variables; scenes are organized hierarchically, and are **generally local to each panel** (axis, legend, plot); generally, contains a list of the datums that are there represented; depending on the kind of scene, it may contain one Datum, a list of Datums, one Data or many Data. The `datums()` method of a scene allows you to iterate through the present datums.  
**Variable** | holds a business value, before it is encoded with a scale, but after it has been grouped and aggregated to match the actual representation; it is not an already visually encoded value, like a number of pixels or an rgb color; most are the value of a same named visual role (although you'll note that it is not the case in panels like the cartesian axis and the color legend).
**Visual Role** | represents a main, high-level, visual function or characteristic of a visualization that varies according to bound data (ex: color, title, size, series, ...).
**Context** | the CCC context object is the value of the JavaScript `this` in CCC extension points, and other callbacks; it provides easy access to commonly needed objects like the chart, the panel, the underlying protovis mark, `pvMark`, the scene, and last but not the least, the CCC's counterpart, and wrapper, of protovis marks, the sign.  

The mapping between the data and visual level is made by specifying the data dimensions that a visual role is bound to. For example, you say:

```javascript
categoryRole: "productFamily, productType"
```

to bind the category visual role to the tuple of dimensions productFamily and productType.

Most of the times, however, because the default dimension names are used, it looks like this separation does not exist - the visual role "category" is bound to a dimension named "category":

```javascript
categoryRole: "category"
```

You'll only appreciate, and take advantage of, this separation of concerns if you name the data dimensions by their actual business names, and then manually map these to desired visual roles, or if you use more than one dimension in a visual role; the latter happens often, for example with the "category" visual role, because, by default, the excess discrete columns in a data source are loaded as dimensions named "category", "category2", "category3", ... and the "category" visual role binds automatically to dimensions having a name that starts with "category".


## Mapping Input Data to Visual Output ##

You have various options to map your input data, there are actual several mapping layers, most of which are optional. This article will walk you through the process: from data ingestion to the visual representation. The next sections are listed in this order.

### Relational Translator ###

First we have to let **CCC** know in which structure our input data is in: This can be specified by setting the `crosstabMode` and `seriesInRows` properties.

If the input dataset is in a cross tab format, the translator will transform it into a relational structure ([Source](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries&p=346596#post346596)):

**Crosstab**

|    |  COLData
-------|------
| ROWData | MeasureData

**Relational**

COL | ROW | VALUE
----|----|-----
COLData | ROWData | MeasureData


> **Note**: In the relational format column headers/titles are not part of the dataset - they are metadata. If you compare this with the crosstab format, column and row titles are part of the dataset.

If the input data is already in a relational structure, the output (the virtual array item - more about that later on) of the translator will be the same.

After processing the input data, the **Relational Translator** outputs a new structure, which is called **Virtual Item Array** (aka Virtual Row). Note that the **Relational Translator** relocates all discrete columns to the left, so the output column structure will be very likely different from the input one.

Let's properly understand the settings:

- **Crosstab**:
	- *True*: Each measure has its own column, the column label is the title of the measure and at the same time part of the dataset. The **measure column title** can act as series or category value and is hence **part of the dataset** (and not just metadata).
	- *False*: The column headers/titles are not part of the dataset, hence they will also not be visible in the chart. They act solely as metadata.
- **Series In Rows**: This swaps the "category" and "series" data, in either format - so, it doesn't do what is says in the name in all circumstances (The name has its roots in the crosstab format and the functionality of the translator was later on extended).


Cees van Kemenade explains in his C-Tools Guide: "If cross tab mode = False the chart component is expecting a three column dataset where each row corresponds to exactly one data-point. Here the parameter series-in-rows determines the order of the columns. If series in rows = false the expected order is Series, Category, Data. If series in rows = true the expected order is Category, Series, Data".

Any combinations of the above are valid:

Crosstab Mode | Series In Rows | Description
-----|------|-----
true | true | one or more measure column, their column titles act as category values (they are part of the dataset), the series values are present as data points (not column title) in the first column
true | false |  one or more measure column, their column titles act as series values (they are part of the dataset), the category/categories values are present as data points (not title) in the left most column(s)
false | true |  The dataset is in following column order: **Category, Series, Data**. Non of the column titles are used for the chart data. The column titles are solely metadata  
false | false |  The dataset is in following column order: **Series, Category, Data**. Non of the column titles are used for the chart data. The column titles are solely metadata. 

> Usually datasets generated by a MDX query or SQL GROUP BY queries are in cross tab mode.


### Dimensions ###

For dimensions, you can define three main properties:

- **Readers** (Dimension Readers)
- **Types** (Dimension Types)
- **Calculations** (Dimension Calculation)


#### Readers (Dimensions Readers) ####

Readers allow you to declare the **mapping of columns to dimensions**. As all the next mapping levels, this step is optional.  

Historically readers were created before CDA existed. Duarte explains: "If we could have always used the CDA dataset colName, the `readers` option would not have been invented."

There are two strategies to define reader names, using: 
- visual role names or visual row prefix names: `category`, `series`, `measure`, `value` etc
- business dimension names

##### Using Visual Role Names #####

> **Note**: If a reader is named or prefixed with `category`, `series`, `measure`, `value` ** etc, **Visual Roles** (more about that later on) will automatically bind to the same same-name or same-name-prefixed dimensions. And, as such, binding a column to a dimension named "category" and not explicitly binding the category role, actually binds the column to the "category" visual role. ** Note that, strictly speaking, in **CCC** terms they are called [Dimension Groups](http://www.webdetails.pt/ctools/charts/jsdoc/symbols/pvc.options.charts.BasicChart.html#dimensionGroups).

Normally you map to one of these most common dimension names:

Some dimension names, are special in the sense that they:
- inherit default attributes (like all dimensions of the "value" *dimension group* — named "value*" — inherit a `valueType` of `Number`)
- have a name prefix that is the name of common visual roles...

- **category**
- **series**
- **measure**
- **value**

Depending on the chart type there might be other dimension types available like e.g. median, lowerQuartil, upperQuartil, minimum, maximum (these ones are all for a box plot).

Example:

```javascript
readers: ['measure, series, category, value']
```

Note: This is not nested inside the `dimensions` object, but directly on the top level.

Here you are basically saying: Map column 1 to a measure dimension, column 2 to a series dimension, column 3 to a category dimension and column 4 to a value dimension.

> **Note** that there is no CCC visual role named "measure" and so, doing it this way, you either need to then map "measure" to an existing visual role explicitly, or you just want it to show in the tooltip and not be graphically represented in any other way.

> **Note**:  It's ok if you also map some positions (columns) to dimensions that aren't named after any visual role and the result is that that these dimension will not be graphically represented: it has no visual function. It can be used to show in the tooltip or simply for interfacing with external components in handlers.

> **Important**: Readers are not defined in the index/column order of the original input dataset! There is one more step in between which you have to be aware of: A **Relational Translator** processes the original input dataset and relocates all **discrete** columns to the left. The relations translator outputs a **Virtual Item Array** (aka Virtual Row or Logical Row) and the index order of the readers has to be based on this virtual item array! The only way to understand how the virtual item array looks like is to use the CCC debugger. See also [this forum post](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries). There are given rules for how the logical row is formed, for each of the translators.

As you can see in the code definition above, there is no way to define that column name X from the input dataset maps to dimension name Y. All you do is basically take the column order ( = index order) of the **Virtual Item Array** into account and then list the dimensions that you want it to map to: *measure* in the above example maps to index 0 (or column 1) in the **Virtual Item Array**.

##### Using Business Dimensions Names #####

Instead of defining measures, categories, series and values as readers, you can also mention their real names e.g.:

```javascript
readers: ['region, brand, quantity, sales'],
```

In this case you have to map both readers​ and visual roles. 

When would you want do this? E.g. when you have 2 plots within one chart and want the first plot to use a different column as a value than the second one. In this case you would define an additional mapping for category, series and value within the **Visual Roles** (more on that later on). In a nutshell: In this case the datums/rows are the same, but each plot maps its "value" visual role to a different dimension.

```javascript
 plots: [
        {
            name: 'main',
            visualRoles: {
                value:    'count', // <-- mapping defined here
                series:   'city',  // <-- mapping defined here
                category: 'period' // <-- mapping defined here
            }
        },
        {
            type: 'point',
            linesVisible: true,
            dotsVisible:  true,
            orthoAxis: 2,
            colorAxis: 2,
            nullInterpolationMode: 'linear',
            visualRoles: {
                value: 'avgLatency', // <-- mapping defined here
                color: {legend: {visible: false}}
            }
        }
    ],
```

Other advantages of using **business dimensions names**:

- extension point/callback code that interfaces with external components is clearer, because it ties to the names of business concepts and not to the visual role they're playing
- changes to the visual mapping don't break such external interfacing code
- dimension labels are automatically generated as capitalized, case-change split versions of the dimension names.

To compare these two approaches, take a good look at these two **CCC Examples**: [PAIRED BAR-LINE MEASURES](http://www.webdetails.pt/ctools/ccc/#type=bar&anchor=paired-bar-line-measures) and [REALLY PAIRED BAR-LINE MEASURES](http://www.webdetails.pt/ctools/ccc/#type=bar&anchor=really-paired-bar-line-measures). Also note that the `crosstabMode`  is set differently in these two examples.


From the [JSDoc](http://www.webdetails.pt/ctools/ccc/charts/jsdoc/symbols/pvc.options.DimensionsReader.html#reader):

A dimensions reader function can \[also\] be specified to perform non-simple operations over the read cells, like the following:

- combine values from two or more cells into a single dimension,
- split the value of one cell into more than one dimension,
- feed a dimension with correlated data read from an external data source.

**Do not use** Readers for **formatting** purposes or to perform **conversions**. Use the formatting and conversion properties in the `dimensions` object therefore. Also, when the value of a dimension is **calculated** from the value of other dimensions, a dimensions calculation may be more appropriate. When datums/rows are generated for trends or null interpolation, calculated dimensions get filled in, but readers don't get called.

#### Dimensions (Dimensions Type and other general properties) ####

Data dimensions can (but do not have to) be explicitly defined. When dimensions are not defined, default dimensions, with default options, are generated to satisfy the needs of the chart. It is possible to define one dimension, totally or partially, and let the others be automatically generated.

You can also define various other properties, like **valueType**, **formatting**, **visibility** etc. Example:

```javascript
dimensions: {
        // Dimension bound to "dataPart" is hidden by default
        region: {isHidden: false},
        // Sort brands
        brand:  {comparer: def.ascending},
        // Notice the currency sign and the /1000 scale factor (the comma beside the dot).
        sales:  {valueType: Number, format: "$#,0,.0K"}
},
```

So within `dimensions` you can list your dimension names (those specified in the `readers` or those automatically inferred [if `readers` is not specified **CCC** will make a good guess]) and then for each of them you can define various properties/options. A list of available options ([Source](http://www.webdetails.pt/ctools/ccc/charts/jsdoc/symbols/pvc.options.DimensionType.html)):

Name | Type | Default | Description
------|-------|-----|-----
label | string | dimension name | The name of the dimension for the user.
isHidden | boolean | false | Indicates if a dimension should not be shown to the user, in tooltips or the like.
valueType | function | null | Indicates the data type of the values of the dimension. null means any type. The possible values are null, String, Number, Boolean and Date.
isDiscrete | boolean | varies | Indicates if the dimension should be considered discrete or continuous.  The default value for the value types Number and Date is continuous, false, and for the other is discrete, true. Only Number and Date can be continuous. The distinction of a dimension being being discrete or continuous from its value type, allows, for example, to have a date category with the same formatting capabilities as when a date is considered continuous (time-series).
converter | function | varies | A function that receives a source data value and converts it to a value of the dimension's value type. The default value depends on the value type. For the value type Number, it is a function that parses a numeric string, and for the value type Date, a function that parses a date with the format in rawFormat.
rawFormat | string | varies | Specifies the source format by which default converter functions should parse input values. The format is that of protovis' formats. Currently, only used by the Date value type's default converter, for which the default raw format is the one specified in chart option timeSeriesFormat (its default is "%Y-%m-%d").
formatter | function | varies | A function that receives a value of the dimension's value type and formats it to be shown to the user. The default value depends on the value type. For the value type Number, it is a function that formats numbers with at most two decimal places, and for the value type Date a function that formats dates with format declared in the `format` option.
format | string | varies | Specifies the format by which default formatter functions should format values. The format is that of protovis' formats. Currently, only used by the Date value type's default formatter, for which the default format "%Y/%m/%d" is assumed.
comparer | function | varies | A function that receives two values of the dimension's value type returns a -1, 0 or 1, depending on whether the first value is less than, equal to, or greater than the second value, respectively. The default value depends on the value type. A default "natural" comparer is assumed when the value type is Date or the value type is Number and continuous.

> **Note**: Stating a dimension name in `readers` or in `visualRoles` implicitly defines it, with default attributes.

##### Additional Notes on Dimension Properties #####

###### What is the difference between a measure and a value dimension

Here we discuss two different the measure concepts: `dataMeasuresInColumns` and the `measure` dimension.

[Technical Ref](http://www.webdetails.pt/ctools/charts/jsdoc/symbols/pvc.options.charts.BasicChart.html#dataMeasuresInColumns): In the cross-tab format, indicates if, a column exists for each series value and measure dimension. This option is only meaningful when `isMultiValued` ([Ref](http://www.webdetails.pt/ctools/charts/jsdoc/symbols/pvc.options.charts.BasicChart.html#isMultiValued)) is true. When true, more than one measure can be represented in the cross-tab format, for each series and category pair.

When false, the cross-tab format can represent only one measure. 

Duarte explains: There is an option in CCC which says `dataMeasuresInColumns`.​ The name `measure`, here, also comes from the crosstab format and associated MDX mindset. That's why the corresponding Physical Column Group was called `measures`. In practice, measures end up as continuous `valueType`: Number/Date dimensions, with `isDiscrete`: false (note you can have a discrete numeric or date dimension).
In terms of dimension names, historically, in CCC, measures were associated to the  plotted `value`, and are thus named.

So, the difference is one of concept and of CCC evolution history​, but, in practice, all value dimensions are considered measures.

The example you have with a dimension named `measures` might just be bringing some confusion. That is a quite different thing. It's kind of a series which says which measure dimension is actually contained in the `value` dimension. I sometimes call this dimension a **measure discriminator**.
The disadvantage of this approach is that there is a single `value` dimension, and with that, there is a single formatter, label, etc. So, having both a quantity and a currency be represented this way won't work. This is the big difference between those two "paired bar-line measures" examples you refer.

###### Color Role ######

The below example is only applicable for the metric point / scatter, whose color role accepts both discrete and continuous dimensions:

By default, the color role is set to be bound to a numeric dimension. If required, however, you can use discrete dimensions for colouring bars, lines etc. To configure the colouring logic you have two options: 

- Use the `color` **role**, in which case you have to change the dimension's `valueType` from a numeric to a discrete value type:

	```javascript
	dimensions: {
	    color: {
	        valueType: String
	    }
	}
	```

- Use the `series` **role**: When the `color` role is not explicitly bound, it uses the dimensions bound to the `series` role. So, setting the `series` role impacts the `color`, by default. The series is always **discrete**. Using this approach changes the drawing order of the bars, lines etc.

[Source](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries&p=346596#post346596))


#### Calculations (Dimensions Calculation) ####

From the [JSDoc](http://www.webdetails.pt/ctools/ccc/charts/jsdoc/symbols/pvc.options.DimensionsCalculation.html): A dimensions calculation allows the values of ([DCL] one or more dimensions) a dimension to be calculated from the typed values of the other non-calculated dimensions.
While a dimensions reader could achieve the same result, it works by reading values from the virtual item, accessing it by index. That would require the knowledge of the indexes in which the desired dimensions were, which is many times not true, specially when the mapping between dimensions and virtual item indexes is determined automatically by the data translator.

The below example list the calculation used in [this sample chart](http://www.webdetails.pt/ctools/ccc/#type=bar&anchor=paired-bar-line-measures): This creates a new calculated dimension called *dataPart*:


```javascript
dimensions: {
	...
    calculations: [{
        // Split rows into != data parts,
        // depending on the "measure" dimension's value.
        names: 'dataPart',
        calculation: function(datum, atoms) {
            atoms.dataPart =
                datum.atoms.measure.value === 'Count' ?
                '0' :  // main plot:   bars
                '1' ;  // second plot: lines
        }
    }],
    ...
}
```


### Visual Roles ###

The `visualRoles` option allows you to declare the **mapping from dimensions to visual roles**.

> **Note**: You only have to define **Visual Roles** if you defined  custom reader names (so not category, series, measure, value).

DS: Hm, not always, [this chart](http://www.webdetails.pt/ctools/ccc/#type=line&anchor=line-with-5-number-statistics) doesn't use any readers ... Where do these value1-5 visual roles come from?

DCL: The translation depends on the hosting chart type. In this case, the chart is a line chart - it has a main plot of type "point" (with lines forcibly visible). Because of that, it simply generates "value\*" dimensions by default.
The box chart, on the other hand, defines dimensions having the names of its main plot's visual roles by default.
When using a box plot in a line chart, we only have automatically generated "value*" dimensions.

```javascript
            type: 'box',
            visualRoles: {
                // Comment the ones you don't want represented
                median:       'value',
                lowerQuartil: 'value2',
                upperQuartil: 'value3',
                minimum:      'value4',
                maximum:      'value5'
            },
```

**CCC** has following **Visual Roles** properties (availability depends on chart type):

- **categoryRole**: shorthand for `visualRoles: { category: '<yourCol>' }`
- **seriesRole**: shorthand for `visualRoles: { series: '<yourCol>' }`
- **dataPartRole**: shorthand for `visualRoles: { dataPart: '<yourCol>' }`. If you have multiple charts, this definition is used to separate the charts (so for each value of the column defined in `dataPartRole` one chart will be created). Dimensions bound to `dataPart` are hidden by default.
- **multiChartRole**: applies only to Multi-Charts
- **valueRole**: the actual numeric value. Shorthand for `visualRoles: { value: '<yourCol>' }`
- **visualRoles**: Can contain any of the above, but the syntax is a bit different (without the `Role` suffix):


```javascript
visualRoles: {
	dataPart: `<yourCol>`
	, value: `<yourCol>`
	, category: `<yourCol>`
	, series: `<yourCol>`
	, ...
}
```

What is the difference between `dataPartRole` and `multiChartRole`?  In a **Multi-Chart** the chart definition for all the charts is exactly the same and the `multiChartRole` is used to "group" these charts. This is different to a chart definition which has more than one plot defined, where `dataPart` is used to apply a specific subset of the data to one specific plot. E.g. If you defined `dataPart: 'region'` in your `visualRoles`, then you can reference a specific subset of this data for one of the specified plots in your chart definition like this: `dataPart: 'EMEA'`.

> **Note**: The dimensions that are not mapped into visual roles will appear in the tooltip anyway (unless marked with isHidden: true). [Source](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries&p=346596#post346596)

### Examples ###


#### Practical Example ####

We will base this practice example on [this CCC example](http://www.webdetails.pt/ctools/ccc/#type=bar&anchor=paired-bar-line-measures).

We have following dataset:

```javascript
{
    "resultset": [
        ["London", "Jan", 35000,  141.3],
        ["London", "Apr", 40000,  120.12],
        ["London", "Jul", 45000,  115.6],
        ["London", "Oct", null,   110.37],
        ["Paris",  "Jan", 70000,  null],
        ["Paris",  "Apr", 80000,  180.9],
        ["Paris",  "Jul", 115000, 170.7],
        ["Paris",  "Oct", 45000,  145.5],
        ["Lisbon", "Jan", 70000,  200.7],
        ["Lisbon", "Apr", 90000,  190.3],
        ["Lisbon", "Jul", 120000, 180.2],
        ["Lisbon", "Oct", 30000,  130.067]
    ],
    "metadata":[
        {"colIndex": 0, "colType": "String", "colName": "City" },
        {"colIndex": 1, "colType": "String", "colName": "Period" },
        {"colIndex": 2, "colType": "Numeric", "colName": "Count"},
        {"colIndex": 3, "colType": "Numeric", "colName": "AvgLatency"}
    ]
}
```

As most people are quite likely more familiar to creating **CCC** charts via **CDE**, I will explain this approach here (but if you use **CCC standalone**, then the approach is not that much different). These are not extremely detailed instruction - I assume you are familiar with CDE:

In CDE define a new **Scriptable JSON** datasource. As query use the JSON object listed above.

Create an extremely simple HTML **Layout** with one column and one row.  

In the **Components** panel create a **CCC Bar Chart**. Reference the query and html element we just created. As **PreExection** function define the following (this avoids filling out all the properties manually, but if you fancy, you can just do that instead and not define a pre-execution function):

```javascript
function(){
    // override the CDE chart definition
    this.chartDefinition = {
        width:   600,
        height:  200,
        animate: false,
        crosstabMode: true
    };
}
```

Save the dashboard and then open it in a separate window (in standard view mode, not edit mode). In the view mode, it's time to activate the logging: So add the following to your URL:

````
?debug=true&debugLevel=5
````

Refresh the page and then open your browser's **Developer Tools** and observer the output in the console. You will find an abundance of information there! Expand the bit where it says `[pvc.BarChart ]  CCC RENDER`. This will show you:

1. **Data Source Summary**: Interesting to see the column types and names

	```bash
	╔═════════╤══════════╤════════╤═════════╤════════════╗
	║ Name    │ City     │ Period │ Count   │ AvgLatency ║
	╟─────────┼──────────┼────────┼─────────┼────────────╢
	║ Label   │          │        │         │            ║
	╟─────────┼──────────┼────────┼─────────┼────────────╢
	║ Type    │ String   │ String │ Numeric │ Numeric    ║
	╟─────────┼──────────┼────────┼─────────┼────────────╢
	║ 1       │ "London" │ "Jan"  │ 35000   │ 141.3      ║
	║ 2       │ "London" │ "Apr"  │ 40000   │ 120.12     ║
	║ 3       │ "London" │ "Jul"  │ 45000   │ 115.6      ║
	```

2. Which **Data Source Translator** was used: In our case the *Crosstab* one.

	```bash
	Crosstab data source translator
	```

3. **Virtual Item Array**: This reflects the inferred or configured readers settings. Initially you'll look at the `Type` and `Name` column to see what the translator is placing in each position of the virtual row. Interesting is also the `Kind` column and the `dimension` column, which tells you if the column was mapped to a series, category or dimension type.

	```bash
	╔═══════╤══════╤════════╤════════╤═══════╤═══════════╗
	║ Index │ Kind │ Type   │ Name   │ Label │ Dimension ║
	╟───────┼──────┼────────┼────────┼───────┼───────────╢
	║ 0     │ C    │ string │        │       │ series    ║
	║ 1     │ R    │ string │ City   │       │ category  ║
	║ 2     │ R    │ string │ Period │       │ category2 ║
	║ 3     │ M    │ number │        │       │ value     ║
	╚═══════╧══════╧════════╧════════╧═══════╧═══════════╝
	```

	**Virtual Item Array Summary (debugger output): Why do some columns not have a name?** Duarte: The Name and Label in that table come directly from the CDA dataset colName and colLabel attributes. The problem is that in the crosstab format, there is no such metadata as the colName and colLabel are used to convey data... This is said in: "Downsides: This form of encoding is nice and concise, but achieves that at the cost of occupying the space intended for metadata and, thus, resulting in absent metadata for the underlying entities "columns" and "measures" properties. Because of this reason, unfortunately, CCC could never really rely on the values of the "colName" and "colLabel" metadata attributes. We do try to use these, when they actually contain metadata, to default dimensions' labels." [Source](http://www.webdetails.pt/ctools/charts/jsdoc/symbols/pvc.options.charts.BasicChart.html#dataIgnoreMetadataLabels)


4. **Complex Type Information**: Shows a non-inferred dimension configuration, like overriding the inferred `valueType` ([Source](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries)). 


	```bash
	╔═══════════╤═════════════════════════════════════════╗
	║ Dimension │ Properties                              ║
	╟───────────┼─────────────────────────────────────────╢
	║ series    │ "Series", Any                           ║
	║ category  │ "City", Any                             ║
	║ category2 │ "Period", Any                           ║
	║ value     │ "Value", Number, comparable, continuous ║
	╚═══════════╧═════════════════════════════════════════╝
	```

5. **Visual Roles Map Summary**

	```bash
╔══════════════╤═════════════╤═════════════════════════════════════════╗
	║ Visual Role  │ Source/From │ Bound to Dimension(s)                   ║
	╟──────────────┼─────────────┼─────────────────────────────────────────╢
	║ multiChart   │ -           │ -                                       ║
	║ dataPart     │ -           │ -                                       ║
	║ bar.color    │ bar.series  │ series ("Series")                       ║
	║ bar.series   │ -           │ series ("Series")                       ║
	║ bar.category │ -           │ category ("City"), category2 ("Period") ║
	║ bar.value    │ -           │ value ("Value")                         ║
	╚══════════════╧═════════════╧═════════════════════════════════════════╝
	```

As you can see, **CCC** infers default dimension and visual roles. That's why I mentioned earlier on that it is not necessary to define them, unless you have good reason to do so.

Now that we understand the basics, let's add a **Readers** definition to our pre-execution code:

```javascript
function(){
    // override the CDE chart definition
    this.chartDefinition = {
        width:   600,
        height:  200,
        animate: false,
        crosstabMode: true,
        dataCategoriesCount: 2,
        readers: ['measure, series, category, value']
    };
}
```

Save the dashboard and refresh the page in 'view mode'. Notice that our chart is rendered differently now? Let's inspect what happened to the **Virtual Item Array**:

```bash
╔═══════╤══════╤════════╤════════╤═══════╤═══════════╗
║ Index │ Kind │ Type   │ Name   │ Label │ Dimension ║
╟───────┼──────┼────────┼────────┼───────┼───────────╢
║ 0     │ C    │ string │        │       │ measure   ║
║ 1     │ R    │ string │ City   │       │ series    ║
║ 2     │ R    │ string │ Period │       │ category  ║
║ 3     │ M    │ number │        │       │ value     ║
╚═══════╧══════╧════════╧════════╧═══════╧═══════════╝
```

Notice how the `Dimension` values changed?

Also `Dimension` and `Properties` values for the **Dimension Complex Type** definition changed:

```bash
╔═══════════╤═════════════════════════════════════════╗
║ Dimension │ Properties                              ║
╟───────────┼─────────────────────────────────────────╢
║ measure   │ "Measure", Any                          ║
║ series    │ "City", Any                             ║
║ category  │ "Period", Any                           ║
║ value     │ "Value", Number, comparable, continuous ║
╚═══════════╧═════════════════════════════════════════╝
```

And the *Bound to Dimension(s)* definitions in the **Visual Roles Map** changed as well:

```
╔══════════════╤═════════════╤═══════════════════════╗
║ Visual Role  │ Source/From │ Bound to Dimension(s) ║
╟──────────────┼─────────────┼───────────────────────╢
║ multiChart   │ -           │ -                     ║
║ dataPart     │ -           │ -                     ║
║ bar.color    │ bar.series  │ series ("City")       ║
║ bar.series   │ -           │ series ("City")       ║
║ bar.category │ -           │ category ("Period")   ║
║ bar.value    │ -           │ value ("Value")       ║
╚══════════════╧═════════════╧═══════════════════════╝
```

Now let's see what happens, if we define the readers names other than measure, series, category, value.

Let's try this (this is based on [this CCC example](http://www.webdetails.pt/ctools/ccc/#type=bar&anchor=really-paired-bar-line-measures) - just for the reference):

```javascript
function(){
    // override the CDE chart definition
    this.chartDefinition = {
        width:   600,
        height:  200,
        animate: false,
        crosstabMode: true,
        dataCategoriesCount: 2,
        //readers: ['measure, series, category, value']
        readers: ['city, period, count, avgLatency']
    };
}
```

If you render the dashboard now the chart area will show following error:

```bash
Error: Invalid operation. The required visual role 'value' is unbound.
```

In this case you have to define in your chart plot the **Visual Role** mapping. E.g.:

```javascript
 plots: [
        {
            name: 'main',
            visualRoles: {
                value:    'count', // <-- mapping
                series:   'city',  // <-- mapping
                category: 'period' // <-- mapping
            }
        },
        ...
```

Let's return to our original approach (as you should by understand the concept of readers by now) and add a `dimensions` definition to the pre-execution function:

```javascript
function(){
    // override the CDE chart definition
    this.chartDefinition = {
        width:   600,
        height:  200,
        animate: false,
        crosstabMode: true,
        dataCategoriesCount: 2,

        readers: ['measure, series, category, value'],

        dimensions: {
            // Explicitly define the "measure" dimension
            // (change the defaults that would otherwise take effect)
            measure: {
                // Hide "measure" from the tooltip
                isHidden: true,

                // Fine tune the labels
                formatter: function(v) {
                    switch(v) {
                        case 'Count':      return "Count";
                        case 'AvgLatency': return "Avg. Latency";
                    }
                    return v + '';
                }
            }
        }
    };
}
```

Save the dashboard and reload the view-mode page to see the log output. You can continue to analyse the setup on your own now - I think you understand the approach by now. If you want see changes in the Dimension Complex Type summary table, just set `dimensions.valueType` differently. 

#### Simple Example ####

Below just a few code snippets:

```javascript
 var chartOptions = {

     /* Data mapping */
     readers: {
         {names: 'territory, country'}
     },

     /* Data definition */
     dimensions: {
         territory: {label: "Territory"},
         country:   {label: "Country"  }
     },

     /* Visual Role mapping */
     visualRoles: {
         multiChart: "territory, country desc"
     },

     // ... other options ....
 };

 var chart = new pvc.BarChart(chartOptions);
```

Using the color role explicitly [Source](http://forums.pentaho.com/showthread.php?145514-Metric-Dot-Chart-Scatter-Plot-with-boundaries&p=346596#post346596):

```javascript
// Specify which dimensions are numeric (this might be inferred from the cdaData metadata, not sure, so maybe this isn't needed)
// Anyway, you might want to specify different formatters, labels, etc., and this is the place to do so.
dimensions: {
    age: { valueType: Number },
    salary: { valueType: Number },
    seniority: { valueType: Number },
    salaryMin: { valueType: Number },
    salaryMax: { valueType: Number }
},

// Specify the virtual item mapping into dimension names:
readers: [
   "gender, name, function, age, salary, seniority, salaryMin, salaryMax"  // a single reader definition that reads virtual item columns into the specified dimensions, in the given order
],

// Map dimensions to visual roles
visualRoles: {
   color: "gender",
   x: "salary",
   y: "age",
   size: "seniority"
}
```

[This](http://stackoverflow.com/questions/23656658/pentaho-cde-chart) also quite an interesting example.

#### Advanced Example: Defining Dimensions with a Lookup ####

Imagine that the data source contains a product type dimension whose value is the product type code. The code is needed for drill-down purposes, or for opening a dialog based on the user's selection.
Yet, we would like to show a proper description to the user. Suppose a separate lookup table exists for "decoding" product type codes. The product type dimension could be defined as follows:

```javascript
 var productTypeLookup ={
     "BO": "Boeing",
     "AB": "Air bus",
     "BY": "Bicycle"
 };

 var chartOptions = {
     categoryRole: 'productType',

     dimensions: {
        productType: {
            label: 'Product type',
            valueType: 'string',    // [DCL] --> Should be `String`, literally
            formatter: function(code){
                return productTypeLookup[code];
            }
        },

       readers: [
           // First column of data source feeds the 'productType' dimension
           {names: 'productType', indexes: 0}
        ]
     }
     // ... other options ...
 };

 var chart = new pvc.BarChart(chartOptions);
```
The example also shows how the product type dimension would receive its values, by specifying a reader for it, although this isn't further developed here. Also, the example shows how to assign the defined dimension to the category visual role.

See DimensionType JS Doc for a list of all the available options.
