package deduplicators;

import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import basics.RDFS;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.HardExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.ConteXtExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.StructureExtractor;


public class AIDAExtractorMerger extends Extractor {

  /** All facts of YAGO */
  public static final Theme AIDAFACTS = new Theme("aidaFacts",
      "All facts necessary for AIDA", ThemeGroup.OTHER);
  
  /** Relations that AIDA needs. */
  public static final Set<String> relations = new FinalSet<>(
      RDFS.type, RDFS.subclassOf, RDFS.label, "<hasGivenName>", "<hasFamilyName>",
      "<hasGender>", "<hasAnchorText>", "<hasInternalWikipediaLinkTo>",
      "<redirectedFrom>", "<hasWikipediaUrl>", "<hasCitationTitle>",
      "<hasWikipediaCategory>", "<hasWikipediaAnchorText>", "<_hasTranslation>",
      "<hasWikipediaId>", "<_yagoMetadata>");

  
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
    input.addAll(DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor
        .allLanguagesExceptEnglish()));
    input.addAll(DictionaryExtractor.CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor
        .allLanguagesExceptEnglish()));
    
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
        if(isAIDARelation(fact)) {
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
