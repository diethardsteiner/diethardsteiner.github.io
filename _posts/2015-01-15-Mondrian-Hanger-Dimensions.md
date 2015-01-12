---
layout: post
title:  "Mondrian 4: Hanger Dimensions (Actual VS Budget and other Scenarios)"
summary: Hanger dimensions in Mondrian 4 open up a new world of possibilities. Before we dive into this topic, we will first have a look at calculated members and then discuss a few use cases for hanger dimensions.
date:   2015-01-15
categories: mondrian
tags: OLAP, Mondrian, MDX
published: true
---

Hanger dimensions in **Mondrian 4** open up a new world of possibilities. Before we dive into this topic, we will first have a look at calculated members and then discuss a few use cases for hanger dimensions.

## Calculated Members

First we have to understand, what exactly **Calculated Members** are: As the name indicates, calculates members will allow you to calculate new values based on already defined ones. Say you have the *amount of calls* and *amount of revenue* in the cube, you could create a calculated measure called *Average Revenue By Call*:

```sql
WITH MEMBER [Measures].[Average Revenue By Call] AS
	 [Measures].[Revenue] / [Measures].[Calls]
SELECT
	[Measures].[Average Revenue By Call] ON 0
	, [Date].[Monthly Calendar].[Month].Members ON 1
FROM [MyCube]
```

This effectively creates a new measure on-the-fly as well: `[Measures].[Average Revenue By Call]`.

Month | Average Revenue By Call
------|----
Jan | 0.25
Feb | 0.36
Mar | 0.21

## From Sets to Calculated Members

You can also create a set as well (combining values for various members of a given dimension). Let's start in a simple fashion using the `TopCount()` function:

```sql
SELECT
    {[Measures].[Sales Actual]} ON 0
    , TopCount(
        [Date].[Monthly Calendar].[Day].Members
        , 3
        , [Measures].CurrentMember
    )  ON 1
FROM [Finance]
```

> **Note**: Sets cannot be assigned to a dimension, only members! Just to see that this doesn't work, I'll demonstrate it:

```sql
WITH SET [Date].[Monthly Calendar].[Top 3 Days By Month] AS
    TopCount(
        [Date].[Monthly Calendar].[Day].Members
        , 3
        , [Measures].CurrentMember
    )
SELECT
    {[Measures].[Sales Actual]} ON 0
    , [Date].[Monthly Calendar].[Top 3 Days By Month]  ON 1
FROM [Finance]
```

We get following error message:

```bash
MondrianException: Mondrian Error:Internal error: assert failed: set names must not be compound
```

Solution: Do not assign to a specific dimension:

```sql
WITH SET [Top 3 Days] AS
    TopCount(
        [Date].[Monthly Calendar].[Day].Members
        , 3
        , [Measures].CurrentMember
    )
SELECT
    {[Measures].[Sales Actual]} ON 0
    , [Top 3 Days By Month]  ON 1
FROM [Finance]
```

If we want to have the sum of the top 3 days then we can create a new calculated member and assign this one to a particular hierarchy:

```sql
WITH SET [Top 3 Days] AS
    TopCount(
        [Date].[Monthly Calendar].[Day].Members
        , 3
        , [Measures].CurrentMember
    )
MEMBER [Date].[Monthly Calendar].[Sum Top 3 Days] AS
    AGGREGATE([Top 3 Days])
SELECT
    {[Measures].[Sales Actual]} ON 0
    , {
        [Date].[Monthly Calendar]
        , [Date].[Monthly Calendar].[Sum Top 3 Days]
    } ON 1
FROM [Finance]
``` 

(All) | Sales Actual
------|-----
All Monthly Calendars | 487,238.37
Sum Top 3 Days By Month | 361,378.34


Why is *Sum Top 3 Days* a member and not a set? The answer is quite simple really: We only get one value returned for it.

## Hanger Dimension: Intro

Hanger dimensions do not have any link to any other tables, they are just "hanging" in "there" on their own. The members of a hanger dimension are based on on calculated member(s).


## Hanger Dimension: Actual VS Budget

For all the following examples we will use an extremely simple dataset. All files can be downloaded from [here](https://www.dropbox.com/sh/wuef2a208dz7gv2/AACLU2n_tc_2oyggGzDsjhqua?dl=0). Execute the SQL file first and then upload the **Mondrian 4** schema to your biserver (making sure your biserver supports Mondrian 4).

Let's see how we can create a dedicated dimension for an actual vs budget analysis.

The `dimension` element defined in the **Mondrian** schema has a `name` attribute, which, as you would imagine, specifies the name of your dimension (e.g. *Actual VS Budget*). To indicate that this dimension should be a **hanger dimension**, add the attribute `hanger` and set it to `true`. 

```xml
<Dimension name="Actual VS Budget" hanger="true">
```

Within the `dimension` element you can define an `attributes` element, and within this one several `attribute` elements, which have a `name` attribute as well (e.g. *Type*). The attribute element will be automatically turned into a hierarchy. So far we have: `[Actual VS Budget].[Type]`: 

```xml
<Dimension name="Actual VS Budget" hanger="true">
    <Attributes>
        <Attribute name="Type"/>
    </Attributes>
</Dimension> 
```

Next we have to define the members:

This can be achieved via a **Calculated Measure**, which has as well a `name` attribute (e.g. *Actual*). So our full qualitified member is `[Actual VS Budget].[Type].[Actual]`.

> **Note**: Normally dimension member values are based on values retrieved from a data source. However, in the case of calculated measures (and hence hanger dimensions as well), you define these values inline. This is an important concept to understand.

Note that our hanger dimension can have more than one hierarchy (attribute) and more than one member (calculated member). So apart from *Actual* we can also define a *Budget* member. 

Let's define the **Calculated Members**:

```xml
<CalculatedMember hierarchy="[Actual VS Budget].[Type]" name="Actual">
    <Formula>
        Aggregate([Measures].[Sales Actual])
    </Formula>
</CalculatedMember>
<CalculatedMember hierarchy="[Actual VS Budget].[Type]" name="Budget">
    <Formula>
        Aggregate([Measures].[Sales Budget])
    </Formula>
</CalculatedMember>
```

Note how the `hierarchy` attribute of the **Calculated Member** element ties back to the hanger dimension and attribute we defined earlier on: `[Actual VS Budget].[Type]`.

Let's check if this is correct:

```sql
SELECT
	NON EMPTY {[Measures].[Sales Actual],[Measures].[Sales Budget]} ON COLUMNS
	, NON EMPTY {[Actual VS Budget].[Type].[Actual], [Actual VS Budget].[Type].[Budget]} ON ROWS
FROM [Finance]
```

(All) | Sales Actual | Sales Budget
------|--------|---
Actual | 487,238.37 | 487,238.37
Budget | 237,094.00 | 237,094.00

While the aggregation is correct for *Actual* and *Budget*, the measures are not respected. We certainly want to have just one column named *Sales*, as right now the displayed table doesn't make any sense at all. Hence let's add another **calculated measure**. Let's test this first with an MDX query:

```sql
WITH MEMBER [Measures].[Sales] AS
IIF( 
    --[Actual VS Budget].[Type].[Budget].currentmember.level.ordinal > 0  -- works as well
    [Actual VS Budget].[Type].CurrentMember IS [Actual VS Budget].[Type].[Budget]
    , [Measures].[Sales Budget]
    , [Measures].[Sales Actual]
)
SELECT
NON EMPTY {[Measures].[Sales]} ON COLUMNS,
NON EMPTY {[Actual VS Budget].[Type].[Actual], [Actual VS Budget].[Type].[Budget]} ON ROWS
FROM [Finance]
```

(All) | Sales
------|------
Actual | 487,238.37
Budget | 237,094.00

As the result looks as expected, we can alter the actual and budget **calculated members** to:

```xml
<CalculatedMember hierarchy="[Actual VS Budget].[Type]" name="Actual">
    <Formula>
        Aggregate([Measures].CurrentMember)
    </Formula>
</CalculatedMember>
<CalculatedMember hierarchy="[Actual VS Budget].[Type]" name="Budget">
    <Formula>
        Aggregate([Measures].CurrentMember)
    </Formula>
</CalculatedMember>
```

... and we can add this calculation to the Mondrian schema:

```xml
<CalculatedMember hierarchy="[Measures].[Measures]" name="Sales" formatString="#,###.00">
    <Formula>
        <![CDATA[
            IIF( 
                [Actual VS Budget].[Type].CurrentMember IS [Actual VS Budget].[Type].[Budget]
                , [Measures].[Sales Budget]
                , [Measures].[Sales Actual]
            )
        ]]>
    </Formula>
</CalculatedMember>	
```

Ideally you might also want to set the visibility of the **Sales Actual** and **Sales Budget** Measures to `false`.

Upload the **Mondrian** schema to your BA/BI Server and test it with following query:

```sql
SELECT
	NON EMPTY {[Measures].[Sales]} ON COLUMNS
	, NON EMPTY {[Actual VS Budget].[Type].[Actual], [Actual VS Budget].[Type].[Budget]} ON ROWS
FROM [Finance]
```

(All) | Sales
------|-----
Actual | 487,238.37
Budget | 237,094.00

> **Note**: Saiku doesn't currently seem to support hanger dimensions via drang and drop. Write the MDX query instead.

## Hanger Dimension: Top X

Top X queries are always very popular. First lets list the 3 most succesful days sales wise:

```sql
SELECT
    {[Measures].[Sales Actual]} ON 0
    , TopCount(
        [Date].[Monthly Calendar].[Day].Members
        , 3
        , [Measures].CurrentMember
    )  ON 1
FROM [Finance]
```

This returns following dataset:

Day | Sales Actual
---|---
2 | 284,212.34
7 | 39,342.11
4 | 37,823.89

Ok, so this works just fine. Let's add this in a slightly amended form to our **Mondrian** schema:

Add this hanger dimension:

```xml
<Dimension name="Top" hanger="true">
    <Attributes>
        <Attribute name="Type"/>
    </Attributes>
</Dimension>
```

Add this **calculated member**:

```xml
<CalculatedMember hierarchy="[Top].[Type]" name="Sum Top 3 Days">
    <Formula>
        Aggregate(
            TopCount(
                [Date].[Monthly Calendar].[Day].Members
                , 3
                , [Measures].CurrentMember
            )
        )
    </Formula>
</CalculatedMember>
```

Note that here we added the `Aggregate()` function to sum up the measures of this set.

Let's test everything:

```sql
SELECT
    NON EMPTY {[Top].[Type].[Sum Top 3 Days]} ON COLUMNS
    , NON EMPTY 
        {[Date].[Monthly Calendar].[Month].Members}
        * {[Measures].[Sales Actual]} 
    ON ROWS
FROM [Finance]
```

Month | MeasuresLevel | Sum Top 3 Days
---|----|---
1 | Sales Actual | 361,378.34

## Hanger Dimension: Various Rates

Sometimes you might want to show different strategies applied to your data, in example 10%, 20% etc increase of something. And ideally you'd like to compare all these strategies. The only way to do this is to use a hanger dimension.

Add this **hanger dimension**:

```xml
<Dimension name="Rate" hanger="true">
    <Attributes>
        <Attribute name="Type"/>
    </Attributes>
</Dimension> 
```

Add these **calculated measures**:

```xml
<CalculatedMember hierarchy="[Rate].[Type]" name="0%">
    <Formula>
        Aggregate([Measures].CurrentMember) * 1
    </Formula>
</CalculatedMember>
<CalculatedMember hierarchy="[Rate].[Type]" name="10%">
    <Formula>
        Aggregate([Measures].CurrentMember) * 1.1
    </Formula>
</CalculatedMember>
<CalculatedMember hierarchy="[Rate].[Type]" name="20%">
    <Formula>
        Aggregate([Measures].CurrentMember) * 1.2
    </Formula>
</CalculatedMember>
```

> **Note**: Using `[Measures].CurrentMember` in the calculated member instead of specifying a specific measure ( e.g. `[Measure].[Sales Actual]` ) will make the forumla work across all the measures defined in the cube.

Upload your **Mondrian** schema to the BA/BI server.

Let's test this:

```sql
SELECT
    NON EMPTY {[Measures].[Sales Actual]} ON COLUMNS
    , NON EMPTY {[Rate].[Type].[0%], [Rate].[Type].[10%], [Rate].[Type].[20%]} ON ROWS
FROM [Finance]
```

(All) | Sales Actual
----|------
0% | 487,238.37
10% | 535,962.21
20% | 584,686.04

This is not a very useful example, but you can image the verious cases such a comparison would be suitable for.

Sources:
- [Mondrian in Action](http://www.manning.com/back/)
