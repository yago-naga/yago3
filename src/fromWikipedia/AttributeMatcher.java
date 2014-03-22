package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.D;
import javatools.datatypes.FrequencyVector;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromThemes.InfoboxTermExtractor;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches multilingual Infobox attributes to English attributes.
 * 'German' can be replaced by any arbitrary language as second language.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class AttributeMatcher extends Extractor {

  private static ExtendedFactCollection yagoFacts = null;

  /* Map of German attribute to YAGO relations to |Test & Gold|, |Test| */
  Map<String, Map<String, Integer>> german2yago2count;

  private String language;

  private double WILSON_THRESHOLD = 0;

  private double SUPPORT_THRESHOLD = 1;

  public static final HashMap<String, Theme> MATCHED_INFOBOXATTS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> MATCHEDATTSOURCES_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> MATCHED_INFOBOXATTS_SCORES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      MATCHED_INFOBOXATTS_MAP.put(s, new Theme("matchedAttributes" + Extractor.langPostfixes.get(s),
          "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
      MATCHEDATTSOURCES_MAP.put(s, new Theme("matchedAttributesSources" + Extractor.langPostfixes.get(s), "Sources of matched attributes",
          ThemeGroup.OTHER));
      MATCHED_INFOBOXATTS_SCORES_MAP.put(s, new Theme("matchedAttributesScores" + Extractor.langPostfixes.get(s),
          "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
    }

  }

  @Override
  public Set<Theme> input() {
    HashSet<Theme> result = new HashSet<Theme>(Arrays.asList(InfoboxMapper.INFOBOXFACTS_MAP.get("en"),

    InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language)));
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new HashSet<>(Arrays.asList(MATCHED_INFOBOXATTS_MAP.get(language), MATCHEDATTSOURCES_MAP.get(language),
        MATCHED_INFOBOXATTS_SCORES_MAP.get(language)));
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {

    /**
     * Counts, for every german attribute, how often it appears with every
     * YAGO relation
     */
    german2yago2count = new HashMap<String, Map<String, Integer>>();

    /** Counts for every german attribute how many facts there are */
    Map<String, Integer> germanFactCountPerAttribute = new HashMap<>();

    yagoFacts = getFactCollection(input.get(InfoboxMapper.INFOBOXFACTS_MAP.get("en")));
    for (Fact germanFact : input.get(InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language))) {
      String germanRelation = germanFact.getRelation();
      String germanSubject = germanFact.getArg(1);
      String germanObject = germanFact.getArg(2);
      /*
       * We look for German attributes where both subject and object (in
       * translated form) appear in YAGO. If the attribute is skipped at
       * this level, it still has the chance to be processed if appears
       * with 'good' subject and object in other facts
       */
      if (yagoFacts.getFactsWithSubject(germanSubject) == null || yagoFacts.getFactsWithObject(germanObject) == null) continue;

      // We increase the counter for the attribute of the german fact
      D.addKeyValue(germanFactCountPerAttribute, germanRelation, 1);
      // System.out.println(germanFactCountPerAttribute.get(germanRelation));

      // Let's now see to which YAGO relations the german attribute could
      // map
      Map<String, Integer> germanMap = german2yago2count.get(germanRelation);
      if (germanMap == null) german2yago2count.put(germanRelation, germanMap = new HashMap<String, Integer>());
      List<Fact> relationsWithGivenSubjectAndObject = yagoFacts.getFactsWithSubject(germanSubject);
      // intersection
      relationsWithGivenSubjectAndObject.retainAll(yagoFacts.getFactsWithObject(germanObject));
      for (Fact fact : relationsWithGivenSubjectAndObject) {
        D.addKeyValue(germanMap, fact.getRelation(), 1);
      }
    }
    // Now output the results:
    for (String germanAttribute : german2yago2count.keySet()) {
      for (String yagoRelation : german2yago2count.get(germanAttribute).keySet()) {
        int total = germanFactCountPerAttribute.get(germanAttribute);
        int correct = german2yago2count.get(germanAttribute).get(yagoRelation);
        double[] ws = FrequencyVector.wilson(total, correct);

        if (correct >= 0) {
          Fact scoreFact = new Fact(germanAttribute,
              (double) correct / total + " <" + correct + "/" + total + ">" + "     " + ws[0] + "    " + ws[1], yagoRelation);
          writers.get(MATCHED_INFOBOXATTS_SCORES_MAP.get(language)).write(scoreFact);
          /** filtering out */
          if (ws[0] - ws[1] > WILSON_THRESHOLD && correct > SUPPORT_THRESHOLD) {
            Fact fact = new Fact(germanAttribute, "<_infoboxPattern>", yagoRelation);
            write(writers, MATCHED_INFOBOXATTS_MAP.get(language), fact, MATCHEDATTSOURCES_MAP.get(language),
                FactComponent.wikipediaURL(germanAttribute), "");
          }

        }
      }
    }

  }

  private static synchronized ExtendedFactCollection getFactCollection(FactSource infoboxFacts) {
    if (yagoFacts != null) return (yagoFacts);
    yagoFacts = new ExtendedFactCollection();
    for (Fact f : infoboxFacts) {
      yagoFacts.add(f);
    }
    return (yagoFacts);
  }

  public static Set<String> getIntersection(Set<String> set1, Set<String> set2) {
    boolean set1IsLarger = set1.size() > set2.size();
    Set<String> cloneSet = new HashSet<String>(set1IsLarger ? set2 : set1);
    cloneSet.retainAll(set1IsLarger ? set1 : set2);
    return cloneSet;
  }

  public static Set<String> getUnion(Set<String> set1, Set<String> set2) {
    Set<String> cloneSet = new HashSet<String>(set1);
    cloneSet.addAll(set2);
    return cloneSet;
  }

  public void setWilsonThreshold(double d) {
    WILSON_THRESHOLD = d;
  }

  public void setSupportThreshold(double d) {
    SUPPORT_THRESHOLD = d;
  }

  public AttributeMatcher(String secondLang) {
    language = secondLang;
  }

  public static void main(String[] args) throws Exception {

    new AttributeMatcher("de").extract(new File("D:/data3/yago2s"), "mapping infobox attributes in different languages");
  }

}
