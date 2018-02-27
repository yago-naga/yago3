/*
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

package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.PersonNameExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.ConteXtExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.StructureExtractor;
import fromWikipedia.WikiInfoExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * AIDA Fact Extractor
 * 
 * Extracts all facts necessary for AIDA and puts them in a single file.

*/

public class AIDAOneShotExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<Theme>();

    // For YAGO compliance.
    input.add(SchemaExtractor.YAGOSCHEMA);

    // Dictionary.
    input.addAll(StructureExtractor.STRUCTUREFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)); // also gives links and anchor texts.
    input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(PersonNameExtractor.PERSONNAMES);
    input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
    input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(GenderExtractor.GENDERBYPRONOUN.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
    input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
    input.add(HardExtractor.HARDWIREDFACTS);

    // Metadata.
    input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Types and Taxonomy.
    input.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    input.add(ClassExtractor.YAGOTAXONOMY);

    // Keyphrases.
    input.addAll(ConteXtExtractor.CONTEXTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return input;
  }

  /** All facts of YAGO */
  public static final Theme AIDAFACTS = new Theme("aidaFactsOneShot", "All facts necessary for AIDA", ThemeGroup.OTHER);

  /** All facts of YAGO */
  public static final Theme AIDACONFLICTS = new Theme("_aidaFactConflicts",
      "Facts that were not added because they conflicted with an existing fact");

  /** Relations that AIDA needs. */
  public static final Set<String> relations = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.label, "<hasGivenName>", "<hasFamilyName>",
      "<hasGender>", "<hasAnchorText>", "<hasInternalWikipediaLinkTo>", "<redirectedFrom>", "<hasWikipediaUrl>", "<hasCitationTitle>",
      "<hasWikipediaCategory>", "<hasWikipediaAnchorText>");

  @Override
  public Theme myOutput() {
    return AIDAFACTS;
  }

  @Override
  public Theme conflicts() {
    return AIDACONFLICTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if (relations.contains(fact.getRelation())) {
      return true;
    } else {
      return false;
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new AIDAOneShotExtractor().extract(new File("C:/fabian/data/yago3"), "test");
  }

}
