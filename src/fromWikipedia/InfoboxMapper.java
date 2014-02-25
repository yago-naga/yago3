package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.parsers.Char;
import utils.PatternList;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.InfoboxTermExtractor;
import fromThemes.Redirector;
import fromThemes.TypeChecker;

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * Maps the facts in the output of InfoboxExtractor for different languages.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxMapper extends Extractor {
  
  protected String language;
  
  public static final HashMap<String, Theme> INFOBOXFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> INFOBOXFACTS_TOTYPECHECK_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> INFOBOXFACTS_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> INFOBOXSOURCES_MAP = new HashMap<String, Theme>();
  
  static {
    for (String s : Extractor.languages) {
      INFOBOXFACTS_TOREDIRECT_MAP.put(s, new Theme("infoboxFactsToBeRedirected" + Extractor.langPostfixes.get(s),
          "Facts of infobox, still to be redirected and type-checked", ThemeGroup.OTHER));
      INFOBOXFACTS_TOTYPECHECK_MAP.put(s, new Theme("infoboxFactsToBeTypechecked" + Extractor.langPostfixes.get(s),
          "Facts of infobox, redirected, still to be type-checked", ThemeGroup.OTHER));
      INFOBOXFACTS_MAP.put(s, new Theme("infoboxFacts" + Extractor.langPostfixes.get(s), "Facts of infobox, redirected and type-checked",
          ThemeGroup.OTHER));
      INFOBOXSOURCES_MAP.put(s, new Theme("infoboxSources" + Extractor.langPostfixes.get(s), "Sources of infobox", ThemeGroup.OTHER));
    }
    
  }
  
  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(new Redirector(INFOBOXFACTS_TOREDIRECT_MAP.get(language), INFOBOXFACTS_TOTYPECHECK_MAP.get(language),
        this, this.language), new TypeChecker(INFOBOXFACTS_TOTYPECHECK_MAP.get(language), INFOBOXFACTS_MAP.get(language), this)));
  }
  
  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>(Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, HardExtractor.HARDWIREDFACTS,
        InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language), WordnetExtractor.WORDNETWORDS, PatternHardExtractor.TITLEPATTERNS));
    if (!language.equals("en")) {
      input.add(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(language));
    }
    return input;
  }
  
  @Override
  public Set<Theme> output() {
    return new HashSet<>(Arrays.asList(INFOBOXFACTS_TOREDIRECT_MAP.get(language), INFOBOXSOURCES_MAP.get(language)));
  }
  
  /** Extracts a relation from a string */
  protected void extract(String entity, String object, String relation, String attribute, Map<String, String> preferredMeanings,
      FactCollection factCollection, Map<Theme, FactWriter> writers, PatternList replacements) throws IOException {
    
    // Check inverse
    boolean inverse;
    if (relation.endsWith("->")) {
      inverse = true;
      relation = Char.cutLast(Char.cutLast(relation)) + '>';
    } else {
      inverse = false;
    }
    
    if (inverse) {
      Fact fact = new Fact(object, relation, entity);
      write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language), FactComponent.wikipediaURL(entity),
          "InfoboxExtractor from " + attribute);
    } else {
      Fact fact = new Fact(entity, relation, object);
      write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language), FactComponent.wikipediaURL(entity),
          "InfoboxExtractor from " + attribute);
    }
  }
  
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    
    FactCollection infoboxFacts = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    PatternList replacements = new PatternList(infoboxFacts, "<_infoboxReplace>");
    Map<String, String> combinations = infoboxFacts.asStringMap("<_infoboxCombine>");
    Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(input);
    
    FactCollection nonMappedFacts = new FactCollection(input.get(InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language)));
    
    // Get the infobox patterns depending on the language
    Map<String, Set<String>> patterns;
    if (this.language.equals("en")) {
      patterns = InfoboxExtractor.infoboxPatterns(infoboxFacts);
    } else {
      FactCollection matchedAttributes = new FactCollection(input.get(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(language)));
      patterns = InfoboxExtractor.infoboxPatterns(matchedAttributes);
    }
    
    for (Fact f : nonMappedFacts) {
      
      String attribute = FactComponent.stripBrackets(FactComponent.stripPrefix(f.getRelation()));
      String value = f.getArg(2);
      if (value == null) {
        continue;
      }
      
      Set<String> relations = patterns.get(attribute);
      if (relations == null) continue;
      
      for (String relation : relations) {
        extract(f.getArg(1), value, relation, attribute, preferredMeanings, hardWiredFacts, writers, replacements);
      }
      
    }
    
  }
  
  public InfoboxMapper(String lang) {
    language = lang;
  }
  
  public static void main(String[] args) throws Exception {
    new InfoboxMapper("en").extract(new File("/home/jbiega/data/yago2s/"), "mapping infobox attributes into infobox facts");
    
  }
  
}