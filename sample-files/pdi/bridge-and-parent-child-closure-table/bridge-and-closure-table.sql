
-- -------------------- --
-- BRIDGE TABLE EXAMPLE --
-- -------------------- --

-- written for postgresql
-- extremely simplied version

DROP SCHEMA IF EXISTS tutorial CASCADE;
CREATE SCHEMA tutorial;


CREATE TABLE tutorial.fact_campaign_funnel
(
    date_tk INT4
    , campaign_name VARCHAR(100)
    , interest_group_tk INT8
    , impressions INT
    , clicks INT
    , items_bought INT
    , basket_value NUMERIC
)
;

CREATE INDEX idx_fcf_date_tk ON tutorial.fact_campaign_funnel (date_tk);
CREATE INDEX idx_fcf_interest_group_tk ON tutorial.fact_campaign_funnel (interest_group_tk);

CREATE TABLE tutorial.bridge_interest_group
(
    interest_group_tk INT
    --, interest_group VARCHAR(100)
    , interest_tk INT8
    , weight_factor NUMERIC
)
;

-- in this case we will reuse existing groups
-- hence enforce unique index
CREATE UNIQUE INDEX idx_big_interest_group ON tutorial.bridge_interest_group (interest_group_tk, interest_tk);

CREATE TABLE tutorial.dim_interest
(
    interest_tk BIGSERIAL
    , interest VARCHAR(100)
)
;

CREATE TABLE
tutorial.dim_date
(
    date_tk INT4
    , date DATE
    , date_string CHAR(10)
    , day_of_month SMALLINT
    , month SMALLINT
    , quarter SMALLINT
    , year INT
    , iso8601_year INT
    , iso8601_week SMALLINT
    , iso8601_day_of_week SMALLINT
    , iso8601_year_week INT
    , iso8601_year_week_label VARCHAR(8)
    , year_month INT
    , year_month_label VARCHAR(8)
    , year_quarter INT
    , year_quarter_label CHAR(7)
    , dow_label VARCHAR(10)
)
;

-- Sample Data

INSERT INTO tutorial.dim_date
WITH date_series AS (
    SELECT
    --DATE(GENERATE_SERIES(DATE '2014-12-20', DATE '2014-12-22','1 day')) AS date
    DATE(GENERATE_SERIES(DATE '2014-12-20', DATE '2017-12-22','1 day')) AS date
)
SELECT
    CAST(TO_CHAR(date,'yyyyMMdd') AS INT) AS date_tk
    , date 
    , TO_CHAR(date,'yyyy-MM-dd') AS date_string
    , EXTRACT(DAY FROM date) AS day
    , EXTRACT(MONTH FROM date) AS month
    , EXTRACT(QUARTER FROM date) AS quarter
    , EXTRACT(YEAR FROM date) AS year
    -- the ISO 8601 year that the date falls in
    -- Each ISO year begins with the Monday of the week containing the 4th of January, 
    -- so in early January or late December the ISO year may be different from the Gregorian year.
    , EXTRACT(ISOYEAR FROM date) AS iso8601_year
    -- The number of the week of the year that the day is in. By definition (ISO 8601), weeks start on Mondays and the first week of a year contains January 4 of that year. In other words, the first Thursday of a year is in week 1 of that year.
    -- In the ISO definition, it is possible for early-January dates to be part of the 52nd or 53rd week of the previous year, 
    -- and for late-December dates to be part of the first week of the next year. 
    -- For example, 2005-01-01 is part of the 53rd week of year 2004, and 2006-01-01 is part of the 52nd week of year 2005, 
    -- while 2012-12-31 is part of the first week of 2013. It's recommended to use the isoyear field together with week to get consistent results.
    , EXTRACT(WEEK FROM date) AS iso8601_week
    -- The day of the week as Monday(1) to Sunday(7)
    -- This is identical to psql dow except for Sunday (which is 0). This matches the ISO 8601 day of the week numbering.
    , EXTRACT(ISODOW FROM date) AS iso8601_dow
    , CAST(CONCAT(EXTRACT(ISOYEAR FROM date), EXTRACT(WEEK FROM date)) AS INT) AS iso8601_year_week
    , CONCAT(EXTRACT(ISOYEAR FROM date), '-W', EXTRACT(WEEK FROM date)) AS iso8601_year_week_label
    , CAST(TO_CHAR(date, 'yyyyMM') AS INT) AS year_month
    , TO_CHAR(date, 'yyyy-MM') AS year_month_label
    , CAST(TO_CHAR(date, 'yyyy0q') AS INT) AS year_quarter
    , CONCAT(EXTRACT(YEAR FROM date), '-Q', EXTRACT(QUARTER FROM date)) AS year_quarter_label
    , TO_CHAR(date,'Day') AS dow_label
FROM date_series
;

SELECT * FROM tutorial.dim_date;

INSERT INTO tutorial.dim_interest
VALUES
    (1, 'sports')
    , (2, 'fashion')
    , (3, 'technology')
    , (4, 'politics')
    , (5, 'travel')
;

INSERT INTO tutorial.bridge_interest_group
VALUES
    (1, 2, 0.5)
    , (1, 3, 0.5)
    , (2, 1, 1::numeric/3::numeric)
    , (2, 3, 1::numeric/3::numeric)
    , (2, 5, 1::numeric/3::numeric)
;


INSERT INTO tutorial.fact_campaign_funnel
VALUES
    (20141220, 'Xmas 50% Sale', 2, 512000, 3000, 56, 2600)
    , (20141221, 'Santa TV Offer', 1, 230200, 5321, 77, 6800)
;


-- standard approach splitting the measures
-- this will be the table that we reference in the OLAP Schema as the
-- main fact table
CREATE VIEW tutorial.vw_fact_campaign_funnel AS
SELECT
    f.date_tk
    , f.campaign_name
    , b.interest_tk
    , f.impressions * b.weight_factor AS impressions
    , f.clicks * b.weight_factor AS clicks
    , f.items_bought  * b.weight_factor AS items_bought
    , f.basket_value * b.weight_factor AS basket_value
FROM tutorial.fact_campaign_funnel f
INNER JOIN tutorial.bridge_interest_group b
    ON f.interest_group_tk = b.interest_group_tk
;

-- impact improach: measure are not split
CREATE VIEW tutorial.vw_fact_campaign_funnel_impact AS
SELECT
    f.date_tk
    , f.campaign_name
    , b.interest_tk
    , f.impressions
    , f.clicks
    , f.items_bought
    , f.basket_value
FROM tutorial.fact_campaign_funnel f
INNER JOIN tutorial.bridge_interest_group b
    ON f.interest_group_tk = b.interest_group_tk
;

-- analysis
SELECT
    campaign_name
    , ROUND(SUM(impressions)) AS impressions
    , ROUND(SUM(clicks)) AS clicks
    , ROUND(SUM(items_bought)) AS items_bought
    , ROUND(SUM(basket_value)) AS basket_value
FROM tutorial.vw_fact_campaign_funnel f
INNER JOIN tutorial.dim_date dd
    ON dd.date_tk = f.date_tk
INNER JOIN tutorial.dim_interest di
    ON di.interest_tk = f.interest_tk
GROUP BY 1
;

-- So we see that the aggregation results are still correct. This is thanks
-- to the applied weighting factors. Sometimes however we want to have a different
-- view on this data. We might want to understand, which type of interests generated
-- the best results. This is when the impact report comes in handy:

SELECT
    interest
    , SUM(impressions) AS impressions
    , SUM(clicks) AS clicks
    , SUM(items_bought) AS items_bought
    , SUM(basket_value) AS basket_value
FROM tutorial.vw_fact_campaign_funnel_impact f
INNER JOIN tutorial.dim_date dd
    ON dd.date_tk = f.date_tk
INNER JOIN tutorial.dim_interest di
    ON di.interest_tk = f.interest_tk
GROUP BY 1
;

-- logic to determine if group exists
-- changed to return interest_group_tk instead of true/false
-- IMPORTANT: All the interests have to exist in dim_interest!
SELECT
    interest_group_tk
--     CASE 
--         WHEN SUM(group_found) > 0 THEN true
--         ELSE false
--     END AS group_found
FROM
(
    WITH tks AS (
        SELECT
            interest_tk
        FROM tutorial.dim_interest
        WHERE
            interest IN ('sports','finance','technology')
    ),
    tks_count AS (
        SELECT
            COUNT(*) AS no_of_interests
        FROM tks
    )
    SELECT
        b.interest_group_tk
        , COUNT(b.interest_tk) AS count_interests_within_group
        , (SELECT * FROM tks_count) AS no_of_interests
        , CASE
            WHEN COUNT(b.interest_tk) = (SELECT * FROM tks_count) THEN 1
            ELSE 0
         END AS group_found
    FROM tutorial.bridge_interest_group b
    WHERE
        interest_tk IN (
            SELECT
                *
            FROM tks
        )
    GROUP BY 1
    ORDER BY 1
) AS group_search
WHERE
    group_found = 1
;


-- for ETL
-- input: 3 = count of interests, 6,1,3 = interest_tk
-- replace with variables in ETL

SELECT
    interest_group_tk
FROM
(
    SELECT
        b.interest_group_tk
        , CASE
            WHEN COUNT(b.interest_tk) = 3 THEN 1
            ELSE 0
         END AS group_found
    FROM tutorial.bridge_interest_group b
    WHERE
        interest_tk IN ( 6,1,3 )
    GROUP BY 1
    ORDER BY 1
) AS group_search  
WHERE
    group_found = 1
;

SELECT * FROM tutorial.bridge_interest_group;
SELECT * FROM tutorial.fact_campaign_funnel;

-- --------------------- --
-- CLOSURE TABLE EXAMPLE --
-- --------------------- --

CREATE TABLE tutorial.dim_employee
(
    employee_tk SERIAL
    , supervisor_tk INT 
    , full_name VARCHAR(70) NOT NULL
    , city VARCHAR(70)
)
;

CREATE TABLE tutorial.employee_closure
(
    supervisor_tk INT4 NOT NULL
    , employee_tk INT4 NOT NULL
    , distance INT2
    -- optional fields
    -- , start_date
    -- , end_date
)
;

CREATE UNIQUE INDEX idx_employee_closure_pk 
ON tutorial.employee_closure
(
   supervisor_tk
   , employee_tk
)
;

CREATE INDEX idx_employee_closure_emp 
ON tutorial.employee_closure 
(
   employee_tk
)
;

CREATE TABLE tutorial.fact_sales
(
    date_tk INT4
    , employee_tk INT4
    , sales NUMERIC
)
;

-- sample data
-- taken from Mondrian docu and slightly changed

INSERT INTO tutorial.dim_employee
VALUES
    (1, NULL, 'Frank', 'Miami')
    , (2, 1, 'Bill', 'San Francisco')
    , (3, 2, 'Eric', 'San Francisco')
    , (4, 1, 'Jane', 'Palo Alto')
    , (5, 3, 'Mark', 'Palo Alto')
    , (6, 2, 'Carla', 'Palo Alto')
;

INSERT INTO tutorial.employee_closure
VALUES
    (1, 1, 0)
    , (1, 2, 1)
    , (1, 3, 2)
    , (1, 4, 1)
    , (1, 5, 3)
    , (1, 6, 2)
    , (2, 2, 0)
    , (2, 3, 1)
    , (2, 5, 2)
    , (2, 6, 1)
    , (3, 3, 0)
    , (3, 5, 1)
    , (4, 4, 0)
    , (5, 5, 0)
    , (6, 6, 0)
;

INSERT INTO tutorial.fact_sales
VALUES
    (20141220, 2, 3544)
    , (20141220, 3, 7342)
    , (20141220, 4, 345)
    , (20141220, 5, 2345)
    , (20141220, 6, 234)
;

-- analysis

-- sum of sales for all employees that work for Mark

SELECT
    full_name AS employee
    , SUM(sales) AS sales
FROM tutorial.fact_sales f
INNER JOIN tutorial.dim_date dd
    ON dd.date_tk = f.date_tk
INNER JOIN tutorial.employee_closure ec
    ON ec.employee_tk = f.employee_tk
INNER JOIN tutorial.dim_employee de
    ON de.employee_tk = ec.supervisor_tk
WHERE
    de.full_name = 'Frank' -- important to contrain here to avoid overcounting!
    AND dd."date" = '2014-12-20'
GROUP BY 1
;

--SELECT SUM(sales) FROM tutorial.fact_sales;

-- Let's see how much revenue Eric's people generated:
SELECT
    full_name AS employee
    , SUM(sales) AS sales
FROM tutorial.fact_sales f
INNER JOIN tutorial.dim_date dd
    ON dd.date_tk = f.date_tk
INNER JOIN tutorial.employee_closure ec
    ON ec.employee_tk = f.employee_tk
INNER JOIN tutorial.dim_employee de
    ON de.employee_tk = ec.supervisor_tk
WHERE
    de.full_name = 'Eric' -- important to contrain here to avoid overcounting!
    AND dd."date" = '2014-12-20'
GROUP BY 1
;

-- Sales for all supervisors that are located in San Francisco

SELECT
    'San Francisco' AS city
    , SUM(sales) AS sales
FROM tutorial.fact_sales f
INNER JOIN tutorial.dim_date dd 
    ON dd.date_tk = f.date_tk
WHERE
    -- use distinct employee_tk to not overcount!
    f.employee_tk IN (
        SELECT
            DISTINCT ec.employee_tk
        FROM tutorial.dim_employee de
        INNER JOIN tutorial.employee_closure ec
            ON ec.supervisor_tk = de.employee_tk
        WHERE
            de.city = 'San Francisco'
    )
    AND dd."date" = '2014-12-20'
GROUP BY 1
;

-- double check result
SELECT
    SUM(sales)
FROM tutorial.fact_sales 
WHERE
    employee_tk IN (2,3,5,6)
;

-- what's the total sales amount
SELECT
    SUM(sales)
FROM tutorial.fact_sales 
;
-- ----------------- --
-- CLOSURE EXAMPLE 2 --
-- ----------------- --

-- Code taken from http://alex-td.blogspot.co.uk/2013/01/parentchild-dimensions-in-jasper.html
-- sligthly modified

CREATE TABLE tutorial.dim_item (
    item_tk SERIAL
  , item_parent_tk INT4
  , item_name VARCHAR(32)
)
;

BEGIN;

  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (1, NULL, 'Food');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (2, 1, 'Vegetables');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (3, 1, 'Fruits');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (4, 1, 'Meat');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (5, 2, 'Potato');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (6, 2, 'Cabbage');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (7, 2, 'Beet');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (8, 3, 'Apple');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (9, 3, 'Cherry');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (10, 3, 'Orange');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (11, 4, 'Pork');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (12, 4, 'Beef');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (13, 4, 'Fowl');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (14, 13, 'Chicken');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (15, 13, 'Turkey');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (16, 13, 'Duck''s flesh');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (17, NULL, 'Entertainment');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (18, 17, 'Cinema');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (19, 17, 'Attraction');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (20, NULL, 'Payments');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (21, 20, 'Municipal Services');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (22, 20, 'Other Services');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (23, 21, 'Water');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (24, 21, 'Electric power');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (25, 21, 'Gas');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (26, 22, 'Internet');
  INSERT INTO tutorial.dim_item (item_tk, item_parent_tk, item_name) VALUES (27, 22, 'TV');
 
COMMIT;

CREATE TABLE tutorial.fact_item_expense (
    item_tk INT4
  , amount NUMERIC(18,2)
)
;

BEGIN;

  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (5, 15.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (6, 36.37);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (7, 9.04);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (8, 70.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (9, 58.30);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (10, 25.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (11, 96.33);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (12, 72.07);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (13, 48.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (14, 56.90);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (18, 264.70);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (19, 105.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (23, 500.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (24, 600.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (25, 150.80);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (26, 200.00);
  INSERT INTO tutorial.fact_item_expense (item_tk, amount) VALUES (27, 100.00);
 
COMMIT;

CREATE TABLE tutorial.item_closure (
    item_tk INT4
    , item_parent_tk INT4
);

CREATE UNIQUE INDEX idx_item_closure ON tutorial.item_closure (item_tk, item_parent_tk);

-- INSERT INTO tutorial.item_closure 
-- SELECT i.id
--      , connect_by_root i.id AS parent_id
--   FROM item i
--  CONNECT BY PRIOR i.id = i.parent_id
-- ;

-- DS: My scripts:

-- finding all descendents/parents of a specific item (WITH DISTANCE)
WITH RECURSIVE q AS (
    SELECT 
        item_tk
        , item_tk AS closure_parent_tk
        , item_parent_tk
        , item_name 
        , 0 AS distance
    FROM tutorial.dim_item 
    WHERE item_name = 'Pork'
    UNION ALL
    SELECT 
        q.item_tk
        , p.item_tk AS closure_parent_tk
        , p.item_parent_tk
        , p.item_name 
        , distance + 1 AS distance
    FROM 
    -- we need a sorted dataset to get the distance right
    (
        SELECT
            *
        FROM tutorial.dim_item
        ORDER BY item_tk ASC
    ) AS p
    INNER JOIN q
        ON p.item_tk = q.item_parent_tk
)
SELECT
    item_tk
    , closure_parent_tk
    , distance
    , item_name
FROM q
;

-- closure input query
--TRUNCATE tutorial.item_closure;
INSERT INTO tutorial.item_closure 
WITH RECURSIVE q AS (
    SELECT 
        item_tk
        , item_tk AS closure_parent_tk
        , item_parent_tk
        , item_name 
    FROM tutorial.dim_item 
    UNION ALL
    SELECT 
        q.item_tk
        , p.item_tk AS closure_parent_tk
        , p.item_parent_tk
        , p.item_name 
    FROM tutorial.dim_item AS p
    INNER JOIN q
        ON p.item_tk = q.item_parent_tk
)
SELECT
    item_tk
    , closure_parent_tk
FROM q
;

-- cross checking:

-- show items for which we have measures:
SELECT
    item_name
    , SUM(f.amount) AS amount
FROM tutorial.fact_item_expense AS f
INNER JOIN tutorial.dim_item AS d
    ON d.item_tk = f.item_tk
GROUP BY 1
ORDER BY 1
;

-- what's the total amount
SELECT
    SUM(amount)
FROM tutorial.fact_item_expense
;

-- ------------------ --
-- PROPERTIES EXAMPLE --
-- ------------------ --

CREATE TABLE tutorial.dim_product
(
    product_tk INT4
    , product_name VARCHAR(125)
    , category VARCHAR(70)
    , sub_category VARCHAR(70)
    , retail_price NUMERIC
    , wholesale_price NUMERIC
    , calories NUMERIC
    -- Nutrion Facts
    -- per serving: serving size 1 tablespoon(25g)
    , fat_grams  NUMERIC
    , carbohydrates_grams NUMERIC
    , dietary_fibre_grams NUMERIC
    , protein_grams NUMERIC
)
;

INSERT INTO tutorial.dim_product
(
    product_tk
    , product_name
    , category
    , sub_category
    , retail_price
    , wholesale_price
    , calories
    , fat_grams 
    , carbohydrates_grams
    , dietary_fibre_grams
    , protein_grams
)
VALUES
    (1, 'Green Fruitmix 150g', 'Snacks', 'Dried Fruits', 2.10, 1.20, 71, 0, 16.8, 0.5, 0.5)
    , (2, 'Green Fruitmix 300g', 'Snacks', 'Dried Fruits', 3.50, 1.70, 71, 0, 16.8, 0.5, 0.5)
;

CREATE TABLE tutorial.fact_grocery_sales
(
    date_tk INT4
    , product_tk INT4
    , quantity INT4
)
;

INSERT INTO tutorial.fact_grocery_sales
VALUES
    (20141220, 1, 345)
    , (20141220, 2, 172)
    , (20141221, 1, 375)
    , (20141221, 2, 145)
;

SELECT * FROM tutorial.dim_product;
SELECT * FROM tutorial.fact_grocery_sales;

-- ------------------ --
--    DATE DIM TEST   --
-- ------------------ --

CREATE TABLE tutorial.fact_dummy
(
    date_tk INT4
    , quantity INT4
)
;

INSERT INTO tutorial.fact_dummy
WITH date_series AS (
    SELECT
    --DATE(GENERATE_SERIES(DATE '2014-12-20', DATE '2014-12-22','1 day')) AS date
    DATE(GENERATE_SERIES(DATE '2014-12-20', DATE '2017-12-22','1 day')) AS date
)
SELECT
    CAST(TO_CHAR(date,'yyyyMMdd') AS INT) AS date_tk
    , FLOOR(random()*1000)
FROM date_series
;

SELECT * FROM tutorial.fact_dummy LIMIT 200;
