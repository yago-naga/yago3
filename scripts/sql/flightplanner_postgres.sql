/*     FlightPlanner for Postgres
*
* This script creates the database used for the YAGO2s Flight Planner. 
* To run this script
*  - first run postgres.sql to load YAGO into a database
*  - then run 
*        psql -a -d <database> -h <hostname> -U <user> -f <thisScript>
*/


-- Creating table
DROP TABLE IF EXISTS yagoFlights;
CREATE TABLE yagoFlights (fromAirport varchar, toAirport varchar, airline varchar);
DROP TABLE IF EXISTS yagoPlaces;
CREATE TABLE yagoPlaces (place varchar, latitude float, longitude float);

-- Loading data
INSERT INTO yagoPlaces
 SELECT lati.subject, lati.value, longi.value FROM yagoFacts lati, yagoFacts longi
 WHERE lati.subject=longi.subject AND lati.predicate='<hasLatitude>' AND longi.predicate='<hasLongitude>';

INSERT INTO yagoFlights
 SELECT c.subject, c.object, b.object FROM yagoFacts c, yagoFacts b
 WHERE c.id=b.subject AND c.predicate='<isConnectedTo>' AND b.predicate='<byTransport>';
 
-- Creating indexes
CREATE INDEX yagoFlightIndex ON yagoFlights(fromAirport);
CREATE INDEX yagoPlaceIndex ON yagoPlaces(place);
CREATE INDEX yagoPlaceLatIndex ON yagoPlaces(latitude);
CREATE INDEX yagoPlaceLonIndex ON yagoPlaces(longitude);

-- working on auto complete yago means table

-- remove old tables
DROP TABLE IF EXISTS allyagoplacesmeans;
DROP TABLE IF EXISTS yagoplacesmeans;



-- create gather table
CREATE TABLE allyagoplacesmeans
(
  str character varying,
  subjct character varying,
  "value" double precision
);

--insert data with duplicates
insert into allyagoplacesmeans (
select lower(substring(f.object from 2 for char_length(f.object)-6)), f.subject,f2.value from yagofacts f 
inner join yagoplaces p on p.place = f.subject
inner join yagofacts f2 on f.subject = f2.subject and f2.predicate = '<hasWikipediaArticleLength>'
where f.predicate  = 'rdfs:label' and length(f.object)>10);

--create final table
CREATE TABLE yagoplacesmeans
(
  str character varying,
  subjct character varying,
  "value" double precision
);

--remove duplicates
insert into yagoplacesmeans (select str, subjct, max(value) from allyagoplacesmeans group by str,subjct);

-- drop duplicate table
--drop table allyagoplacesmeans;

--create index
CREATE INDEX ind_yagoplacesmeans_str ON yagoplacesmeans (str);

-- done
