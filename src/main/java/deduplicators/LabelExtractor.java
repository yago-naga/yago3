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

package deduplicators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.PersonNameExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.RedirectExtractor;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - LabelExtractor
 * 
 * Deduplicates all label facts (except for the multilingual ones). This
 * extractor is different from FactExtractor so that it can run in parallel.
 *
*/

public class LabelExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("We don't have conflicts here, so let's just take any order")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(HardExtractor.HARDWIREDFACTS);
    input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
    input.add(PersonNameExtractor.PERSONNAMES);
    input.add(WordnetExtractor.WORDNETWORDS);
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(WordnetExtractor.WORDNETGLOSSES);
    input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
    input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
    input.add(GeoNamesDataImporter.GEONAMES_MAPPED_DATA);
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return input;
  }

  /** Relations that we care for */
  public static Set<String> relations = new FinalSet<>(RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>",
      "<hasGloss>", "<redirectedFrom>");

  /** All facts of YAGO */
  public static final Theme YAGOLABELS = new Theme("yagoLabels",
      "All facts of YAGO that contain labels (rdfs:label, skos:prefLabel, isPreferredMeaningOf, hasGivenName, hasFamilyName, hasGloss, redirectedFrom)",
      ThemeGroup.CORE);

  @Override
  public Theme myOutput() {
    return YAGOLABELS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return relations.contains(fact.getRelation());
  }

}
