/*     YAGO2s SPOTLX to Postgres database
*
* This script loads YAGO2s SPOTLX into a Postgres database.
* 
* The database has to be in UTF-8. To create such a database, say
*   createdb yago2spotlx --encoding utf8 --lc-ctype en_US.utf8  --lc-collate en_US.utf8 --template template0
* 
*  (or template_postgis)
* 
* After that, run this script as follows:
*  - download YAGO in TSV format
*  - in a shell or dos box, cd into the directory of the YAGO TSV files
*  - run 
*        psql -a -d <database> -h <hostname> -U <user> -f <thisScript>
*/

-- Creating table
DROP TABLE IF EXISTS relationalfacts;
CREATE TABLE relationalfacts (id varchar,
                              subject varchar,
                              predicate varchar,
                              object varchar,
                              value float,
                              timebegin timestamp with time zone,
                              timeend timestamp with time zone,
                              location varchar,
                              locationlatitude float,
                              locationlongitude float,
                              source varchar,
                              context varchar);
							  
-- Loading files
-- (don't worry if some files are not found)
SET client_encoding to 'utf-8';
\copy relationalfacts FROM 'spotlx_yagoGeonamesGlosses.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoDBpediaClasses.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoLabels.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoSources.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoDBpediaInstances.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoLiteralFacts.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoStatistics.tsv' NULL AS 'null' 
\copy relationalfacts FROM 'spotlx_yagoFacts.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoMetaFacts.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoTaxonomy.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoGeonamesClassIds.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoMultilingualClassLabels.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoTransitiveType.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoGeonamesClasses.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoMultilingualInstanceLabels.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoGeonamesData.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoSchema.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoWikipediaInfo.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoGeonamesEntityIds.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoSimpleTaxonomy.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoWordnetIds.tsv' NULL AS 'null'
\copy relationalfacts FROM 'spotlx_yagoWordnetDomains.tsv' NULL AS 'null'

-- Remove facts on which we cannot build the index
DELETE FROM relationalfacts WHERE length(object)>1000;

SET maintenance_work_mem TO 1000000;

-- Creating indexes
CREATE INDEX spotlx_id ON relationalfacts (id);
CREATE INDEX spotlx_id_subject ON relationalfacts (id, subject);
CREATE INDEX spotlx_predicate_subject_object ON relationalfacts (predicate, subject, object);
CREATE INDEX spotlx_subject_predicate_object ON relationalfacts (subject, predicate, object);
CREATE INDEX spotlx_object_predicate_subject ON relationalfacts (object, predicate, subject);
CREATE INDEX spotlx_subject_object_predicate ON relationalfacts (subject, object, predicate);
CREATE INDEX spotlx_predicate_object_subject ON relationalfacts (predicate, object, subject);
CREATE INDEX spotlx_object_subject_predicate ON relationalfacts (object, subject, predicate);
CREATE INDEX spotlx_spatial ON relationalfacts USING GIST(ST_GeographyFromText('SRID=4326;POINT(' || locationlongitude || ' ' || locationlatitude || ')'));
CREATE INDEX spotlx_timebegin_timeend ON relationalfacts(timebegin, timeend);
CREATE INDEX spotlx_timeend_timebegin ON relationalfacts(timeend, timebegin);
CREATE INDEX spotlx_location ON relationalfacts(location);
CREATE INDEX spotlx_locationlatitude_locationlongitude ON relationalfacts(locationlatitude, locationlongitude);
CREATE INDEX spotlx_locationlongitude_locationlatitude ON relationalfacts(locationlongitude, locationlatitude);
CREATE INDEX spotlx_subject_text ON relationalfacts USING gin(to_tsvector('english', subject));
CREATE INDEX spotlx_context_text ON relationalfacts USING gin(to_tsvector('english', context));
CREATE INDEX spotlx_subjectlow_predicate_object ON relationalfacts (lower(subject), predicate, object);
CREATE INDEX spotlx_objectlow_predicate_subject ON relationalfacts (lower(object), predicate, subject);

-- Computing statistics
ALTER TABLE relationalfacts ALTER id SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER subject SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER object SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER predicate SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER timebegin SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER timeend SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER location SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER locationlatitude SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER locationlongitude SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER source SET STATISTICS 1000;
ALTER TABLE relationalfacts ALTER context SET STATISTICS 1000;
VACUUM ANALYZE relationalfacts;
