/*     YAGO to Postgres database
*
* This script loads YAGO into a Postgres database.
* 
* The database has to be in UTF-8. To create such a database, say
*   createdb yago --encoding utf8 --lc-ctype en_US.utf8  --lc-collate en_US.utf8 --template template0
* 
* After that, run this script as follows:
*  - download YAGO in TSV format
*  - in a shell or dos box, cd into the directory of the YAGO TSV files
*  - run 
*        psql -a -d <database> -h <hostname> -U <user> -f <thisScript>
*/

-- Creating table
DROP TABLE IF EXISTS yagoFacts;
CREATE TABLE yagoFacts (id varchar, subject varchar, predicate varchar, object varchar, value float);

-- Loading files
-- (don't worry if some files are not found)
SET client_encoding to 'utf-8';
\copy yagoFacts FROM 'yagoGeonamesGlosses.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoSimpleTypes.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoDBpediaClasses.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoLabels.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoSources.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoDBpediaInstances.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoLiteralFacts.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoStatistics.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoFacts.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoMetaFacts.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoTaxonomy.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoGeonamesClassIds.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoMultilingualClassLabels.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoTransitiveType.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoGeonamesClasses.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoMultilingualInstanceLabels.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoTypes.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoGeonamesData.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoSchema.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoWikipediaInfo.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoGeonamesEntityIds.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoSimpleTaxonomy.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoWordnetIds.tsv' NULL AS ''
\copy yagoFacts FROM 'yagoWordnetDomains.tsv' NULL AS ''
 
-- Remove facts on which we cannot build the index
DELETE FROM yagofacts WHERE length(object)>1000;

-- Creating indexes
CREATE INDEX yagoIndexSubject ON yagoFacts(subject);
CREATE INDEX yagoIndexObject ON yagoFacts(object);
CREATE INDEX yagoIndexValue ON yagoFacts(value);
CREATE INDEX yagoIndexPredicate ON yagoFacts(predicate);
CREATE INDEX yagoIndexId ON yagoFacts(id);
CREATE INDEX yagoIndexSubjectPredicate ON yagoFacts(subject,predicate);
CREATE INDEX yagoIndexObjectPredicate ON yagoFacts(object,predicate);
CREATE INDEX yagoIndexValuePredicate ON yagoFacts(value,predicate);

-- Creating index for case-insensitive search
-- (you may abort the script if you don't need this index) 
CREATE INDEX yagoIndexLowerObject ON yagofacts (lower(object));
-- done