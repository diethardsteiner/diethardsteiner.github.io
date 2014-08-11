---
layout: post
title:  "Mondrian semi-additive measures"
summary: This article explains a workaround you can use to implement semi-additive measures in Mondrian
date:   2014-08-12
categories: Mondrian
tags: Pentaho
published: true
---

Currently **Mondrian** doesn't offer a way to define **semi-additive** measures (see as well [this Jira case](http://jira.pentaho.com/browse/MONDRIAN-962)). However, Vladislav Krakhalev proposed a workaround using `OPENINGPERIOD()` or `CLOSINGPERIOD()` **MDX** functions. Thanks a lot! 

So what are **semi-additive measures**? Let's best explain this based on an example:

Imagine you have to prepare a **subscriber base analysis** for a telecom provider. You have the obvious date dimension, tariff plan dimension, etc. 

So you can sum the subscriber base on a daily level, but once you roll up to weekly, monthly, quarterly or yearly level, you have to take the first or last value of this period (whatever the client is happy with really - averages usually don't do the trick). 

First make sure you have proper **time dimension** defined (like the one shown below), so that the rollup works correctly:

```
<Dimension type="TimeDimension" visible="true" highCardinality="false" name="Date">
	<Hierarchy name="Monthly Calendar" visible="true" hasAll="true" allMemberName="Total" allMemberCaption="Total" primaryKey="date_tk">
		<Table name="dim_date" schema="dma_subscriber_base" />
		<Level name="Year" visible="true" column="year4" type="Integer" uniqueMembers="false" levelType="TimeYears" hideMemberIf="Never" />
		<Level name="Quarter" visible="true" column="year_quarter" ordinalColumn="quarter_number" type="String" uniqueMembers="false" levelType="TimeQuarters" hideMemberIf="Never" />
		<Level name="Month" visible="true" column="year_month_number" type="String" uniqueMembers="false" levelType="TimeMonths" hideMemberIf="Never"/>
		<Level name="Date" visible="true" column="date_digits" ordinalColumn="date_tk" type="Date" uniqueMembers="true" levelType="TimeDays" hideMemberIf="Never"/>
	</Hierarchy>
</Dimension>
```

Then use MDX functions [`OPENINGPERIOD()`](http://msdn.microsoft.com/en-us/library/ms145992.aspx) or [`CLOSINGPERIOD()`](http://msdn.microsoft.com/en-us/library/ms145584.aspx). Let's test these functions:

```
WITH
MEMBER [Measures].[Subbase] AS
(
	(OPENINGPERIOD([Date.Monthly Calendar].[Date]), [Measures].[Subscribers])
) 
SELECT
[Measures].[Subbase] On Columns,
Non Empty({[Date.Monthly Calendar].[Date].Members}) On Rows
FROM [Subscriber Base]

WITH
MEMBER [Measures].[Subbase] AS
(
	(OPENINGPERIOD([Date.Monthly Calendar].[Date]), [Measures].[Subscribers])
) 
SELECT
[Measures].[Subbase] On Columns,
Non Empty({[Date.Monthly Calendar].[Month].Members}) On Rows
FROM [Subscriber Base]

WITH
MEMBER [Measures].[Subbase] AS
(
	(OPENINGPERIOD([Date.Monthly Calendar].[Date]), [Measures].[Subscribers])
) 
SELECT
[Measures].[Subbase] On Columns,
Non Empty({[Date.Monthly Calendar].[Quarter].Members}) On Rows
FROM [Subscriber Base]

WITH
MEMBER [Measures].[Subbase] AS
(
	(OPENINGPERIOD([Date.Monthly Calendar].[Date]), [Measures].[Subscribers])
) 
SELECT
[Measures].[Subbase] On Columns,
Non Empty({[Date.Monthly Calendar].[Year].Members}) On Rows
FROM [Subscriber Base]
```

Our test tells us that the logic is working as expected, so the next step is to add a `CalculatedMember` to the OLAP Schema:

```
<CalculatedMember name="Subbase" formatString="#,##0" caption="Subbase" description="Subbase at the first day of the period" formula="(OPENINGPERIOD([Date.Monthly Calendar].[Date]), [Measures].[Subscribers])" dimension="Measures" visible="true"/>
```

This looks like a very simple workaround and should help solving quite a lot of scenarios.
