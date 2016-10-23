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
