---
layout: post
title:  "Big Data Snapshot and Accumulating Snapshot"
summary: This article explains how to implement an accumulating snapshot on Hadoop
date: 2015-07-27
categories: data-modeling
tags: Kimball
published: true
--- 

In this article we will have a look at various approaches on how to create snapshot tables with varying degrees of flexibility. We will start by taking a brief look at the classic **Kimball** approaches, then discuss a different approach for a Big Data environment and finally take a look at an alternative approach which I came up with. This article is more of a theoretical nature - initially I tried to include some practical examples, but the article was just getting way too long, so I cut it back to the essential points.

# Classic Kimball Approaches

Both the **snapshot** as well as the **accumulating snapshot** rely on a fact table. You can think of them as alternative views on the same data, which allow to answer a different set of questions. 

## Snapshot

A snapshot allows us to easily check at which stage a certain item is on a given day: A classic example is an item being delivered from your online merchant to your doorstep. It's a **predefined process** with **given steps**. A classic **Kimball** style **snapshot** would list the ID of your order, any dimensions, a date or timestamp for each of the steps (e.g. *Package ready to be dispatched*, *Arrived at local distribution centre* etc) and there would be a counter of `1` for each of the steps completed. The table row gets updated as the delivery progresses. This works fine for processes that are more of a static nature.

Here an extremely simplified example: The object with id `20` just entered **step 1** in our process, hence we insert a new record populating the respective fields:


id | dim_a | step_1_started_tk | step_2_started_tk |step_1_count |step_2_count
---|----|----|---|----|----|----|----|----|----|
20 | aaa | 20150602 | `NULL` |  1 | `NULL` |

Two days later when the objects enters **step 2** of our process we have to update the snapshot table with the **timestamp** and **count**:

id | dim_a | step_1_started_tk | step_2_started_tk |step_1_count |step_2_count
---|----|----|---|----|----|----|----|----|----|
20 | aaa | 20150602 | 20150604 |  1 | 1 |

> **Important**: Dimensional values which were originally inserted for one particular record never get updated! So the value of `dim_a` in the above example must not change at any point. The same principles as for the standard fact table apply: No data gets overwritten at any point. It's a write once process (In the case of the snapshot on a cell level). 

If the process is of a more flexible nature, e.g. it's possible to jump one or several steps back and forward (even several times), then pursuing the **Kimball** style snapshot approach might be quite a challenge/impossible.

## Accumulating Snapshot

**Snapshot** tables allow us to answer the question *How many items entered step/stage X on a certain day?*, however, they do not allow us to answer the question *How many items do we have in a given step/stage of our process on a given day?*: This is what **accumulating snapshots** are for.

snapshot_date_tk | dim_a | dim_b | quantity_on_hand_eod
----|-----|-----|-----|----|
20150603 | aaa | bbb | 234

The **quantity** on hand is usually measured at the end of the day. 

**Accumulating Snapshot** saves reporting solutions the computing intensive task of having to calculate for each day how many objects entered and exited a certain stage/step since the very beginning. 

The **ETL** process has less to do as well to create a new record: We only have to look at the previous day's accumulative quantity and then add all the new objects entering the step/stage and subtract all the objects leaving this step/stage. So it is a fairly easy calculation as well. 

# Creating an event table

On a recent project the task was to store any event in a workflow on Hadoop. As most of you will know, **Hadoop HDFS** was not originally designed for updates, hence we had to resort to an alternative strategy - which was suggested by Nelson Sousa: The idea is to **negate** the last event once a new event arrives. The original event has a count of `1` whereas the negating/closing record of the same event has a count of `-1`. When you sum the records, the result will be `0`. 

Another requirement was that we can record any event: The workflow is not necessarily sequential. Items can jump back and forth several steps several times. This means that **Kimball's accumulating snapshot** approach cannot be applied. 

The basic algorithm will create a closing record for each event **once a new one arrives**. The closing record's date will be the same as the one of the next event:

![](/images/events_algorithm.png)

A second ETL process creates the accumulating snapshot, which we will discuss next.

## Creating the Accumulating Snapshot

In order to be able to analyse the amount of items in a certain state on a given day, we have to create an **accumulating snapshot** table. The setup is similar to the one of bank statement: There is a balance and the difference to the previous balance is shown (in form of transactions). Hence there is no need to recalculate everything since the opening of bank account.

> **Filling the gaps**: The important point to consider is that for a daily snapshot we have to create daily records even if there is no measure available for this particular day and the given combination of dimensions. The fillings are highlighted in the below screenshot in blue:

![](/images/events_snapshot.png)

A more sensible approach is to not create **fillers** once the cumulative sum of a given combination of dimensions reaches zero - this will safe you some disk space.

A simple strategy to create these **fillings** in the **ETL** is to calculate the difference in amount of days between the current record and the next record and subtract minus 1. Then you can use this number to clone the current record. Care must be taken to set any measures to `NULL` in the **clones** (apart from the cumulative measures).

## Creating the Cube Definition

Writing the **Mondrian** OLAP Schema for the snapshot and accumulating snapshot tables is quite straight forward. The one noteworthy point is that we should create a derived dimension called **Flow** (based on our `+1` and `-1` counts) in order to better show the in and out movement. Another important point is that the end users have to be educated to only analyse **accumulating snapshot** figures on a daily level, as they will not roll up to a higher level (unless you fancy implementing semi-additive measures, which automatically picks the beginning of the month e.g. if you are on a monthly level - this can be quite performance intensive though).

## Sample Analysis

A very simple report will show the amount of issues added and removed from a certain status on a given day:

![](/images/events_in_and_out.png)

The next report is a bit similar, but we set following filters:

- Show all **Status**es except `Opened`
- Show all statuses for **Flow** `in` only

![](/images/events_daily_status_movement.png)

Our sample dataset is extremely small, but in real world scenarios you can take the output of the above query for just one day and display it in a **Sankey** or **Alluvial Diagram**. Here is an example based around our use case, just with a  bit more data:

![](/images/events_alluvial_diagram.png)

The above diagram was quickly created with [RAW](http://app.raw.densitydesign.org) - a good way to prototype this type of chart. You can later on implement this kind of chart in a **Pentaho CDE** dashboard.

And finally we want to understand how many items there are each day in a given status, which can be answered by our **Accumulating Snapshot** cube:

![](/images/events_daily_snapshot.png)

`2015-03-01` shows `0` items as in **Status** `opened`. The reason for this is that the snapshot represents the state at the end of the day: One item was opened on `2015-03-01` and assigned on the same day, hence our snapshot shows `0` for `opened` and `1` for `assigned`. 

# Creating an Accumulating Snapshot Table

## Basic Strategy - Processing One Day at a time

1. We get the snapshot data of the previous day (so that we have access to the cumulative figure) and increase the snapshot date by one day (so that it has the same snapshot date as the new data). We rename the cumulative sum field to the same name as the measure field in the new dataset (see 2.), so that we can easily aggregate them.
2. We get one day worth of new data and transform it to have exactly the same field structure as the snapshot dataset.  
4. We merge the two datasets in a SQL style `UNION ALL` fashion.
5. Now we just have to aggregate the merged dataset by all dimensional columns and we have the new cumulative sum.
6. Filter out the records which have a zero cumulative sum and discard them (if you want to save storage space).

![](/images/events_diagram_etl_snapshot_one_day.png)

## Basic Strategy - Processing several days at once

The approach below assumes that you do not have any late arriving data.

1. From the **existing accumulative snapshot data**, get the records for the last/most recent date as we have to have access to the cumulative quantity.
2. Get the **new data** (all days).
3. Merge streams in a SQL `UNION ALL` fashion.
4. **Sort** the data in ascending order.
5. Create the **cumulative sum**.
6. From the **new data** we require the **maximum date** (across any combination of dims). 
7. **Create Fillers**: Calculate the required number of clones between the given record and the next one (in the same dimensional category). If there is no record available for that particular dimensional category for the last day within this dataset, use the maximum date from step 5 to calculate the additional clones. Finally, based on the previously derived required clones number, clone the records (hence creating the fillers).
8. Discard the **existing accumulative snapshot data** from step 1.

This is only a very rough outline, there are many more steps required to make this work properly. 

![](/images/events_diagram_etl_snapshot_all.png)

Why do we need the max date of the new data? Because we have to create **filler** records for any combination of dimensions until this date in case there is no measure for them available. E.g. we might have measures for category X until the most recent date but for category Y we might only have data until 2 days earlier. So even though no changes happened for category Y, we still have to create records for it (in the ideal case only if the cumulative sum is not zero).

It is worth noting that if there is a certain category (combination of dims) in the existing snapshot data but no corresponding new data, we have to keep on creating records for them in the snapshot table for each day (create clones). But this is quite often an overkill, as I don't consider it necessary to show records when there is a cumulative sum of 0.

For all categories (combinations of dims) that we create clones for for each day, the cumulative sum will be the same and the standard sum/quantity has to be set to 0. 

> **In a nutshell**: If there are no new measures for a given day, a snapshot table still has to show a record (if that combination of dims showed up in the past).

![](/images/events_snapshot_cases.png)

The example above (shown in the screenshot) illustrates some of the considerations we have to take:

- **Gaps in data** (rows with green font)
	- No data between 31 May and Jun 2 (rows with yellow background colour)
	- No data between 3rd and 6th June (most recent date in the example) (rows with red background colour): For other categories we have data up to the 7th June, so we have to create an extra record for this category / combination of dimensions.

### Complications When Adding Age (in Days or Working Days)

On one of the project I was working the requirement was to be able to analyse how many working days a Jira issue was in a certain state. 

issue_id | state | datetime | event_count  
 ------	| ------	| ------	| ------	|  
288 | Opened | 2015-09-01 12:31:00 | 1  
288 | Opened | 2015-09-03 10:11:00 | -1  

Adding the age in days or working days adds further complication to creating a snapshot, as we will see shortly:

issue_id | state | datetime | event_count  | age (days) |
 ------	| ------	| ------	| ------	|  ----- |
288 | Opened | 2015-09-01 12:31:00 | 1  | 1
288 | Opened | 2015-09-03 10:11:00 | -1  | 3  

In our snapshot ETL, in memory, we would expand this dataset to the following:

issue_id | state | datetime | event_count  | age (days) |
 ------	| ------	| ------	| ------	|  ----- |
288 | Opened | 2015-09-01 12:31:00 | 1  | 1  
288 | Opened	| 2015-09-02 00:00:00 | 0 | 2
288 | Opened | 2015-09-03 10:11:00 | -1  | 3  

Now the problem is that our event count of `+1` and `-1` are not representative any more, because we introduced a deeper granularity by adding the age in days. To correct this, we would have to create a dataset like this:

issue_id | state | datetime | age (days) | event_count  |
 ------	| ------	| ------	| ------	|  ----- |
288 | Opened | 2015-09-01 12:31:00 | 1  | 1
288 | Opened | 2015-09-02 00:00:00 | 1  | -1
288 | Opened	| 2015-09-02 00:00:00 | 2 | 1
288 | Opened	| 2015-09-03 10:11:00 | 2 | -1
288 | Opened | 2015-09-03 10:11:00 | 3 | 1  
288 | Opened | 2015-09-03 10:11:00 | 3 | -1  
288 | Assigned | 2015-09-03 10:11:00 | 1 | 1 

Now the key is made up of `issue_id`, `state`, `datetime` and `age (days)`, hence the Jira issue can enter and exit this detailed *state*.

If the requirement is to use **working days**, the strategy is still the same, however, one point worth noting is, that the some key values can exist across several days, e.g. a weekend:

issue_id | state | datetime | age (working days) | event_count  |
 ------	| ------	| ------	| ------	|  ----- |
288 | InProgress | 2015-09-04 13:22:00 | 1  | 1
288 | InProgress | 2015-09-05 00:00:00 | 1  | 0
288 | InProgress	| 2015-09-06 00:00:00 | 1 | 0  
288 | InProgress	| 2015-09-07 00:00:00 | 1 | -1
288 | InProgress	| 2015-09-07 00:00:00 | 2 | 1  
288 | InProgress | 2015-09-08 00:00:00 | 2 | -1 
288 | InProgress | 2015-09-08 10:11:00 | 3 | 1  
288 | InProgress | 2015-09-08 10:11:00 | 3 | -1  
288 | UnderReview | 2015-09-08 10:11:00 | 1 | 1  

So this is getting fairly complex now. We have to find a better strategy that works with our initial fact events table data:

How about just flagging the rows if they are **on hand** instead of creating the enter-exit (+1 -> -1) combos and using a standard **Sum** instead of an **Cumulative Sum** aggregation? Would this not be easier?

issue_id | state | datetime | event_count  | age (days) | quantity on hand 
 ------	| ------	| ------	| ------	|  ----- | ----
288 | Opened | 2015-09-01 12:31:00 | 1  | 1  | 1
288 | Opened	| 2015-09-02 00:00:00 | 0 | 2 | 1
288 | Opened | 2015-09-03 10:11:00 | -1  | 3 | 0  

In this case we only have to create one filler record a day for each unique key combination. One important point to consider is that we have to ignore intra-day state changes, e.g.:

issue_id | state | datetime | event_count  | age (days) | quantity on hand 
 ------	| ------	| ------	| ------	|  ----- | ----
277 | Opened | 2015-09-01 12:31:00 | 1  | 1  | 0  
277 | Opened | 2015-09-01 13:33:00 | -1  | 1  | 0  
277 | Assigned | 2015-09-01 13:33:00 | 1  | 1  | 1  
277 | Assigned | 2015-09-02 11:23:00 | -1  | 2  | 0  
277 | InProgress | 2015-09-02 11:23:00 | 1  | 1  | 1

> **Note**: As Jira issue 277 gets opened and then assigned on the same day, the entering and exit records of state `Opened` have both `quantity on hand` set to `0`.

In most cases we do not want to keep the same granularity in the snapshot table as in the fact events table. Usually we would at least get rid of the `issue_id`.

There are some rules to consider when aggregating the data:

- The fact events table needs an additional `age_in_days` fields so that we can accurately detect intra-day state changes (and hence exclude them from the snapshot). `age_in_days` has to be set to `0` for these records. For intra-day state changes, the `-1` has to be respected in the negating record. Also, any age in days values have to be set to `0`.
- The **negating** (exiting) records (`-1`) have to be treated as `0`. We only want to keep these records in order to create the **fillers** in the ETL (for non-intra-day status changes). Once the fillers are created, we discard these records.
- When we clone the **entering** (`+1`) row we keep the event count figure to obtain `quantity_on_hand` instead of replacing it by `0` or `NULL`.

![](/images/events_snapshot_strategy_age_on_hand.png)

The values in the white cells in the `Sum (Corrected)` column in the above screenshot would be returned by the input query shown below. The green cells are the **fillers** generated by the ETL. 

An input query for the **Snapshot ETL** could look like this:

```sql
SELECT
	status
	, "user"
	, age_working_days
	, cum_age_working_days
	, CAST(CAST(date_tk AS CHAR(8)) AS DATE) AS date
	, 1 AS is_new_data
	, SUM(
		CASE
			-- we want to ignore intra day ins and outs 
			WHEN quantity = -1 AND age_days = 0 THEN - 1
			WHEN quantity = -1 THEN 0 
			ELSE 1
		END
	) AS quantity_on_hand
	, CAST('${VAR_NEW_DATA_MAX_DATE}' AS DATE) AS new_data_max_date
FROM events_dma.fact_events
WHERE
	date_tk > ${VAR_SNAPSHOT_MAX_DATE}
GROUP BY 1,2,3,4,5,6
ORDER BY 1,2,3,4,5,6
``` 

## Handling Late Arriving Data

In the case of Hadoop, we cannot just update existing records in case we have late arriving data (and usually it is not a good idea to update records in a any case unless it is a snapshot).

![](/images/events_snapshot_late_arriving_records.png)

In the example shown above, we receive a record on the 8th of June for the 1st of Jun. Snapshot data is already available until the 7th of Jun. As we cannot update the existing records, we just insert new records (highlighted in yellow) for evert day since the 1st of Jun to correct the existing records. This approach was suggested by Nelson Sousa.

It is worth noting that the Mondrian Cube Definition should easily handle this scenario without requiring any changes, however, the ETL might need some adjustment (e.g. when accumulating snapshot figures get sourced, they have to get aggregated). 

# An alternative approach

The previously discussed approach was quite difficult to implement - difficult mainly because we had to show the age of a given item in working days (in a given state/event on a given day). 

Here is an alternative approach I came up with, which is good for a quick prototype. I wouldn't use it for production as it generates way too much data - unless you are not worried about this. 

## Outline of the Strategy

![](/images/events_my_alt_approach.png)

You will notice that we keep on creating negating records and the pairs have `+1` and `-1` counters (`quantity_moving`). The difference to the other approaches is that the ETL creates **fillers** for the days in between the entering and exit record. Notable addition is the `quantity_on_hand` field, which is set to `1` for every day we actually have this object in the given state/event. Now just put a Mondrian Schema on top, et voilá: you have everything! Mondrian will sum up the `quantity_on_hand` for a given day, which will give you the accumulative snapshot figures and at the same time you also have access to figures about items entering and exiting a given status (`quantity_moving`).

Also notice, that for this a particular `issue_id` we had a raw record for 2015-06-06 and 2015-06-15, but nothing thereafter. There were other records in the source data available last time we imported, so we had to take the maximum date from the source data and generate rows for this issue id up to that date. This will make sure that `quantity_on_hand` figures are correct for all dates.

Another point to consider is that if an issue id enters and exists a certain state within the same day, we do not want to consider it for `quantity_on_hand`. The snapshot is taken e.g. at the end of the day, hence intra-day changes are not represented. To detect intra-day changes, the ETL has to generate an `age in days` field based on which the `quantity_on_hands` figure can be calculated correctly.

## Pros and Cons

**Drawbacks** of the strategy:

- **Data Explosion**: We create records for every day for every item in a given state (except closed).
- **Snapshot**: Once the cumulative sum for a certain combination of dimension reaches 0, no fillers are created (as a normal snapshot would have) (it would still have records for days where there are no facts to report on), but on the other hand side, you do not create empty facts in fact tables either. I do not think this is much of a concern for an end user. The end user will only see a blank report if there are no facts at all or just see data for dimensions where there is a cumulitive sum. However, if you need these fillers, you can create them in an additional ETL process.

**Pros**:

- It's very easy to understand - I doubt it get's any simpler than this.
- It's very easy to check the results as all calculated/derived fields are generated on the most granular level.
- There is only one output table which provides all required results.
- It should be easy to run this in parallel (partitioning by `issue_id` e.g.).

**OLAP SCHEMA**: When analysing **Quantity On Hand** exclude the status **closed**. Status **closed** is the only status that items never leave. So there is not much use of having a cumulative sum for this status and hence this strategy also does set `quantity_on_hand_eod` to 0 for status **closed** records. In general, we are not interested in having a cumulative sum for closed items since the beginning of the service. We are more likely to ask **How many issues were closed this week?**, which can be answered by using `quantity_moving`. For all other statuses (which have an in and out movement) we require `quantity_on_hand_eod` in order to answer the question **How many issues are in status X on a given day?**. Another advantage of not generating a cumulative sum for status **closed** is that we do not have to create a huge amount of clones/fillers. We only have to create fillers for all other statuses (which have an in and out movement).

![](/images/events_my_alt_approach_cube.png)