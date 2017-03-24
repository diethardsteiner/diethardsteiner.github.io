/* SQL Dialect: Postgresql */
DROP SCHEMA IF EXISTS multivalued CASCADE;

CREATE SCHEMA multivalued;

CREATE TABLE multivalued.dim_student
(
  student_tk BIGINT
  , student_name VARCHAR(20)
)
;


CREATE TABLE multivalued.fact_grades
(
  student_tk BIGINT
  , course_name VARCHAR(20)
  , grade SMALLINT
)
;
  

CREATE TABLE multivalued.fact_student_hobbies
(
  student_tk BIGINT
  , hobby_name VARCHAR(20)
  , cnt SMALLINT
)
;


INSERT INTO multivalued.dim_student VALUES
(1, 'Bob')
, (2, 'Lilian')
;

INSERT INTO multivalued.fact_grades VALUES 
(1, 'Math', 7)
, (1, 'Math', 4)
, (1, 'Physics', 3)
, (2, 'Math', 8)
;

INSERT INTO multivalued.fact_student_hobbies VALUES
(1, 'Gaming', 1)
, (1, 'Reading', 1)
, (2, 'Gaming', 1)
, (2, 'Jogging', 1)
, (2, 'Writing', 1)
;

SELECT * FROM multivalued.dim_student;
SELECT * FROM multivalued.fact_grades;
SELECT * FROM multivalued.fact_student_hobbies;

/* part 2 */

CREATE TABLE multivalued.dim_client
(
  client_tk BIGINT
  , client_name VARCHAR(20)
)
;

INSERT INTO multivalued.dim_client VALUES
(1, 'Joe')
, (2, 'Susan')
, (3, 'Tim')
;


CREATE TABLE multivalued.fact_client_interests
(
  client_tk BIGINT
  , interest_name VARCHAR(20)
  , cnt SMALLINT
)
;

INSERT INTO multivalued.fact_client_interests VALUES
(1, 'Fishing', 1)
, (1, 'Photography', 1)
, (1, 'Cooking', 1)
, (2, 'Cooking', 1)
, (2, 'Biology', 1)
, (3, 'Geography', 1)
, (3, 'Photography', 1)
, (3, 'Cooking', 1)
;


CREATE TABLE multivalued.dim_product
(
  product_tk BIGINT
  , product_name VARCHAR(20)
  , unit_price DECIMAL
)
;

INSERT INTO multivalued.dim_product VALUES
( 1, 'AAA', 2.00)
, (2, 'BBB', 3.00)
, (3, 'CCC',  1.40)
;


CREATE TABLE multivalued.dim_date
(
  date_tk BIGINT
  , the_date DATE
)
;

INSERT INTO multivalued.dim_date VALUES
(20170324, '2017-03-24')
;

CREATE TABLE multivalued.fact_sales
(
  date_tk BIGINT
  , client_tk BIGINT
  , product_tk BIGINT
  , no_of_units INT
  , amount_spent DECIMAL
)
;

INSERT INTO multivalued.fact_sales VALUES
  (20170324,1,1,2,4)
, (20170324,2,1,3,6)
, (20170324,1,2,4,12)
, (20170324,2,2,2,6)
, (20170324,3,2,3,9)
, (20170324,2,3,2,2.8)
, (20170324,3,3,1,1.4)
;

/* exploded view */
SELECT
   the_date
   , client_name
   , interest_name
   , product_name
   , no_of_units
   , amount_spent
   , no_of_units * unit_price AS amount_spent_cc
FROM multivalued.fact_sales fc
INNER JOIN multivalued.dim_date dd
  USING(date_tk)
INNER JOIN multivalued.dim_client du
  USING(client_tk)
INNER JOIN multivalued.dim_product da
  USING(product_tk)
INNER JOIN multivalued.fact_client_interests fui
  USING(client_tk)
;
