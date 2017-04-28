package fromThemes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.AttributeMatcher.MatchingStatistics.AttributeStat;
import fromThemes.AttributeMatcher.MatchingStatistics.Stat;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.TSVFile;
import javatools.util.FileUtils;
import utils.AttributeMappingMeasure;
import utils.FactCollection;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * This Extractor matches multilingual Infobox attributes to English attributes.
 * 'German' can be replaced by any arbitrary language as second language.
 *
 * Could be improved by:
 *  - type checking before attribute matching: would remove false matches because of year
 *  - apply redirects before attribute matching
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Farzaneh Mahdisoltani, with contributions from Fabian M. Suchanek.

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

public class AttributeMatcher extends MultilingualExtractor {

  public class MatchingStatistics {

    public class Stat {

      // Counts, for every foreign attribute, how often it appears with every
      // YAGO relation
      public int correctCount = 0;

      // Counts, for every foreign attribute, how often it appears with every
      // YAGO relation but is false
      public int wrongCount = 0;

      // Store example information, to facilitate debugging of matches
      List<String> correctExamples = null;

      List<String> wrongExamples = null;

      public void ensureExampleLists() {
        if (correctExamples == null) {
          correctExamples = new ArrayList<>();
          wrongExamples = new ArrayList<>();
        }
      }
    }

    /** Save information, how many value matches and clashes there are, compared to YAGO relations */
    public class AttributeStat {

      Map<String, Stat> yagoRelationMap = new HashMap<>();

      // Counts for every foreign attribute how many facts there are
      public int factCount = 0;

      public Stat get(String yagoRelation, boolean createIfMissing) {
        Stat result = yagoRelationMap.get(yagoRelation);
        if (result == null) {
          if (createIfMissing) {
            yagoRelationMap.put(yagoRelation, result = new Stat());
          }
        }
        return result;
      }
    }

    // map from attribute names to statistics
    public Map<String, AttributeStat> map = new HashMap<>();

    /** Shorthand function, gets or creates attribute statistics */
    public AttributeStat get(String foreignAttribute, boolean createIfMissing) {
      AttributeStat fAMap = map.get(foreignAttribute);
      if (fAMap == null) {
        if (createIfMissing) {
          map.put(foreignAttribute, fAMap = new AttributeStat());
        }
      }
      return fAMap;
    }

    /** Shorthand function, gets or creates match statistics */
    public Stat get(String foreignAttribute, String yagoRelation, boolean createIfMissing) {
      AttributeStat fAMap = get(foreignAttribute, createIfMissing);
      return fAMap.get(yagoRelation, createIfMissing);
    }

    /** Shorthand function, gets or creates macth statistics */
    public Stat get(Entry<String, String> e, boolean createIfMissing) {
      return get(e.getKey(), e.getValue(), createIfMissing);
    }
  }

  /** Threshold calculation for matching, used by MatchingDecider */
  public AttributeMappingMeasure measure = new AttributeMappingMeasure.Wilson(0.04);

  /** Retrieves a set of relations, based on attribute statistics */
  public interface MatchingDecider {

    public Set<String> getRelationsForAttribute(String foreignAttribute, AttributeStat stat);
  }

  public static final MultilingualTheme MATCHED_INFOBOXATTS = new MultilingualTheme("matchedAttributes",
      "Attributes of the Wikipedia infoboxes in different languages with their YAGO counterparts.");

  public Map<Entry<String, String>, Boolean> goldstandard;

  /**
   * Our input theme. This is a separate field so that it can be set from
   * outside for testing purposes.
   */
  public Theme inputTheme;

  /**
   * The English YAGO facts. This is a separate field so that it can be set
   * from outside for testing purposes.
   */
  public Theme referenceTheme = InfoboxMapper.INFOBOXFACTS.inEnglish();

  /** Reuse _attributeMatches_{languageCode}.tsv. This can be helpful for debugging, as facts, value matches and clashes don't need to be counted. */
  public boolean reuseAttributeMatchesFile = false;

  /** Write examples for matches and clashes to stdout */
  public int writeExampleCount = 0;

  @Override
  public Set<Theme> input() {
    if (reuseAttributeMatchesFile) {
      return new FinalSet<Theme>(inputTheme, PatternHardExtractor.MULTILINGUALATTRIBUTES);
    } else {
      return new FinalSet<Theme>(referenceTheme, inputTheme, PatternHardExtractor.MULTILINGUALATTRIBUTES);
    }
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(referenceTheme, PatternHardExtractor.MULTILINGUALATTRIBUTES);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(MATCHED_INFOBOXATTS.inLanguage(language));
  }

  @Override
  public void extract() throws Exception {

    Theme foreignFacts = inputTheme;

    if (!PatternHardExtractor.INFOBOXPATTERNS.isAvailableForReading()) {
      PatternHardExtractor.INFOBOXPATTERNS.assignToFolder(inputTheme.file().getParentFile());
    }
    // Load the map of manual mappings
    Map<String, String> manualMapping = getPredefinedMapping(null);

    MatchingStatistics foreignStats;
    if (reuseAttributeMatchesFile) {
      foreignStats = readMatchingStatistics();
    } else {
      foreignStats = calculateMatchingStatistics(foreignFacts);
    }

    Theme out = MATCHED_INFOBOXATTS.inLanguage(language);
    Writer tsv = null;
    if (!reuseAttributeMatchesFile) {
      tsv = FileUtils.getBufferedUTF8Writer(new File(out.file().getParent(), "_attributeMatches_" + language + ".tsv"));
    }

    List<Entry<String, String>> matchings = calculateMapping(foreignStats, tsv, getMatchingDecider(foreignStats, manualMapping, this.measure));
    for (Entry<String, String> e : matchings) {
      out.write(new Fact(e.getKey(), "<_infoboxPattern>", e.getValue()));
    }

    if (tsv != null) {
      tsv.close();
    }
  }

  /** Create an instance of the matching decider.
   * It searches for the relation with the most matches, and checks if 'measure' accepts it. Manual mappings override everything. */
  public MatchingDecider getMatchingDecider(final MatchingStatistics foreignStats, final Map<String, String> manualMapping,
      final AttributeMappingMeasure measure) {

    MatchingDecider md = new MatchingDecider() {

      @Override
      public Set<String> getRelationsForAttribute(String foreignAttribute, AttributeStat stat) {
        Set<String> result = new LinkedHashSet<>();
        Stat best = foreignStats.new Stat();
        String bestRelation = manualMapping != null ? manualMapping.get(foreignAttribute) : null;
        if (bestRelation != null) bestRelation = manualMapping != null ? manualMapping.get(foreignAttribute.toLowerCase()) : null;
        if (bestRelation != null) {
          best = foreignStats.new Stat();
          best.correctCount = Integer.MAX_VALUE;
          best.wrongCount = 0;
        }

        AttributeStat attrStat = foreignStats.get(foreignAttribute, true);
        for (String yagoRelation : attrStat.yagoRelationMap.keySet()) {
          Stat s = attrStat.get(yagoRelation, false);
          if (s == null) continue;
          if (s.correctCount == 0) continue;
          if (s.correctCount > best.correctCount) {
            best = s;
            bestRelation = yagoRelation;
          }
        }
        if (bestRelation != null && measure.measure(stat.factCount, best.correctCount, best.wrongCount)) {
          result.add(bestRelation);
        }
        return result;
      }
    };

    return md;
  }

  /** Read infobox patterns themes for manual mapping */
  public Map<String, String> getPredefinedMapping(String yagoFolder) {
    Map<String, String> result = new HashMap<>(), tmp;
    try {
      if (yagoFolder != null) {
        PatternHardExtractor.INFOBOXPATTERNS.assignToFolder(new File(yagoFolder));
        PatternHardExtractor.MULTILINGUALATTRIBUTES.assignToFolder(new File(yagoFolder));
      }
      tmp = PatternHardExtractor.INFOBOXPATTERNS.factCollection().getMap("<_infoboxPattern>");
      for (Entry<String, String> e : tmp.entrySet()) {
        result.put(e.getKey(), e.getValue());
        result.put(e.getKey().toLowerCase(), e.getValue());
      }
      tmp = PatternHardExtractor.MULTILINGUALATTRIBUTES.factCollection().getMap("<_infoboxPattern>");
      for (Entry<String, String> e : tmp.entrySet()) {
        result.put(e.getKey(), e.getValue());
        result.put(e.getKey().toLowerCase(), e.getValue());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /** Calculate mapping from statistics and write it to a tsv */
  public List<Entry<String, String>> calculateMapping(MatchingStatistics foreignStats, Writer tsv, MatchingDecider md) throws IOException {
    List<Entry<String, String>> matchings = new ArrayList<>();
    for (String foreignAttribute : foreignStats.map.keySet()) {
      AttributeStat attrStat = foreignStats.get(foreignAttribute, false);
      if (attrStat != null) {
        Set<String> relations = md.getRelationsForAttribute(foreignAttribute, attrStat);
        if (relations == null) continue;
        for (String yagoRelation : relations) {
          Stat stat = attrStat.get(yagoRelation, false);
          if (stat == null) continue;
          matchings.add(new AbstractMap.SimpleEntry<>(foreignAttribute, yagoRelation));

          if (tsv != null) {
            tsv.write(foreignAttribute + "\t" + yagoRelation + "\t" + attrStat.factCount + "\t" + stat.correctCount + "\t" + stat.wrongCount + "\n");
          }

          // write examples
          if (writeExampleCount > 0) {
            System.out.println(
                "\n" + foreignAttribute + "\t" + yagoRelation + "\t" + attrStat.factCount + "\t" + stat.correctCount + "\t" + stat.wrongCount);
            System.out.println("value matches (examples):");
            if (stat.correctExamples != null) {
              for (String f : stat.correctExamples) {
                System.out.println(f);
              }
            }
            System.out.println("value clatches (examples):");
            if (stat.wrongExamples != null) {
              for (String f : stat.wrongExamples) {
                System.out.println(f);
              }
            }
          }
        }
      }
    }
    return matchings;
  }

  /** Read matching statistics from a file. This is used for debugging and evaluating the AttributeMappingMeasure */
  public MatchingStatistics readMatchingStatistics() {
    return readMatchingStatistics(MATCHED_INFOBOXATTS.inLanguage(language).file().getParent());
  }

  /** Read matching statistics from a file. This is used for debugging and evaluating the AttributeMappingMeasure */
  public MatchingStatistics readMatchingStatistics(String attributeMatchesPath) {
    MatchingStatistics result = new MatchingStatistics();
    File f = new File(attributeMatchesPath, "_attributeMatches_" + language + ".tsv");
    try (TSVFile reader = new TSVFile(f)) {

      while (reader.hasNext()) {
        List<String> fields = reader.next();
        if (fields.size() != 5) {
          continue;
        }

        AttributeStat attrStat = result.get(fields.get(0), true);
        Stat s = attrStat.get(fields.get(1), true);
        try {
          attrStat.factCount = Integer.parseInt(fields.get(2));
          s.correctCount = Integer.parseInt(fields.get(3));
          s.wrongCount = Integer.parseInt(fields.get(4));
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /** Count matches and clashes for foreign attributes and yago facts. Case is ignored, as well as date values that occur with other values (entities/numbers) */
  private MatchingStatistics calculateMatchingStatistics(Theme foreignFacts) throws IOException {
    MatchingStatistics result = new MatchingStatistics();

    FactCollection yagoFacts = referenceTheme.factCollection();

    // We collect all objects for a subject and a relation
    // to account for the fact that we have the same object
    // in several types at this point.
    String currentSubject = "";
    String currentRelation = "";
    Set<String> currentObjects = new HashSet<>();

    // Note that the last fact is ignored, this doesn't matter if the number of facts is large
    for (Fact foreignFact : foreignFacts) {
      String foreignRelation = foreignFact.getRelation();
      String foreignSubject = foreignFact.getArg(1);
      String foreignObject = foreignFact.getArg(2);

      // TODO: check whether assumption, that foreignFacts are grouped by subject and relation is true
      if (currentSubject.equals(foreignSubject) && currentRelation.equals(foreignRelation)) {
        currentObjects.add(foreignObject);
        // matching won't work if cases are different. This seems to be a problem as of 2015-08-17
        String lowercase = foreignObject.toLowerCase();
        if (!lowercase.equals(foreignObject)) {
          currentObjects.add(lowercase);
        }
        continue;
      }

      // Come here if we have a new relation.
      // Treat currentRelation and currentObjects
      if (!currentObjects.isEmpty() && yagoFacts.containsSubject(currentSubject)) {
        // Let's now see to which YAGO relations the foreign attribute
        // could map
        for (String yagoRelation : yagoFacts.getRelations(currentSubject)) {

          Stat s = result.get(currentRelation, yagoRelation, true);
          Set<String> yagoObjects = yagoFacts.collectObjects(currentSubject, yagoRelation);
          // matching won't work if cases are different. This seems to be a problem as of 2015-08-17
          Set<String> yagoLowercaseObjects = new HashSet<String>();
          for (String obj : yagoObjects) {
            if (obj == null) continue;
            String lowercase = obj.toLowerCase();
            if (!lowercase.equals(obj)) {
              yagoLowercaseObjects.add(lowercase);
            }
          }

          // filter date like looking values if there is an entity
          Set<String> dates = new HashSet<>();
          for (String obj : currentObjects) {
            if (FactComponent.isUri(obj)) {
            } else if (FactComponent.isLiteral(obj)) {
              String datatype = FactComponent.getDatatype(obj);
              if (YAGO.string.equals(datatype)) {
              } else if (YAGO.date.equals(datatype)) {
                // extract year
                dates.add(obj);
                int pos = obj.lastIndexOf("-");
                pos = pos > 0 ? obj.lastIndexOf("-", pos - 1) : -1;
                int start = obj.charAt(0) == '"' ? 1 : 0;
                if (pos > 0) {
                  dates.add(FactComponent.forStringWithDatatype(obj.substring(start, pos), YAGO.integer));
                }
              }
            }
          }
          if (dates.size() > 0) {
            Set<String> newCurrentObjects = new HashSet<>(currentObjects);
            newCurrentObjects.removeAll(dates);
            if (newCurrentObjects.size() > 0) {
              currentObjects = newCurrentObjects;
            }
          }

          // calculate intersection
          if (containsAny(yagoObjects, currentObjects) || containsAny(yagoLowercaseObjects, currentObjects)) {
            if (writeExampleCount > 0) {
              s.ensureExampleLists();
              if (s.correctExamples.size() < writeExampleCount) {
                s.correctExamples.add(currentSubject + " " + currentRelation + " " + currentObjects + "; compare yago objects " + yagoObjects);
              }
            }
            s.correctCount += 1;
          } else {
            if (writeExampleCount > 0) {
              s.ensureExampleLists();
              if (s.wrongExamples.size() < writeExampleCount) {
                s.wrongExamples.add(currentSubject + " " + currentRelation + " " + currentObjects + "; compare yago objects " + yagoObjects);
              }
            }
            s.wrongCount += 1;
          }
        }
      }

      currentSubject = foreignSubject;
      currentRelation = foreignRelation;
      currentObjects.clear();
      currentObjects.add(foreignObject);
      String lowerCase = foreignObject.toLowerCase();
      if (!lowerCase.equals(foreignObject)) {
        currentObjects.add(lowerCase);
      }
      AttributeStat attrStat = result.get(currentRelation, true);
      attrStat.factCount += 1;
    }
    return result;
  }

  /** TRUE if intersection is non-empty */
  public static <K> boolean containsAny(Set<K> yagoObjects, Set<K> currentObjects) {
    for (K k : yagoObjects) {
      if (currentObjects.contains(k)) return (true);
    }
    return false;
  }

  public AttributeMatcher(String secondLang) {
    super(secondLang);
    inputTheme = isEnglish() ? InfoboxTermExtractor.INFOBOXTERMS.inLanguage(language)
        : InfoboxTermExtractor.INFOBOXTERMSTRANSLATED.inLanguage(language);
  }

  /** Constructor used to test the matching with/of other sources */
  protected AttributeMatcher(Theme inputTheme, Theme referenceTheme, String outputSuffix) {
    super(outputSuffix);
    this.inputTheme = inputTheme;
    this.referenceTheme = referenceTheme;
  }

  /**
   * Use this class if you want to match any attributes (not necessarily
   * multilingual ones).
   */
  public static class CustomAttributeMatcher extends Extractor {

    /** We have a small matcher that does the work */
    protected AttributeMatcher amatch;

    @Override
    public Set<Theme> input() {
      return amatch.input();
    }

    @Override
    public Set<Theme> output() {
      return amatch.output();
    }

    @Override
    public Set<Theme> inputCached() {
      return amatch.inputCached();
    }

    public CustomAttributeMatcher(Theme inputTheme, Theme referenceTheme, String outputSuffix) {
      super();
      amatch = new AttributeMatcher(inputTheme, referenceTheme, outputSuffix);
    }

    @Override
    public void extract() throws Exception {
      amatch.extract();
    }
  }
}
