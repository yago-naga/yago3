package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * MultilingualComparator - YAGO2s
 * 
 * Finds the confirming facts, new facts, and missing facts, for Types in each language. 
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class MultilingualComparator extends Extractor {

  protected String baseLang;

  public static final HashMap<String, Theme> MISSINGFACTS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> MISSINGENTITIES_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CONFIRMINGFACTS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CONFIRMINGENTITIES_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> NEWFACTS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> NEWENTITIES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      MISSINGFACTS_MAP.put(s, new Theme("missingFacts" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
      MISSINGENTITIES_MAP.put(s, new Theme("missingEntities" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
      CONFIRMINGFACTS_MAP.put(s, new Theme("confirmingFacts" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
      CONFIRMINGENTITIES_MAP.put(s, new Theme("confirmingEntities" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
      NEWFACTS_MAP.put(s, new Theme("newFacts" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
      NEWENTITIES_MAP.put(s, new Theme("newEntities" + Extractor.langPostfixes.getFactsWithRelation(s), ""));
    }

  }

  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(TypeCoherenceChecker.YAGOTYPES, CategoryTypeExtractor.CATEGORYTYPES.inLanguage(baseLang),
        InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(baseLang), CategoryTypeExtractor.CATEGORYTYPES.inLanguage(language),
        InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(language)));
  }

  @Override
  public Set<Theme> output() {

    return new FinalSet<Theme>(MISSINGFACTS_MAP.get(language), MISSINGENTITIES_MAP.get(language), CONFIRMINGFACTS_MAP.get(language),
        CONFIRMINGFACTS_MAP.get(language), CONFIRMINGENTITIES_MAP.get(language), NEWFACTS_MAP.get(language), NEWENTITIES_MAP.get(language));
  }

  protected ExtendedFactCollection loadFacts(FactSource factSource, ExtendedFactCollection result) {
    for (Fact f : factSource) {
      result.add(f);
    }
    return (result);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactSource yagoTypes = input.get(TypeCoherenceChecker.YAGOTYPES);
    ExtendedFactCollection baseLangFactCollection = new ExtendedFactCollection();
    loadFacts(input.get(CategoryTypeExtractor.CATEGORYTYPES.inLanguage(baseLang)), baseLangFactCollection);
    loadFacts(input.get(InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(baseLang)), baseLangFactCollection);
    Set<String> baseLangEntities = baseLangFactCollection.getSubjects();

    ExtendedFactCollection secondLangFactCollection = new ExtendedFactCollection();
    loadFacts(input.get(CategoryTypeExtractor.CATEGORYTYPES.inLanguage(language)), secondLangFactCollection);
    loadFacts(input.get(InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(language)), secondLangFactCollection);
    Set<String> secondLangEntities = baseLangFactCollection.getSubjects();

    int missingFacts = 0;
    int missingEntities = 0;
    int newFacts = 0;
    int newEntities = 0;
    int confirmingFacts = 0;
    int confirmingEntities = 0;

    for (Fact f : yagoTypes) {
      if (secondLangFactCollection.contains(f.getArg(1), f.getRelation(), f.getArg(2))) {
        if (baseLangFactCollection.contains(f.getArg(1), f.getRelation(), f.getArg(2))) {
          confirmingFacts++;
          output.get(CONFIRMINGFACTS_MAP.get(language)).write(f);
        } else {
          newFacts++;
          output.get(NEWFACTS_MAP.get(language)).write(f);
        }
      } else if (baseLangFactCollection.contains(f.getArg(1), f.getRelation(), f.getArg(2))) {
        missingFacts++;
        output.get(MISSINGFACTS_MAP.get(language)).write(f);
      }

      if (secondLangEntities.contains(f.getArg(1))) {
        if (baseLangEntities.contains(f.getArg(1))) {
          confirmingEntities++;
          output.get(CONFIRMINGENTITIES_MAP.get(language)).write(new Fact(f.getArg(1), "", ""));
        } else {
          newEntities++;
          output.get(NEWENTITIES_MAP.get(language)).write(new Fact(f.getArg(1), "", ""));
        }
      } else if (baseLangEntities.contains(f.getArg(1))) {
        missingEntities++;
        output.get(MISSINGENTITIES_MAP.get(language)).write(new Fact(f.getArg(1), "", ""));
      }

    }
    output.get(MISSINGFACTS_MAP.get(language)).write(new Fact("Missing_facts", "hasNumberOf", missingFacts + ""));
    output.get(MISSINGENTITIES_MAP.get(language)).write(new Fact("Missing_entities", "hasNumberOf", missingEntities + ""));
    output.get(CONFIRMINGFACTS_MAP.get(language)).write(new Fact("Confirming_facts", "hasNumberOf", confirmingFacts + ""));
    output.get(CONFIRMINGENTITIES_MAP.get(language)).write(new Fact("Confirming_entities", "hasNumberOf", confirmingEntities + ""));
    output.get(NEWFACTS_MAP.get(language)).write(new Fact("New_facts", "hasNumberOf", newFacts + ""));
    output.get(NEWENTITIES_MAP.get(language)).write(new Fact("New_entities", "hasNumberOf", newEntities + ""));

  }

  public MultilingualComparator(String baseLanguage, String language) {
    this.baseLang = baseLanguage;
    this.language = language;
  }

  public static void main(String[] args) throws Exception {
    new MultilingualComparator("en", "de").extract(new File("D:/data3/yago2s/"), "nnn");
  }

}
