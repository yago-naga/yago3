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

package fromThemes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import basics.Fact;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.parsers.NumberFormatter;
import utils.FactCollection;
import utils.FactTemplate;
import utils.Theme;

/**
 * Generates the results of rules.
 * 
*/
public abstract class BaseRuleExtractor extends Extractor {

  public abstract Theme getRULERESULTS();

  public abstract Theme getRULESOURCES();

  /** Represents a rule */
  public static class Rule {

    /** Conditions of the rule */
    public final List<FactTemplate> body;

    /** Consequences of the rule */
    public final List<FactTemplate> head;

    /** Reference of the current fact */
    public final int reference;

    /** reference to the original rule */
    public final Rule original;

    /** Creates a rule from an implies-fact */
    public Rule(Fact f) {
      this(FactTemplate.create(f.getArgJavaString(1)), FactTemplate.create(f.getArgJavaString(2)), 1);
    }

    /** creates a rule with conditions and consequences */
    public Rule(List<FactTemplate> body, List<FactTemplate> head, int reference) {
      this(body, head, reference, null);
    }

    /** creates a rule with conditions and consequences */
    public Rule(List<FactTemplate> body, List<FactTemplate> head, int reference, Rule original) {
      this.body = body;
      this.head = head;
      this.reference = reference;
      this.original = original == null ? this : original;
    }

    /**
     * returns a map that maps the variables of the first body template to
     * match fact (or null)
     */
    public Map<String, String> mapFirstTo(Fact fact) {
      return (body.get(0).mapTo(fact));
    }

    /**
     * Removes first body atom, maps remainder
     * 
     * @param string
     */
    public Rule rest(Map<String, String> map, String id) {
      Map<String, String> refMap = new TreeMap<>(map);
      if (id != null) refMap.put("#" + reference, id);
      return (new Rule(FactTemplate.instantiatePartially(body.subList(1, body.size()), map), FactTemplate.instantiatePartially(head, map),
          reference + 1, original));
    }

    /** TRUE if the rule does not have conditions */
    public boolean isReadyToGo() {
      return (body.isEmpty());
    }

    /** Returns head, translated to facts */
    public List<Fact> headFacts() {
      Map<String, String> empty = new TreeMap<>();
      return (FactTemplate.instantiate(head, empty));
    }

    @Override
    public String toString() {
      return body + "=>" + head;
    }

    /** Returns the first body atom */
    public FactTemplate firstBody() {
      return (body.get(0));
    }
  }

  /** represents a set of rules */
  public static class RuleSet {

    protected Map<String, Map<String, List<Rule>>> rel2subj2rules = new TreeMap<>();

    /** returns $ for variables, the value otherwise */
    public static String index(String value) {
      if (FactTemplate.isVariable(value)) return ("$");
      else return (value);
    }

    /** Adds a rule */
    public void add(Rule r) {
      String rel = index(r.firstBody().getRelation());
      Map<String, List<Rule>> map = rel2subj2rules.get(rel);
      if (map == null) rel2subj2rules.put(rel, map = new TreeMap<String, List<Rule>>());
      String subj = index(r.firstBody().getArg1());
      D.addKeyValue(map, subj, r, ArrayList.class);
    }

    /* Get a list of all rules */
    public List<Rule> allRules() {
      List<Rule> allRules = new ArrayList<Rule>();

      for (Entry<String, Map<String, List<Rule>>> rsr : rel2subj2rules.entrySet()) {
        for (Entry<String, List<Rule>> sr : rsr.getValue().entrySet()) {
          allRules.addAll(sr.getValue());
        }
      }

      return allRules;
    }

    /** Returns rules whose first body atom matches potentially */
    public List<Rule> potentialMatches(Fact f) {
      List<Rule> result = new ArrayList<>();
      for (String rel : Arrays.asList("$", f.getRelation())) {
        Map<String, List<Rule>> map = rel2subj2rules.get(rel);
        if (map == null) continue;
        for (String subj : Arrays.asList("$", f.getArg(1))) {
          List<Rule> candidates = map.get(subj);
          if (candidates != null) result.addAll(candidates);
        }
      }
      return (result);
    }

    /** True if no rules got stocked */
    public boolean isEmpty() {
      return (rel2subj2rules.isEmpty());
    }

    @Override
    public String toString() {
      return rel2subj2rules.toString();
    }
  }

  /** Fact collection containing implication rules */
  protected abstract FactCollection getInputRuleCollection() throws Exception;

  /**
   * How many rules from the rule source can be processed at once Defaults to
   * 0 - no limit
   */
  public int maxRuleSetSize() {
    return 0;
  }

  /** Extract implication rules from input facts */
  protected List<RuleSet> initializeRuleSet() throws Exception {
    FactCollection ruleFacts = getInputRuleCollection();
    List<Rule> allRules = new ArrayList<Rule>();

    for (Fact f : ruleFacts.getFactsWithRelation("<_implies>")) {
      allRules.add(new Rule(f));
    }

    /*
     * If the number of rules processed at once should be limited, we split
     * allRules into a list of RuleSets of the desired size
     */
    List<RuleSet> ruleSets = new ArrayList<RuleSet>();

    int ruleCounter = 0;
    RuleSet rules = new RuleSet();
    for (Rule r : allRules) {
      if (maxRuleSetSize() > 0 && ruleCounter >= maxRuleSetSize()) {
        ruleSets.add(rules);
        rules = new RuleSet();
        ruleCounter = 0;
      }

      rules.add(r);
      ruleCounter++;
    }
    ruleSets.add(rules);
    return ruleSets;
  }

  private Map<String, Map<String, List<Fact>>> loadInputFacts() {
    Map<String, Map<String, List<Fact>>> relationToSubjectToFacts = new HashMap<>();
    for (Theme theme : input()) {
      Announce.doing("Loading ", theme);
      for (Fact fact : theme) {
        relationToSubjectToFacts //
            .computeIfAbsent(fact.getRelation(), k -> new HashMap<>()) //
            .computeIfAbsent(fact.getSubject(), k -> new ArrayList<Fact>(1)) //
            .add(fact);
      }
      Announce.done();
    }
    return relationToSubjectToFacts;
  }

  /** TRUE if the relation of a fact(template) is negated */
  public static boolean isNegated(FactTemplate f) {
    return (f.getRelation().startsWith("-"));
  }

  private void instantiate(Rule r, Map<String, Map<String, List<Fact>>> relationToSubjectToFacts) throws Exception {
    if (r.isReadyToGo()) {
      for (Fact h : r.headFacts()) {
        write(getRULERESULTS(), h, getRULESOURCES(), /* Theme reference? */
            "", "RuleExtractor from " + r.original.toString());
      }
    }

    else {
      FactTemplate firstBody = r.firstBody();
      Boolean relBound = !FactTemplate.isVariable(firstBody.getRelation());
      Boolean subjBound = !FactTemplate.isVariable(firstBody.getArg1());
      Boolean objBound = !FactTemplate.isVariable(firstBody.getArg2());

      Announce.debug("Currently processed rule: ", r);

      // Handle negation
      if (isNegated(firstBody)) {
        // Make sure everybody is bound
        if (!relBound || !subjBound || !objBound) {
          Announce.debug("In order to use negation, all elements have to be bound:", r);
          return;
        }
        // If it is contained in the KB, just return
        String relation = firstBody.getRelation().substring(1);
        if (relationToSubjectToFacts.containsKey(relation)) {
          List<Fact> factsWithSubjectAndRelation = relationToSubjectToFacts.get(relation).getOrDefault(firstBody.getArg1(), Arrays.asList());

          for (Fact f : factsWithSubjectAndRelation) {
            if (f.getObject().equals(firstBody.getArg2())) {
              return;
            }
          }
        }
        // Otherwise continue recursively
        instantiate(r.rest(Collections.<String, String> emptyMap(), null), relationToSubjectToFacts);
        return;
      }

      List<Collection<Fact>> fcs = new ArrayList<>();
      if (relBound) {
        String relation = r.firstBody().getRelation();
        if (relationToSubjectToFacts.containsKey(relation)) {
          if (subjBound) {
            fcs.add(relationToSubjectToFacts.get(relation).getOrDefault(r.firstBody().getArg1(), Arrays.asList()));
          } else {
            fcs.addAll(relationToSubjectToFacts.get(relation).values());
          }
        }
      } else {
        for (String relation : relationToSubjectToFacts.keySet()) {
          if (subjBound) {
            fcs.add(relationToSubjectToFacts.get(relation).getOrDefault(r.firstBody().getArg1(), Arrays.asList()));
          } else {
            fcs.addAll(relationToSubjectToFacts.get(relation).values());
          }
        }
      }

      for (Collection<Fact> fc : fcs) {
        for (Fact f : fc) {
          Map<String, String> map = r.mapFirstTo(f);

          if (map != null) {
            instantiate(r.rest(map, f.getId()), relationToSubjectToFacts);
          }
        }
      }

    }
  }

  @Override
  public void extract() throws Exception {
    List<RuleSet> ruleSets = initializeRuleSet();
    Map<String, Map<String, List<Fact>>> relationToSubjectToFacts = loadInputFacts();

    Announce.doing("Doing a pass on all rules");
    for (RuleSet rules : ruleSets) {
      for (Rule r : rules.allRules()) {
        Announce.doing("Processing the rule: ", r);
        Announce.message("Starting at", NumberFormatter.ISOtime());
        Announce.doing("Doing a pass on all fact themes");
        instantiate(r, relationToSubjectToFacts);
        Announce.done();
        Announce.message("Rule " + r + " finished at", NumberFormatter.ISOtime());
        Announce.done();
      }
    }
    Announce.done();

  }

  // @Override
  //public void extractOld(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
  //  List<RuleSet> ruleSets = initializeRuleSet();
  //
  //  FactCollection allFacts = loadInputFacts();
  //
  //  Announce.debug(ruleSets);
  //  // Loop
  //  for (RuleSet rules : ruleSets) {
  //    Announce.doing("Applying rules");
  //    Announce.debug(rules);
  //    RuleSet survivingRules = new RuleSet();
  //    do {
  //      // Apply all rules
  //      Announce.doing("Doing a pass on all facts");
  //      int factCounter = 0;
  //      for (Fact fact : allFacts) {
  //
  //        factCounter++;
  //        if (factCounter % 10 == 0) Announce.debug("Processed ", factCounter, " facts");
  //
  //        for (Rule r : rules.potentialMatches(fact)) {
  //          Map<String, String> map = r.mapFirstTo(fact);
  //          if (map != null) {
  //            // Announce.debug("Matched", fact, "with", r);
  //            Rule newRule = r.rest(map, fact.getId());
  //            if (newRule.isReadyToGo()) {
  //              for (Fact h : newRule.headFacts()) {
  //                write(getRULERESULTS(), h, getRULESOURCES(), /*
  //                                                             * FactComponent.
  //                                                             * forTheme
  //                                                             * (reader.getKey())
  //                                                             */
  //                    "", "RuleExtractor from " + r.original.toString());
  //              }
  //            } else {
  //              survivingRules.add(newRule);
  //            }
  //          }
  //        }
  //      }
  //      Announce.done();
  //      rules = survivingRules;
  //      survivingRules = new RuleSet();
  //    } while (!rules.isEmpty());
  //    Announce.done();
  //  }
  //}
}
