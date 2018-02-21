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

package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactSource;
import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.Pair;

/**
 * Extracts from strings by help of fact templates
 *
*/
public class FactTemplateExtractor {

  /** List of patterns */
  public final List<Pair<Pattern, List<FactTemplate>>> patterns = new ArrayList<>();

  /**
   * Constructor
   *
   * @throws IOException
   */
  public FactTemplateExtractor(FactSource facts, String relation) throws IOException {
    this(new FactCollection(facts), relation);
  }

  /** Constructor */
  public FactTemplateExtractor(FactCollection facts, String relation) {
    Announce.doing("Loading fact templates of", relation);
    for (Fact fact : facts.getFactsWithRelation(relation)) {
      patterns.add(new Pair<Pattern, List<FactTemplate>>(fact.getArgPattern(1), FactTemplate.create(fact.getArgJavaString(2))));
    }
    if (patterns.isEmpty()) {
      Announce.warning("No patterns found for relation " + relation);
    }
    Announce.done();
  }

  /**
   * Extracts facts using patterns without provenance
   *
   * @param string
   * @param dollarZero
   * @return Collection of facts
   */
  public Collection<Fact> extract(String string, String dollarZero) {
    return extract(string, dollarZero, "eng");
  }

  /**
  * Extracts facts using patterns without provenance for a specific language
  *
  * @param string
  * @param dollarZero
  * @return Collection of facts
  */
  public Collection<Fact> extract(String string, String dollarZero, String language) {
    Map<String, String> languageMap = null;
    try {
      languageMap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");
    } catch (IOException e) {
      e.printStackTrace();
    }

    Map<String, String> variables = new TreeMap<>();
    variables.put("$0", dollarZero);
    List<List<FactTemplate>> templateGroups = makeTemplateGroups(string, language);
    List<Fact> facts = new ArrayList<>();
    for (List<FactTemplate> template : templateGroups) {
      facts.addAll(FactTemplate.instantiate(template, variables, language, languageMap));
    }
    return facts;
  }

  /**
   * Creates templates which are used for extraction.
   * For every pattern it creates a new {@code FactTemplate} list, in order to preserve meta-fact references
   *
   * @param string
   * @return
   */
  public List<List<FactTemplate>> makeTemplateGroups(String string, String language) {
    Map<String, String> languageMap = null;
    try {
      languageMap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<List<FactTemplate>> result = new ArrayList<>();
    for (Pair<Pattern, List<FactTemplate>> pattern : patterns) {
      Matcher m = pattern.first().matcher(string);
      while (m.find()) {
        Map<String, String> variables = new TreeMap<>();
        for (int i = 1; i <= m.groupCount(); i++) {
          // somehow a NullPointerException occurred in the next line
          if (m.group(i) == null || m.group(i).trim().isEmpty()) {
            Announce.debug("$" + i + " was empty, skipping fact for pattern: " + pattern);
            continue;
          } else {
            variables.put("$" + i, m.group(i));
          }
        }
        List<FactTemplate> templates = FactTemplate.instantiatePartially(pattern.second(), variables, language, languageMap);
        result.add(templates);
      }
    }
    return (result);
  }

  /**
   * Extracts facts using patterns including provenance
   *
   * @param string
   * @param dollarZero
   * @return Collection of <fact, extractionTechnique> pairs
   * @deprecated Found no reference to this. Can it go away? Fabian
   */
  @Deprecated
  public Collection<Pair<Fact, String>> extractWithProvenance(String string, String dollarZero) {
    Map<String, String> languageMap = null;
    try {
      languageMap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<Pair<Fact, String>> result = new LinkedList<>();
    for (Pair<Pattern, List<FactTemplate>> pattern : patterns) {
      Matcher m = pattern.first().matcher(string);
      while (m.find()) {
        Map<String, String> variables = new TreeMap<>();
        variables.put("$0", dollarZero);
        for (int i = 1; i <= m.groupCount(); i++) {
          if (m.group(i).trim().isEmpty()) {
            Announce.debug("$" + i + " was empty, skipping fact for pattern: " + pattern);
            continue;
          } else {
            variables.put("$" + i, m.group(i));
          }
        }
        for (Fact f : FactTemplate.instantiate(pattern.second(), variables, "eng", languageMap)) {
          result.add(new Pair<Fact, String>(f, pattern.first.toString() + " -> " + pattern.second.toString()));
        }
      }
    }
    return (result);
  }
}
