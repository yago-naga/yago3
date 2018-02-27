/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

package fromOtherSources;

import java.io.File;
import java.util.Set;

import basics.Fact;
import basics.FactSource;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Produces the hard-coded facts that are YAGO-internal.
 *
*/
public class PatternHardExtractor extends HardExtractor {

  /** Patterns of infoboxes */
  public static final Theme INFOBOXPATTERNS = new Theme("_infoboxPatterns", "The Wikipedia infobox patterns used in YAGO");

  /** Patterns for cleaning article texts */
  public static final Theme AIDACLEANINGPATTERNS = new Theme("_aidaCleaningPatterns", "The AIDA cleaning patterns used in YAGO");

  /** Language codes */
  public static final Theme LANGUAGECODEMAPPING = new Theme("_languageCodeMappings", "Mappings from ISO 639-1 codes to ISO 639-2/T codes.");

  /** False facts*/
  public static final Theme FALSEFACTS = new Theme("_falseFacts", "Facts that shall be excluded from YAGO");

  /** Manual multilingual attribute mappings*/
  public static final Theme MULTILINGUALATTRIBUTES = new Theme("_multilingualAttributes",
      "Manual mappings for multilingual attributes where the automatic mapping fails.");

  public static final Theme INFOBOXTEMPORALPATTERNS = new Theme("_infoboxTemporalPatterns", "The Wikipedia infobox patterns used in YAGO");

  /** Regexes which transform infobox values */
  public static final Theme INFOBOXREPLACEMENTS = new Theme("_infoboxReplacements", "The Wikipedia infobox patterns used in YAGO");

  /** Patterns of titles */
  public static final Theme TITLEPATTERNS;
  static {
    if (Extractor.includeConcepts) {
      TITLEPATTERNS = new Theme("_titlePatterns_includeConcepts", "The replacement patterns for Wikipedia titles used in YAGO");
    }
    else {
      TITLEPATTERNS = new Theme("_titlePatterns_onlyNamedEntities", "The replacement patterns for Wikipedia titles used in YAGO");
    }
  }
  
  /** Patterns of strings for the TermParser */
  public static final Theme STRINGPARSER = new Theme("_stringParser", "The replacement patterns for string extraction");

  /** Patterns of titles */
  public static final Theme DATEPARSER = new Theme("_dateParser", "The replacement patterns for date extraction");

  /** Patterns of titles */
  public static final Theme NUMBERPARSER = new Theme("_numberParser", "The replacement patterns for number extraction");

  /** Patterns of categories */
  public static final Theme CATEGORYPATTERNS = new Theme("_categoryPatterns", "The Wikipedia category patterns used in YAGO");

  /** Patterns of disambiguation pages */
  public static final Theme DISAMBIGUATIONTEMPLATES = new Theme("_disambiguationPatterns", "Patterns for the disambiguation pages of Wikipedia");

  /** Patterns of entity keyphrases */
  public static final Theme CONTEXTPATTERNS = new Theme("_extendedContextWikiPatterns", "Patterns for extracting Keyphrases");

  /** Patterns of entity keyphrases */
  public static final Theme STRUCTUREPATTERNS = new Theme("_extendedStructureWikiPatterns",
      "Patterns for extracting regular structure from Wikipedia (e.g. links)");

  /** Implication rules of YAGO */
  public static final Theme RULES = new Theme("_rules", "These are the implication rules of YAGO");

  /** Implication rules of YAGO SPOTLX representation */
  public static final Theme SPOTLX_ENTITY_RULES = new Theme("_spotlxEntityRules", "Implication rules for YAGO SPOTLX representation");

  public static final Theme SPOTLX_FACT_RULES = new Theme("_spotlxFactRules", "Implication rules for YAGO SPOTLX representation");

  @Override
  public Set<Theme> output() {
    return (new FinalSet<Theme>(INFOBOXPATTERNS, INFOBOXTEMPORALPATTERNS, INFOBOXREPLACEMENTS, TITLEPATTERNS, CATEGORYPATTERNS, RULES,
        DISAMBIGUATIONTEMPLATES, CONTEXTPATTERNS, STRUCTUREPATTERNS, LANGUAGECODEMAPPING, SPOTLX_ENTITY_RULES, SPOTLX_FACT_RULES, STRINGPARSER,
        NUMBERPARSER, DATEPARSER, AIDACLEANINGPATTERNS, FALSEFACTS, MULTILINGUALATTRIBUTES));
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Copying patterns");
    Announce.message("Input folder is", inputData);
    for (Theme t : output()) {
      File f = t.findFileInFolder(inputData);
      Announce.doing("Copying hard wired facts from", f.getName());
      for (Fact fact : FactSource.from(f)) {
        t.write(fact);
      }
      Announce.done();
    }
    Announce.done();
  }

  public PatternHardExtractor(File inputFolder) {
    super(inputFolder);
  }

  public PatternHardExtractor() {
    this(new File("./data"));
  }

  public static void main(String[] args) throws Exception {
    Parameters.init(args[0]);
    File yago = Parameters.getFile("yagoFolder");
    new PatternHardExtractor(new File("./data")).extract(yago, "test");
  }
}
