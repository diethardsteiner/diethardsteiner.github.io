-- for postgresql

-- this version of the script is for the D3 C-Tools tutorial
-- where we are not using a GIS enable DB

CREATE DATABASE elections;

\c elections

CREATE SCHEMA stg_d3_map;

CREATE TABLE stg_d3_map.uk_voting_results
(
    firstname VARCHAR(255)
    , lastname VARCHAR(255)
    , party_name VARCHAR(255)
    , constituency_name VARCHAR(255)
    , press_association_number INTEGER
    , ons_gss_code VARCHAR(20)
    , count_votes INTEGER
    , share_votes NUMERIC  
)
;

CREATE INDEX uvr_press_association_number 
ON stg_d3_map.uk_voting_results(press_association_number)
;

CREATE TABLE stg_d3_map.uk_voting_constituencies
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
ON stg_d3_map.uk_voting_constituencies(press_association_number)
;


CREATE VIEW stg_d3_map.vw_uk_voting_winners
AS
SELECT
    constituency_name
    , a.ons_gss_code
    , party_name
    , a.count_votes
    , a.share_votes
FROM stg_d3_map.uk_voting_results a
INNER JOIN
(
    SELECT 
        press_association_number
        , MAX(count_votes) AS count_votes
    FROM stg_d3_map.uk_voting_results
    WHERE
        press_association_number IS NOT NULL
    GROUP BY 1
) b
ON
    a.press_association_number = b.press_association_number
    AND a.count_votes = b.count_votes
;

--- for dashboard

-- understand where the strongholds of a given party is

--- cda name: qry_parties
SELECT DISTINCT party_name FROM stg_d3_map.uk_voting_results;

--- cda name: qry_party_share
SELECT
    ons_gss_code
    , share_votes AS count_votes
FROM stg_d3_map.uk_voting_results
WHERE
    party_name = 'Green Party Candidate'
;
