package deduplicators;

import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.MetadataExtractor;
import fromOtherSources.WikidataImageExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryGlossExtractor;
import fromWikipedia.ConteXtExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.StructureExtractor;
import fromWikipedia.WikiInfoExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Mohamed Amir Yosef, with contributions
from Johannes Hoffart.

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

public class AIDAExtractorMerger extends Extractor {

  /** All facts of YAGO */
  public static final Theme AIDAFACTS = new Theme("aidaFacts", "All facts necessary for AIDA", ThemeGroup.OTHER);

  /** Relations that AIDA needs. */
  public static final Set<String> relations = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.label, RDFS.sameas, "<hasGivenName>", "<hasFamilyName>",
      "<hasGender>", "<hasAnchorText>", "<hasInternalWikipediaLinkTo>", "<redirectedFrom>", "<hasWikipediaUrl>", "<hasCitationTitle>",
      "<hasWikipediaCategory>", "<hasWikipediaAnchorText>", "<_hasTranslation>", "<hasWikipediaId>", "<_yagoMetadata>", YAGO.hasWikiDataImage, YAGO.hasWikiPage, YAGO.hasImageUrl, YAGO.hasGloss);

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>();

    //YAGO functional facts needed for AIDA
    //hasWIkipediaUrl, hasGender
    //hasGivenName, hasFamilyName
    input.add(AIDAFunctionalExtractor.AIDAFUNCTIONALFACTS);

    //the rest of the facts that don't need functional check
    // Dictionary.
    input.addAll(StructureExtractor.STRUCTUREFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)); // also gives links and anchor texts.
    input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(HardExtractor.HARDWIREDFACTS);

    // Types and Taxonomy.
    input.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    input.add(ClassExtractor.YAGOTAXONOMY);

    // Keyphrases.
    input.addAll(ConteXtExtractor.CONTEXTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Translation.
    input.addAll(DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    input.addAll(DictionaryExtractor.CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));

    // Metadata.
    input.add(MetadataExtractor.METADATAFACTS);
    input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Image.
    input.add(WikidataImageExtractor.WIKIDATAIMAGES);

    // WikiData links.
    input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
    
    // Wikipedie category glosses.
    input.addAll(CategoryGlossExtractor.CATEGORYGLOSSES.inLanguages(MultilingualExtractor.wikipediaLanguages));

    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(AIDAFACTS);
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Merging all AIDA Sources");
    for (Theme theme : input()) {
      Announce.doing("Merging facts from", theme);
      for (Fact fact : theme) {
        if (isAIDARelation(fact)) {
          AIDAFACTS.write(fact);
        }
      }
      Announce.done();
    }
    Announce.done();
  }

  public boolean isAIDARelation(Fact fact) {
    if (relations.contains(fact.getRelation())) {
      return true;
    } else {
      return false;
    }
  }

}
