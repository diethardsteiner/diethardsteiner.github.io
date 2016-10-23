---
layout: post
title: "Rethinking the Snapshot Strategy"
summary: This article discusses a different strategy for creating a snapshot
date: 2016-10-23
categories: Data Modeling
tags: Data Modeling
published: true
--- 

Every now and then I am thinking about [the snapshot strategy](/data-modeling/2015/07/27/Events-and-Snapshot.html) I described about a year ago. I am still not quite happy about it. It's a huge new data generating monster (well it actually creates a massive amount of output data based on the relatively compact input dataset). It sometimes puzzles me that **OLAP** tools don't have a better way to handle snapshots. Or maybe there is a better strategy to do this?!

Just to recap: If you are asked to provide a daily snapshot, your ETL has to generate a row for every day for every little thing that you are trying to keep  track of. This is kind of the consequence of providing this data in a **cube**: You have to have a (fact/snapshot) row available for a particular day in order to join it to any other dimension.

The problem of having to produce these **artificial records** is not going away for now (unless there is better support for this by OLAP tools), so we have to find better ways to generate the snapshot. However, every now and then I ask myself: What if we have to generate a snapshot on a by second grain (as opposed daily), how will we cope with generating so much data? There must be a better approach to do this. This is certainly an extreme situation, however, it pronounces the problem we have with current approach.

So how could we possibly make things a bit easier? What if we tried to implement everything in SQL, so that we can i.e. run it easily on Hive? The main challenge here is actually how to create the **artificial clones**? 

For this example we assume that we are tracking the status of cases. Say we have a case which got `opened` on `2016-02-01` and changed its status to `assigned` on `2016-02-05`. So we have to clone the record from `2016-02-01` 3 times: For `2016-02-02`, `2016-02-03` and `2016-02-04`. The key here is to treat this data as a **Slowly Changing Dimension Type 2**, so assign a `valid_from` and `valid_to` to each event. We store this data in a table called `dim_events`. It's not really necessary to make this table a fully qualified dimension - the idea is just to treat the data in a similar fashion. 

So what about creating the **clones** then? Actually, this can be very easily achieved by joining to the **Date Dimension**. We just have to use the `valid_from` and `valid_to` from our `dim_events` table and retrieve the respective days from the `dim_date` table using `WHERE` clause. Based on the example above, we have one record from `dim_event` being joined to 4 records from `dim_date`, so in the end we will get 4 records returned. We enrich the result by adding a constant of `1` for aggregation purposes (not strictly required). 

I prototyped the solution quickly with PostgreSQL:

```sql
DROP SCHEMA IF EXISTS snapshot_example CASCADE;

CREATE SCHEMA snapshot_example;

CREATE TABLE snapshot_example.dim_date AS
WITH RECURSIVE date_generator(date) AS (
    VALUES (DATE '2016-01-01')
  UNION ALL
    SELECT date+1 FROM date_generator WHERE date < DATE '2017-01-01'
)
SELECT 
  CAST(TO_CHAR(date,'YYYYMMDD') AS INT) AS date_tk
  , date AS the_date 
  , EXTRACT(DAY FROM date) AS the_day
  , EXTRACT(MONTH FROM date) AS the_month
  , EXTRACT(QUARTER FROM date) AS the_quarter
  , EXTRACT(YEAR FROM date) AS the_year
FROM 
date_generator
;

SELECT * FROM snapshot_example.dim_date LIMIT 20;


-- treat events as slowly changing dimension
CREATE TABLE snapshot_example.dim_events
(
  event_tk BIGINT
  , case_id     BIGINT
  , valid_from    INT
  , valid_to      INT
  , event_status  VARCHAR(20)
)
;

INSERT INTO snapshot_example.dim_events VALUES
  (1,2321, 20160201, 20160205, 'open')
  , (2,2321, 20160205, 20160207, 'assigned')
  , (3,2321, 20160207, 20160208, 'closed')
;

-- create snapshot by exploding the events
CREATE TABLE snapshot_example.snapshot AS
SELECT
  case_id
  , event_tk
  , date_tk AS snapshot_date_tk
  , valid_from
  , valid_to
  , event_status
  , 1 AS quantity
FROM snapshot_example.dim_events f
INNER JOIN snapshot_example.dim_date d
ON
  d.date_tk >= f.valid_from
  AND d.date_tk < f.valid_to
;

SELECT * FROM snapshot_example.snapshot LIMIT 20;
```

An the result looks like this (`valid_from` and `valid_to` are only part of the result to facilitate QA and should be removed thereafter):

case_id | event_tk | snapshot_date_tk | valid_from | valid_to | event_status | quantity
----|----|----|----|----|----|----
2321 | 1 | 20160201 | 20160201 | 20160205 | open | 1
2321 | 1 | 20160202 | 20160201 | 20160205 | open | 1
2321 | 1 | 20160203 | 20160201 | 20160205 | open | 1
2321 | 1 | 20160204 | 20160201 | 20160205 | open | 1
2321 | 2 | 20160205 | 20160205 | 20160207 | assigned | 1
2321 | 2 | 20160206 | 20160205 | 20160207 | assigned | 1
2321 | 3 | 20160207 | 20160207 | 20160208 | closed | 1

Keep in mind that with a snapshot you only look at one day at a time, we do not aggregate the final figures across days! The main question that this output answers is the following: "How many cases were in status X on day Y"?

> **Note**: If for some reason you also have to count the days and/or working days that a certain case stayed in a given status, you can just simply add a binary flag (using an `TINYINT` representation of `0` and `1`) to the date dimension and enrich the join of `dim_event` and `dim_date` with these flags, which can then just be aggregated in some form (so perform some additional aggregation across days upfront for the final output ... you could use a windowed function in example):


```sql
SUM(is_working_day) OVER (PARTITION BY ... ORDER BY ...)
```

The final join helps us to **materialise** the view which the **OLAP/Cube** tool requires. 

The output of the query above might be in some cases a bit too granular, but performing an aggregation of top of this is very easy.


