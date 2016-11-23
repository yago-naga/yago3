package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import javatools.administrative.Announce;
import javatools.parsers.DateParser;

/**
 * This class can instantiate a template of the form "S P O; S P O; ..."
 * ...where the components can be (1) constants (2) variables of the form $i (3)
 * fact references of the form #i (4) typed variables of the form @XXX($i),
 * where XXX is String, Url, WikiLink, Date or Number
 *
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
public class FactTemplate {

  /** FactID */
  private String id = null;

  /** Argument 1 */
  private String arg1;

  /** Relation */
  private String relation;

  /** Argument 2 */
  private String arg2;

  /** Pattern for checking URLs */
  private static Pattern urlPattern = Pattern.compile("^https?://.+");

  /** Constructor */
  public FactTemplate(String arg1, String relation, String arg2) {
    super();
    // this.arg1 = arg1.intern();
    this.arg1 = arg1;
    // this.relation = relation.intern();
    this.relation = relation;
    // this.arg2 = arg2.intern();
    this.arg2 = arg2;
  }

  /** TRUE if the component is a variable */
  public static boolean isVariable(String c) {
    return (c.startsWith("?") || c.startsWith("$"));
  }

  /** TRUE if the component is a fact reference */
  public static boolean isFactReference(String c) {
    return (c.startsWith("#"));
  }

  /** TRUE if the component is a typed variable */
  public static boolean isTypedVariable(String c) {
    return (c.startsWith("@"));
  }

  /** TRUE if the component does not contain variables */
  private static boolean isInstantiated(String c) {
    return !isVariable(c) && !isFactReference(c) && !isTypedVariable(c);
  }

  /** TRUE if template doesn't contain variables */
  public boolean isInstantiable() {
    if (id != null) return true;
    if (arg1 == null || arg2 == null || relation == null) return false;
    return isInstantiated(arg1) && isInstantiated(relation) && isInstantiated(arg2);
  }

  public String getId() {
    return id;
  }

  public String getArg1() {
    return arg1;
  }

  public String getRelation() {
    return relation;
  }

  public String getArg2() {
    return arg2;
  }

  /** Adds the fact references used to a set */
  public void addFactReferencesTo(Set<Integer> set) {
    if (isFactReference(arg1)) set.add(arg1.charAt(1) - '0');
    if (isFactReference(arg2)) set.add(arg2.charAt(1) - '0');
  }

  /** Instantiates the fact template (or returns NULL) for a specific language
   * @param languageMap */
  public Fact instantiate(Map<String, String> variables, boolean makeId, String language, Map<String, String> languageMap) {
    String a1 = instantiate(arg1, variables, language, languageMap);
    String r = instantiate(relation, variables, language, languageMap);
    String a2 = instantiate(arg2, variables, language, languageMap);
    if (a1 == null || a2 == null || r == null) return (null);
    Fact fact = new Fact(a1, r, a2);
    if (makeId || id != null) fact.makeId();
    return (fact);
  }

  /** Creates a fact component from a string with variables for a specific language
   * @param languageMap */
  public static String instantiate(String s, Map<String, String> variables, String language, Map<String, String> languageMap) {
    for (Entry<String, String> rep : variables.entrySet())
      s = s.replace(rep.getKey(), rep.getValue());
    if (s.startsWith("@")) return (format(s, language, languageMap));
    if (isVariable(s) || isFactReference(s)) return (null);
    return (FactComponent.forAny(s));
  }

  /** Creates a fact component from a string with variables for a specific language
   * @param languageMap */
  public static String instantiatePartially(String s, Map<String, String> variables, String language, Map<String, String> languageMap) {
    for (Entry<String, String> rep : variables.entrySet())
      s = s.replace(rep.getKey(), rep.getValue());
    if (s.startsWith("@")) return (format(s, language, languageMap));
    if (isVariable(s) || isFactReference(s)) return s;
    return (FactComponent.forAny(s));
  }

  /** Creates a fact component for a formatted string of the form @XXX()
   * @param LanguageMap */
  public static String format(String word, String language, Map<String, String> languageMap) {
    final Pattern formattedPattern = Pattern.compile("@([a-zA-Z]+)\\((.*?)\\)");
    Matcher m = formattedPattern.matcher(word);
    if (!m.matches()) {
      Announce.debug("Ill-formed formatter", word);
      return (null);
    }
    String thing = m.group(2).trim();
    if (thing.isEmpty()) {
      Announce.debug("Empty fact template component");
      return (null);
    }
    switch (m.group(1)) {
      case "Text":
      case "String":
        return (FactComponent.forStringWithLanguage(thing, language, languageMap));
      case "Url":
        if (!urlPattern.matcher(thing).matches()) {
          Announce.debug("Not an URL:", thing);
          return (null);
        }
        return (FactComponent.forUri(thing));
      case "Entity":
        // There is a bug somewhere that introduces final >'s for
        // CategoryMapper.
        // TODO: Remove this hack if the bug is gone...
        if (thing.endsWith(">")) thing = thing.substring(0, thing.length() - 1);
        return (FactComponent.forForeignWikipediaTitle(thing, language));
      case "Date":
        String date = DateParser.normalize(thing);
        String[] datecomp = DateParser.getDate(date);
        if (datecomp == null) {
          Announce.debug("Not a date:", thing);
          return (null);
        }
        return (FactComponent.forDate(DateParser.newDate(datecomp[0], datecomp[1], datecomp[2])));
      default:
        Announce.warning("Unknown formatter", word);
    }
    return (null);
  }

  /** Reads facts from a fact template */
  public static List<FactTemplate> create(String factTemplates) {
    List<FactTemplate> factList = new ArrayList<>();
    for (String factTemplate : factTemplates.split(";")) {
      factTemplate = factTemplate.trim();
      if (factTemplate.length() == 0) continue;
      factTemplate += ' ';
      String[] split = new String[3];
      int argNum = 0;
      int pos = 0;
      while (argNum < 3) {
        while (factTemplate.charAt(pos) == ' ')
          if (++pos >= factTemplate.length()) throw new RuntimeException("Template must have 3 components: " + factTemplate);
        if (factTemplate.charAt(pos) == '"') {
          int endPos = factTemplate.indexOf('"', pos + 1);
          if (endPos == -1) throw new RuntimeException("Closing quote is missing in: " + factTemplate);
          split[argNum] = factTemplate.substring(pos, endPos + 1);
          pos = endPos + 1;
        } else if (factTemplate.charAt(pos) == '\'') {
          int endPos = factTemplate.indexOf('\'', pos + 1);
          if (endPos == -1) throw new RuntimeException("Closing quote is missing in: " + factTemplate);
          split[argNum] = factTemplate.substring(pos + 1, endPos);
          pos = endPos + 1;
        } else {
          int endPos = factTemplate.indexOf(' ', pos + 1);
          split[argNum] = factTemplate.substring(pos, endPos);
          pos = endPos + 1;
        }
        argNum++;
      }
      if (pos != factTemplate.length()) throw new RuntimeException("Too many components in template:" + factTemplate);
      FactTemplate result = new FactTemplate(split[0], split[1], split[2]);
      if (result.arg1.startsWith("#")) {
        if (result.arg1.length() != 2)
          throw new RuntimeException("A template list can only contain template references of the form #x: " + factTemplate);
        int factId = result.arg1.charAt(1) - '0';
        if (factId < 1 || factId > factList.size())
          throw new RuntimeException("#x in a template can only refer to preceding templates by their id, 1-based: " + factTemplate);
      }
      factList.add(result);
    }
    return (factList);
  }

  /** Instantiates fact templates with variables
   * @param languageMap */
  public static List<Fact> instantiate(List<FactTemplate> templates, Map<String, String> variables) {
    return instantiate(templates, variables, null);
  }

  /** Instantiates fact templates with variables
  * @param languageMap */
  public static List<Fact> instantiate(List<FactTemplate> templates, Map<String, String> variables, Map<String, String> languageMap) {
    return instantiate(templates, variables, "eng", languageMap);
  }

  /** Instantiates fact templates with variables for a specific language
   * @param languageMap */
  public static List<Fact> instantiate(List<FactTemplate> templates, Map<String, String> variables, String language,
      Map<String, String> languageMap) {
    List<Fact> factList = new ArrayList<Fact>();
    Set<Integer> factReferences = new TreeSet<>();
    for (FactTemplate template : templates) {
      template.addFactReferencesTo(factReferences);
    }
    for (int i = 0; i < templates.size(); i++) {
      if (factReferences.contains(i + 1)) {
        Fact fact = templates.get(i).instantiate(variables, true, language, languageMap);
        if (fact == null) {
          Announce.message("Can't instantiate", i, "in", templates, "with", variables);
          continue;
        }
        if (fact.getId() == null) fact.makeId();
        variables.put("#" + (i + 1), fact.getId());
        factList.add(fact);
      } else {
        factList.add(templates.get(i).instantiate(variables, false, language, languageMap));
      }
    }
    return (factList);
  }

  @Override
  public String toString() {
    return (id != null ? id + " " : "") + arg1 + " " + relation + " " + arg2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((arg1 == null) ? 0 : arg1.hashCode());
    result = prime * result + ((arg2 == null) ? 0 : arg2.hashCode());
    result = prime * result + ((relation == null) ? 0 : relation.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    FactTemplate other = (FactTemplate) obj;
    if (arg1 == null) {
      if (other.arg1 != null) return false;
    } else if (!arg1.equals(other.arg1)) return false;
    if (arg2 == null) {
      if (other.arg2 != null) return false;
    } else if (!arg2.equals(other.arg2)) return false;
    if (relation == null) {
      if (other.relation != null) return false;
    } else if (!relation.equals(other.relation)) return false;
    return true;
  }

  /** Returns a map of the variables to the fact */
  public Map<String, String> mapTo(Fact fact) {
    Map<String, String> result = new TreeMap<String, String>();
    if (!arg1.equals(fact.getArg(1))) {
      if (isVariable(arg1)) result.put(arg1, fact.getArg(1));
      else return (null);
    }
    if (!relation.equals(fact.getRelation())) {
      if (isVariable(relation)) result.put(relation, fact.getRelation());
      else return (null);
    }
    if (!arg2.equals(fact.getArg(2))) {
      if (isVariable(arg2)) result.put(arg2, fact.getArg(2));
      else return (null);
    }
    return (result);
  }

  /** Instantiates the fact template partially */
  private FactTemplate instantiatePartially(Map<String, String> variables, String language, Map<String, String> languageMap) {
    String a1 = instantiatePartially(arg1, variables, language, languageMap);
    String r = instantiatePartially(relation, variables, language, languageMap);
    String a2 = instantiatePartially(arg2, variables, language, languageMap);
    if (a1 == null || a2 == null || r == null) return (null);
    return new FactTemplate(a1, r, a2);
  }

  /** Instantiates the fact templates partially */
  public static List<FactTemplate> instantiatePartially(List<FactTemplate> templates, Map<String, String> variables) {
    return instantiatePartially(templates, variables, "eng", null);
  }

  /**
   * Instantiates the fact templates partially.
   * Creates FactIDs for templates if
   * - it is referenced by another template in the {@code templates} list AND
   * - it does not contain variables
   */
  public static List<FactTemplate> instantiatePartially(List<FactTemplate> templates, Map<String, String> variables, String language,
      Map<String, String> languageMap) {
    List<FactTemplate> result = new ArrayList<>();

    // check which metafact references (#) appear
    Set<Integer> factReferences = new TreeSet<>();
    for (FactTemplate template : templates) {
      template.addFactReferencesTo(factReferences);
    }

    for (int i = 0; i < templates.size(); i++) {
      FactTemplate t = templates.get(i);
      FactTemplate inst = t.instantiatePartially(variables, language, languageMap);
      boolean finished = inst.isInstantiable();
      if (finished && factReferences.contains(i + 1)) {
        inst.id = new Fact(inst.arg1, inst.relation, inst.arg2).makeId();
        variables.put("#" + (i + 1), inst.id);
      }

      if (inst != null) {
        result.add(inst);
      }
    }
    return (result);
  }

}
