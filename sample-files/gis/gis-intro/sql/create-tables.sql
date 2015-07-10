-- for postgresql
CREATE DATABASE elections;

\c elections

CREATE EXTENSION postgis;
-- check correct installation
SELECT postgis_full_version();

CREATE SCHEMA stg;

CREATE TABLE stg.uk_voting_results
(
  firstname VARCHAR(255)
, lastname VARCHAR(255)
, party_name VARCHAR(255)
, constituency_name VARCHAR(255)
, press_association_number INTEGER
, ons_gss_code VARCHAR(20)
, count_votes INTEGER
)
;

CREATE INDEX uvr_press_association_number 
ON stg.uk_voting_results(press_association_number)
;

CREATE TABLE stg.uk_voting_constituencies
(
  press_association_number INTEGER
, constituency_name VARCHAR(255)
, ons_gss_code VARCHAR(20)
, constituency_type VARCHAR(20)
, count_eligible_electors INTEGER
, count_valid_votes INTEGER
)
;

CREATE UNIQUE INDEX uvc_press_association_number 
ON stg.uk_voting_constituencies(press_association_number)
;


CREATE TABLE stg.uk_voting_winners
AS
SELECT
	constituency_name
	, party_name
	, a.count_votes
  , geom
FROM stg.uk_voting_results a
INNER JOIN
(
	SELECT 
		press_association_number
		, MAX(count_votes) AS count_votes
	FROM stg.uk_voting_results
	WHERE
		press_association_number IS NOT NULL
	GROUP BY 1
) b
ON
	a.press_association_number = b.press_association_number
	AND a.count_votes = b.count_votes
INNER JOIN stg.uk_map_constituencies c
ON c.pcon13cd = a.ons_gss_code
;
