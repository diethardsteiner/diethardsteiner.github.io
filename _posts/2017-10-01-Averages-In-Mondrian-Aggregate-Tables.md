
---
layout: post
title: "Mondrian: Averages in Aggregate Tables"
summary: This article explains how to make averages work with Mondrian aggregated tables
date: 2017-10-01
categories: Mondrian
tags: Mondrian
published: false
--- 

This article explains how to make averages work with **Mondrian aggregated tables**. To understand this article, basic knowledge on Mondrian aggregate tables is required.

## Average Methods for Aggregate Tables

"Mondrian can compute averages just fine from a `SUM` and `FACT_COUNT` column - we never need `AVG` columns at all. Calculating `SUM` from `AVG` seems backwards to me, and may result in rounding errors." (Julian Hyde, 2007) 

While this statement still holds true for a lot of use cases, over the years there have been refinements to the original approach.

Following methods are available:

### Average From Sum

`AvgFromSum`: Mondrian will use this formula as part of the SQL query: 

```
sum(measure_sum) / sum(fact_count)
``` 

To populate the aggregate table, we can use a SQL query similar to this one: 

```sql
SELECT
  ...
  , SUM(`measure`) AS `measure_sum`,
  , COUNT(*) AS fact_count
FROM ..
```

### Average From Average

`AvgFromAvg`: Mondrian will use this formula as part of the SQL query: 

```
sum('measure_avg' * 'fact_count') / sum('fact_count')
```

### Aggregate Measure Fact Count

Only **version 7.1** of the **Pentaho Server** introduced a feature to support averages in aggregate tables in a better way. Prior to this a workaround had to be used, where a **calculated measure** had to be defined dividing the sum of the measure by a non-null count of the same measure.


## Solutions

### Prior to Pentaho Server v7.1

#### Works in all scenarios

Gain total control by defining a calculated measure for averages:

Imagine you have a fact table with a `total_sales` column.

When defining the aggregate table explicitly in the Mondrian Schema:

1. To the fact table add: `total_sales_fact_count`
2. To the cube add an invisible measure `[Total Sales Fact Count]` with the aggregation type `SUM`. Then add a calculated member `[Avg Sales]` with the formula `[Measures].[Total Sales] / [Measures].[Total Sales Fact Count]`.
3. The aggregation table remains the same, so add:

  - `total_sales`
  - `total_sales_fact_count`

4. The SQL to populate the aggregate table should sum up `total_sales` and `total_sales_fact_count` (using the these columns from the fact table).
5. In the cube, add the aggregate table definition. Define both `total_sales` and `total_sales_fact_count` as `AggMeasure`.

#### Non Explicitly Defined Aggregate Tables

If you are just after the simple average, you can just skip defining the aggregate table explicitly and create a table following Mondrian's naming conventions, something like this:

```sql
CREATE TABLE test.agg_time_activity_fact_sales AS
SELECT
  time_tk
  , department_tk
  , SUM(total_sales) AS total_sales
  , COUNT(*) AS fact_count
FROM test.fact_sales
GROUP BY 1,2
;
```

And when we query the cube, Mondrian will create a query similar to this one:

```sql
select department_name as c0, sum(total_sales) / sum(fact_count) as m0 from dim_x, agg_time_activity_fact_sales where department_tk = department_tk and department_name = 'a' group by department_name
```


### As of Pentaho Server v7.1

**Pentaho Server** v7.1 ships with a new **Mondrian** version (3.14.0.0-12) that supports a new feature called `AggMeasureFactCount`, which enables you to specify an additional fact count column per measure in the aggregate table definition. The count represents the **count of non-null values**. It is not required any more to specify a calculated measure for calculating the average (hence you also don't need the additional count column in the fact table).

> `AggMeasureFactCount`: The number of non-null fact table rows associated with a specific fact column. Mondrian can use this information when calculating averages from aggregate table values.

> **Important**: `AggMeasureFactCount` has to be specified after the `AggFactCount` and before `AggForeignKey` element.

Example shown [here](https://github.com/pentaho/mondrian/blob/e6e1f7695f3a27d08adfed549b657ba7d184fcb3/mondrian/src/it/java/mondrian/rolap/aggmatcher/AggMeasureFactCountTest.java)

```xml
<AggName name="agg_c_6_fact_csv_2016">
  <AggFactCount column="fact_count"/>
  <AggMeasureFactCount column="store_sales_fact_count" factColumn="store_sales" />
  <AggMeasureFactCount column="store_cost_fact_count" factColumn="store_cost" />
  <AggMeasureFactCount column="unit_sales_fact_count" factColumn="unit_sales" />
  <AggMeasure name="[Measures].[Unit Sales]" column="UNIT_SALES" />
  <AggMeasure name="[Measures].[Store Cost]" column="STORE_COST" />
  <AggMeasure name="[Measures].[Store Sales]" column="STORE_SALES" />
  <AggLevel name="[Time].[Year]" column="the_year" />
  <AggLevel name="[Time].[Quarter]" column="quarter" />
  <AggLevel name="[Time].[Month]" column="month_of_year" />
</AggName>
```

**A full example**:

The main measures are defined with aggregation type average:

```xml
...
<Measure name="Total Sales" column="total_sales" visible="true" aggregator="avg" formatString="#,##0; -#,##0"/>
<Measure name="Total Quantity" column="total_quantity" visible="true" aggregator="avg" formatString="#,##0; -#,##0"/>
...
```

The **aggregate table** definition looks like this (note the addition on **measure fact count columns**):

```xml
...
<AggName name="agg_fact_sales_by_department">
  <AggFactCount column="fact_count"/>
  <AggMeasureFactCount column="total_sales_fact_count" factColumn="total_sales" />
  <AggMeasureFactCount column="total_quantity_fact_count" factColumn="total_quantity" />
  <AggForeignKey factColumn="department_id" aggColumn="department_id" />
  <AggMeasure column="total_sales" name="[Measures].[Total Sales]"></AggMeasure>
  <AggMeasure column="total_quantity" name="[Measures].[Total Quantity]"></AggMeasure>
  ...
```

Populating the aggregate table:

```sql
INSERT INTO test.agg_fact_sales_by_department
SELECT
  department_id
  , COUNT(total_sales) AS total_sales_fact_count
  , COUNT(total_quantity) AS total_quantity_fact_count
  , SUM(total_sales) AS total_sales
  , SUM(total_quantity) AS total_quantity
  , COUNT(*) AS fact_count
FROM test.fact_sales
GROUP BY 1
;
```

When you query the cube, the `pentaho.log` should show something like this then:

```
2017-09-30 22:39:36,515 DEBUG [mondrian.rolap.RolapUtil] Segment.load: done executing sql [select sum("agg_fact_sales_by_department"."total_sales" * "agg_fact_sales_by_department"."total_sales_fact_count") / sum("agg_fact_sales_by_department"."total_sales_fact_count") as "m0" from "test"."agg_fact_sales_by_department" as "agg_fact_sales_by_department"], exec+fetch 35 ms, 1 rows, ex=11, close=11, open=[]
```

This confirms that the new aggregate table is in fact used by **Mondrian**.

I just want to highlight again how the average is calculated by Mondrian internally:

```sql
SELECT 
  SUM(total_sales * total_sales_fact_count) / SUM(total_sales_fact_count)
FROM agg_fact_sales_by_department
```

The **important thing** here is that `total_sales` and `total_sales_fact_count` get first multiplied on a row level and then summed up, the result of which then gets divided by the sum of `total_sales_fact_count`. Let's illustrate this with an example:

| total_sales | total_sales_fact_count | row level multiplication | final average result
|-----|-----|---------|------
| 2000 | 100 | 200000 | 
| 3000 | 150 | 450000 | 
| Total | 250 | 650000 | 2600


## Related Jira Cases

- [Incorrect avg calculation in aggregates](http://jira.pentaho.com/browse/MONDRIAN-347)
- [Wrong results when aggregate tables used to calculate avg value which rolls up one or more NULLs](http://jira.pentaho.com/browse/MONDRIAN-2398)

Problem fixed in 7.1: [Github Merge Request](https://github.com/pentaho/mondrian/pull/763)

## Some Background Info

This [Jira case](http://jira.pentaho.com/browse/MONDRIAN-347) from 2007 provides us quite some interesting information: Apart from telling us that Mondrian can handle aggregate columns differently if they follow the naming pattern outlined in the Mondrian documentation (e.g. `measure_sum`, `measure_avg`, you can use this approach when you don't want to explicitly define the aggregate in the Mondrian schema), we also learn that there are at least two aggregation methods for averages:

"Mondrian can compute averages just fine from a `SUM` and `FACT_COUNT` column - we never need AVG columns at all. Calculating SUM from AVG seems backwards to me, and may result in rounding errors." (Julian Hyde)


- `AvgFromSum`: `sum(measure_sum) / sum(fact_count)`
- `AvgFromAvg`, which uses `sum('measure_avg' * 'fact_count') / sum('fact_count')`

A problem highlighted further down this Jira case is the handling of `NULL` values:

"Similar issue if some of the measures are NULL"

```sql
avg(1,2,null,3,4) = 2.5
```

but

```sql
sum(1,2,null,3,4) / sum(1,1,1,1,1) = 2.0
```


"As `fact_count` will still be 1 (or other positive value) if other measures in data table row is not null."

[MONDRIAN-2398](http://jira.pentaho.com/browse/MONDRIAN-2398) goes a bit more into detail on this subject: 

""
When calculating an average from a summed rollup in the aggregate table, Mondrian will compute

```sql
sum(factvalue) / fact_count
```

I.e. the summed fact value over the count of the fact rows in that rollup. Unfortunately if any of the individual fact values is equal to NULL this calculation will differ from a SQL avg, which excludes NULLs.

When this was discussed on the Mondrian dev group, the suggestion for a fix was to include new fact_count columns in the aggregate table that are specific to the measure, such that we have a count of non-NULLs to use for the avg calc.
""

[No way to use an AvgFromSum calculation with explicitly defined aggregate tables](http://jira.pentaho.com/browse/MONDRIAN-2399): Now this one is quite interesting ...

""
Using an explicitly defined aggregate table which includes an average measure (i.e. defined in the Mondrian schema), there is no way to get the calculation to use `AvgFromSum`. It will attempt to rollup with `AvgFromAvg`, which assumes the rolled up avg value is captured in the aggregate table.That method can result in rounding errors and is not always desired.

Ideally there would be some way to specify the exact rollup method used when averages are involved: `AvgFromSum`, `AvgFromAvg`, `SumFromAvg`
""

OPEN QUESTION: So how do you select the rollup method? There isn't anything in the pull request indicating this???

Note: The problem seems to only ever have existed with explicitly defined aggregate tables.
