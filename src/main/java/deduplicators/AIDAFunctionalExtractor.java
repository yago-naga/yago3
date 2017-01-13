package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import basics.Fact;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.PersonNameExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.WikiInfoExtractor;
import javatools.administrative.Announce;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * AIDA Fact Extractor
 * 
 * Extracts all facts necessary for AIDA and puts them in a single file.
 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Mohamed Amir Yosef, with contributions from Johannes Hoffart.

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

public class AIDAFunctionalExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<Theme>();

    // For YAGO compliance.
    input.add(SchemaExtractor.YAGOSCHEMA);

    // Dictionary.
    input.add(PersonNameExtractor.PERSONNAMES);
    input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
    input.addAll(GenderExtractor.GENDERBYPRONOUN.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
    input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
    input.add(HardExtractor.HARDWIREDFACTS);

    // Metadata.
    input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));

    return input;
  }

  /** All facts of YAGO */
  public static final Theme AIDAFUNCTIONALFACTS = new Theme("aidaFunctionalFacts", "All functional facts necessary for AIDA", ThemeGroup.OTHER);

  /** All facts of YAGO */
  public static final Theme AIDAFUNCTIONALCONFLICTS = new Theme("_aidaFunctionalFactConflicts",
      "Facts that were not added because they conflicted with an existing fact");

  @Override
  public Theme myOutput() {
    return AIDAFUNCTIONALFACTS;
  }

  @Override
  public Theme conflicts() {
    return AIDAFUNCTIONALCONFLICTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if (AIDAExtractorMerger.relations.contains(fact.getRelation())) {
      return true;
    } else {
      return false;
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new AIDAFunctionalExtractor().extract(new File("C:/fabian/data/yago3"), "test");
  }

}
