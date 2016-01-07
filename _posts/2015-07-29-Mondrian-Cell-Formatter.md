---
layout: post
title:  "Pentaho Mondrian: Custom Formatting with Cell Formatter"
summary: This article covers the basics of using the Mondrian Cell Formatter feature.
date: 2015-07-29
categories: Mondrian
tags: Mondrian
published: true
---

Formatting measures in cubes is quite essential for the readability of any analysis results. **Pentaho Mondrian** features a `formatString` (attribute of measure declaration in a Mondrian **Cube Definition**) and `FORMAT_STRING` (**MDX** query) options which allow you to define the format in a **Visual Basic** syntax. Say you want to display 12344 as 12,333 you can easily create following formatting mask: `#,###`. This kind of approach works for most use cases.

However, sometimes you might required a custom formatting option which is not covered by the **Visual Basic** formatting options. Imagine e.g. that one of the measures of your cube is a duration in seconds. The integer value that you store in your database is not really a nice way of presenting this measure, unless of course you are only dealing with very small figures. But what if you have e.g. a figure like `102234` seconds. How many days, hours etc is this?

One approach of dealing with this is to create hidden calculated members which break this figure down into days, hours, minutes etc:

measure | mdx calculation
-----|------
days | `CAST(INT(sec/(60*60*24)) AS INTEGER)`
hours | `CAST(INT((sec/(60*60))-(days*24)) AS INTEGER)`
minutes | `CAST(INT((sec/60)-(days*(24*60))-(hours*60)) AS INTEGER)`
seconds | `CAST(ROUND(sec-(days*(24*60*60))-(hours*60*60)-(minutes*60),0) AS INTEGER)`

You could then create a visible measure which concatenates all these invisible measures and displays the figure as `1d 4h 24min 54sec`. Example final calculated visible measure:

```sql
CAST([Measures].[Days] AS STRING) || "d " || IIF([Measures].[Hours] < 10, "0", "") || CAST([Measures].[Hours] AS STRING) || ... and so forth
```

However, while this approach works, you will realize that you cannot sort by this measure properly! That's rather inconvenient.

Fortunately enough, **Mondrian** also provides a [Cell Formatter](http://mondrian.pentaho.com/documentation/schema.php#Cell_formatter), which allows you to access the value of e.g. a measure and manipulate it any way for display purposes, but - and this is the very important bit - this does not influence the underlying data type. So in our example, the integer value for the duration will still be an integer value and hence the **sorting** will work! The other really good point is that you can use various languages to manipulate the value, e.g. **Java** or **JavaScript**. 

To add a special **Cell Formatter**, simply nest the `CellFormatter` **XML element** within the `Measure` or `CalculatedMeasure` XML element. Then nest another `Script` XML element within this one to specify the **Script Language** and finally nest your code within this element. Example (this time including weeks as well):

```xml
<Measure name="Duration" column="duration" visible="true" aggregator="sum">
    <CellFormatter>
        <Script language="JavaScript">
            <![CDATA[
            var result_string = '';
            // access Mondrian value
            var sec =  value;
            var weeks = Math.floor(sec/(60*60*24*7));
            var days = Math.floor((sec/(60*60*24)) - (weeks*7));
            var hours = Math.floor(sec/(60*60) - (weeks*7*24) - (days*24));
            var minutes = Math.floor((sec/60) - (weeks*7*24*60) - (days*24*60) - (hours*60));
            var seconds = Math.floor(sec - (weeks*7*24*60*60) - (days*24*60*60) - (hours*60*60) - (minutes*60));
            result_string = weeks.toString() + 'w ' + days.toString() + 'd ' + hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
            return result_string;
            ]]>
        </Script>
    </CellFormatter>
</Measure>
```

You could of course improve the **JavaScript** further by only showing the relevant duration portions:

```javascript
var result_string = '';
// access Mondrian value
var sec =  value;
var weeks = Math.floor(sec/(60*60*24*7));
var days = Math.floor((sec/(60*60*24)) - (weeks*7));
var hours = Math.floor(sec/(60*60) - (weeks*7*24) - (days*24));
var minutes = Math.floor((sec/60) - (weeks*7*24*60) - (days*24*60) - (hours*60));
var seconds = Math.floor(sec - (weeks*7*24*60*60) - (days*24*60*60) - (hours*60*60) - (minutes*60));
if(weeks !== 0){
	result_string = weeks.toString() + 'w ' + days.toString() + 'd ' + hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
} else if(days !== 0){
	result_string = days.toString() + 'd ' + hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
} else if(hours !== 0){
	result_string = hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
} else if(minutes !== 0){
	result_string = minutes.toString() + 'min ' + seconds.toString() + 'sec';
} else if(seconds !== 0){
	result_string = seconds.toString() + 'sec';
} else {
	// always provide a display value - do not return null
	result_string = '0sec';
}
return result_string;
```

> **Note**: The script has to return a value in any situation - it must not return `null`, otherwise there can be issues with the client tool. E.g. **Analyzer** doesn't display all the results properly if `null` is returned.

> **Important**: When adding the `CellFormatter` make sure that you removed the `formatString` attribute from the `Measure` or `CalculatedMeasure` XML Element, otherwise this attribute will take precedence over the `CellFormatter`.

Amazing isn't it? If I had only known about this feature earlier on! A big thanks to Matt Campbell for pointing out that Mondrian has a **Cell Formatter**.

And finally, you can also define the **Cell Formatter** in your MDX query:

```sql
WITH
MEMBER [Measures].[t2] AS
    '[Measures].[t1] * 2.5'
    , CELL_FORMATTER_SCRIPT_LANGUAGE = "JavaScript"
    , CELL_FORMATTER_SCRIPT = "var result_string = ''; /*your javascript*/ return result_string;"
    , MEMBER_ORDINAL = 33
```

## Global Custom Format Definitions

If you want to use your custom format with several measures or calculated measures, ideally you want to create one global definition for your custom format. In your Mondrian Schema you can define a `UserDefinedFunction` ([Reference](http://mondrian.pentaho.com/api/mondrian/spi/UserDefinedFunction.html)) as child of the `Schema` element (NOT the `Cube` element), which takes care of this. The script has to be adjusted, mainly at the beginning we have to add a few additional lines of code and also slightly change the way the result is returned:

```xml
<UserDefinedFunction name="prettyIntervalFormatter">
    <Script language="JavaScript">
        <![CDATA[
        function getParameterTypes() {
            return new Array(mondrian.olap.type.NumericType());
        }
        function getReturnType(parameterTypes) {
            return new mondrian.olap.type.StringType();
        }
        function execute(evaluator, arguments) {
            var value = arguments[0].evaluateScalar(evaluator);
            var result_string = '';
            // access Mondrian value
            var sec =  value;
            var weeks = Math.floor(sec/(60*60*24*7));
            var days = Math.floor((sec/(60*60*24)) - (weeks*7));
            var hours = Math.floor(sec/(60*60) - (weeks*7*24) - (days*24));
            var minutes = Math.floor((sec/60) - (weeks*7*24*60) - (days*24*60) - (hours*60));
            var seconds = Math.floor(sec - (weeks*7*24*60*60) - (days*24*60*60) - (hours*60*60) - (minutes*60));
            if(weeks !== 0){
                    result_string = weeks.toString() + 'w ' + days.toString() + 'd ' + hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
            } else if(days !== 0){
                    result_string = days.toString() + 'd ' + hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
            } else if(hours !== 0){
                    result_string = hours.toString() + 'h ' + minutes.toString() + 'min ' + seconds.toString() + 'sec';
            } else if(minutes !== 0){
                    result_string = minutes.toString() + 'min ' + seconds.toString() + 'sec';
            } else if(seconds !== 0){
                    result_string = seconds.toString() + 'sec';
            } else {
                    // always provide a display value - do not return null
                    result_string = '0sec';
            }
            return "\"" + result_string + "\"";
        }
        ]]>
    </Script>
</UserDefinedFunction>
```


Now that the global format function is defined, you can use it with **Calculated Measures**, but not directly with **Measures**. **Measures** do lack a property child element: They only have a `formatString` attribute, but you cannot use this one to reference the function:

```xml
<Measure name='Duration Base' column='duration' aggregator='sum' visible='false'/>
<CalculatedMeasure name='Duration' formula='[Measures].[Duration Base]' dimension='Measures' visible='true'>
    <CalculatedMeasureProperty name='FORMAT_STRING' expression='prettyIntervalFormatter([Measures].CurrentMember)'></CalculatedMeasureProperty>
</CalculatedMeasure>
```

> **Very Important**: If you have to apply the function to several measures in this way, make sure that you list the **Measures** first and only then the **Calculated Measures**. You must not mix them, otherwise **Mondrian** will exclude any measures listed after the first calculated measure!

You can use the function in your **MDX** query or even in **Analyzer** or other clients when defining a calculated measure. MDX query example:

```sql
WITH
MEMBER [Measures].[Test] AS
    [Measures].[Sales] / 2
    , FORMAT_STRING = prettyIntervalFormatter([Measures].CurrentMember)
SELECT
    [Event Type].Members ON 0
    , [Measures].[Test] ON 1
FROM [MyCube]
```