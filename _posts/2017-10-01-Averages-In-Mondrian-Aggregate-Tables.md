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

To populate an **average column** in an **aggregate table**, many might be inclined to apply the **SQL** `AVG()` function on the base measure. However, this is not what **Mondrian** expects: 

> **Mondrian Definition: Averages in Aggregate Tables**: This is how Mondrian internally calculates the average for aggregates: 

```
SUM(measure * measure_count) / SUM(measure_count)
```

Only **version 7.1** of the **Pentaho Server** introduced a feature to support averages in aggregate tables fully. Prior to this a workaround had to be used, where a **calculated measure** had to be defined dividing the sum of the measure by a non-null count of the same measure.

Related Jira Cases:

- [Incorrect avg calculation in aggregates](http://jira.pentaho.com/browse/MONDRIAN-347)
- [Wrong results when aggregate tables used to calculate avg value which rolls up one or more NULLs](http://jira.pentaho.com/browse/MONDRIAN-2398)

Problem fixed in 7.1: [Github Merge Request](https://github.com/pentaho/mondrian/pull/763)


Solution for versions **prior v7.1**:

- In your **fact table**, keep the base measure column (e.g. `sales`, ideally rename this column to `sum_sales`) and add a new column (`count_sales`) which has a count of the non-null values.
- In the **aggregate tables** create the same two columns.
- Create a **calculated measure** applying the above mentioned formula using these two columns

As of **v7.1**:

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

| `total_sales` | `total_sales_fact_count` | row level multiplication | final average result
|-----|-----|---------|------
| 2000 | 100 | 200000 | 
| 3000 | 150 | 450000 | 
| Total | 250 | 650000 | 2600

If you want to implement this for BA Server prior to v7.1:

1. To the fact table add: `total_sales_fact_count`
2. To the cube add an invisible measure `[Total Sales Fact Count]` with the aggregation type `SUM`. Then add a calculated member `[Avg Sales]` with the formula `[Measures].[Total Sales] / [Measures].[Total Sales Fact Count]`.

3. The aggregation table remains the same, so add:

  - `total_sales`
  - `total_sales_fact_count`

4. The SQL to populate the aggregate table should sum up `total_sales` and `total_sales_fact_count` (using the these columns from the fact table).

5. In the cube, add the aggregate table definition. Define both `total_sales` and `total_sales_fact_count` as `AggMeasure`.