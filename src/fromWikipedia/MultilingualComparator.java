package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;

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

  protected String language;

  public static final HashMap<String, Theme> MISSINGFACTS_MAP = new HashMap<String, Theme>();

  /** Types deduced from categories */
  public static final HashMap<String, Theme> CONFIRMINGFACTS_MAP = new HashMap<String, Theme>();

  /** Classes deduced from categories */
  public static final HashMap<String, Theme> NEWFACTS_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      MISSINGFACTS_MAP.put(s, new Theme("missingFacts" + Extractor.langPostfixes.get(s), "The sources of category type facts"));
      CONFIRMINGFACTS_MAP.put(s, new Theme("confirmingFacts" + Extractor.langPostfixes.get(s), "", ThemeGroup.TAXONOMY));
      NEWFACTS_MAP.put(s, new Theme("newFacts" + Extractor.langPostfixes.get(s), ""));
    }

  }

  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(TypeCoherenceChecker.YAGOTYPES, CategoryTypeExtractor.CATEGORYTYPES_MAP.get(baseLang),
        InfoboxTypeExtractor.INFOBOXRAWTYPES_MAP.get(baseLang), CategoryTypeExtractor.CATEGORYTYPES_MAP.get(language),
        InfoboxTypeExtractor.INFOBOXRAWTYPES_MAP.get(language)));
  }

  @Override
  public Set<Theme> output() {

    return new TreeSet<Theme>();
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
    loadFacts(input.get(CategoryTypeExtractor.CATEGORYTYPES_MAP.get(baseLang)), baseLangFactCollection);
    loadFacts(input.get(InfoboxTypeExtractor.INFOBOXRAWTYPES_MAP.get(baseLang)), baseLangFactCollection);

    ExtendedFactCollection languageFactCollection = new ExtendedFactCollection();
    loadFacts(input.get(CategoryTypeExtractor.CATEGORYTYPES_MAP.get(language)), languageFactCollection);
    loadFacts(input.get(InfoboxTypeExtractor.INFOBOXRAWTYPES_MAP.get(language)), languageFactCollection);

    int missingFacts = 0;
    int newFacts = 0;
    int confirmingFacts = 0;

    for (Fact f : yagoTypes) {
      if (languageFactCollection.contains(f.getArg(1), f.getRelation(), f.getArg(2))) {
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

    }
    output.get(MISSINGFACTS_MAP.get(language)).write(new Fact("Missing_facts", "hasNumberOf", missingFacts + ""));
    output.get(CONFIRMINGFACTS_MAP.get(language)).write(new Fact("Confirming_facts", "hasNumberOf", confirmingFacts + ""));
    output.get(NEWFACTS_MAP.get(language)).write(new Fact("New_facts", "hasNumberOf", newFacts + ""));
    System.out.println(confirmingFacts);
    System.out.println(missingFacts);
    System.out.println(newFacts);

  }

  public MultilingualComparator(String baseLanguage, String language) {
    this.baseLang = baseLanguage;
    this.language = language;
  }

  public static void main(String[] args) throws Exception {
    new MultilingualComparator("en", "de").extract(new File("D:/data3/yago2s/"), "nnn");
  }

}
