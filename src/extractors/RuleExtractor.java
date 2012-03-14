package extractors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractorUtils.FactTemplate;

/**
 * YAGO2s - RuleExtractor
 * 
 * Generates the results of rules.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class RuleExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(PatternHardExtractor.RULES, CategoryExtractor.CATEGORYTYPES,
				CategoryExtractor.CATEGORYCLASSES, CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS,
				InfoboxExtractor.INFOBOXTYPES, TypeChecker.CHECKEDINFOBOXFACTS, WordnetExtractor.WORDNETCLASSES);
	}

	/** Theme of deductions */
	public static final Theme RULERESULTS = new Theme("ruleResults", "Results of rule applications");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(RULERESULTS);
	}

	/** Represents a rule */
	public static class Rule {
		/** Conditions of the rule */
		public final List<FactTemplate> body;
		/** Consequences of the rule */
		public final List<FactTemplate> head;
		/** Reference of the current fact */
		public final int reference;

		/** Creates a rule from an implies-fact */
		public Rule(Fact f) {
			this(FactTemplate.create(f.getArgJavaString(1)), FactTemplate.create(f.getArgJavaString(2)), 1);
		}

		/** creates a rule with conditions and consequences */
		public Rule(List<FactTemplate> body, List<FactTemplate> head, int reference) {
			this.body = body;
			this.head = head;
			this.reference = reference;
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
			if (id != null)
				refMap.put("#" + reference, id);
			return (new Rule(FactTemplate.instantiatePartially(body.subList(1, body.size()), map),
					FactTemplate.instantiatePartially(head, map), reference + 1));
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
			if (FactTemplate.isVariable(value))
				return ("$");
			else
				return (value);
		}

		/** Adds a rule */
		public void add(Rule r) {
			String rel = index(r.firstBody().relation);
			Map<String, List<Rule>> map = rel2subj2rules.get(rel);
			if (map == null)
				rel2subj2rules.put(rel, map = new TreeMap<String, List<Rule>>());
			String subj = index(r.firstBody().arg1);
			D.addKeyValue(map, subj, r, ArrayList.class);
		}

		/** Returns rules whose first body atom matches potentially */
		public List<Rule> potentialMatches(Fact f) {
			List<Rule> result = new ArrayList<>();
			for (String rel : Arrays.asList("$", f.getRelation())) {
				Map<String, List<Rule>> map = rel2subj2rules.get(rel);
				if (map == null)
					continue;
				for (String subj : Arrays.asList("$", f.getArg(1))) {
					List<Rule> candidates = map.get(subj);
					if (candidates != null)
						result.addAll(candidates);
				}
			}
			return (result);
		}

		/** True if no rules got stocked */
		public boolean isEmpty() {
			return (rel2subj2rules.isEmpty());
		}
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Initialize
		FactCollection ruleFacts = new FactCollection(input.get(PatternHardExtractor.RULES));
		RuleSet rules = new RuleSet();
		for (Fact f : ruleFacts.get("<_implies>")) {
			rules.add(new Rule(f));
		}

		// Loop
		Announce.doing("Applying rules");
		RuleSet survivingRules = new RuleSet();
		do {
			// Apply all rules
			Announce.doing("Doing a pass on all facts");
			for (Entry<Theme, FactSource> reader : input.entrySet()) {
				Announce.doing("Reading", reader.getKey());
				for (Fact fact : reader.getValue()) {
					for (Rule r : rules.potentialMatches(fact)) {
						Map<String, String> map = r.mapFirstTo(fact);
						if (map != null) {
							Rule newRule = r.rest(map, fact.getId());
							if (newRule.isReadyToGo()) {
								for (Fact h : newRule.headFacts()) {
									output.get(RULERESULTS).write(h);
								}
							} else {
								survivingRules.add(newRule);
							}
						}
					}
				}
				Announce.done();
			}
			Announce.done();
			rules = survivingRules;
			survivingRules = new RuleSet();
		} while (!rules.isEmpty());
		Announce.done();
	}

	public static void main(String[] args) throws Exception {
	   //new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
		Announce.setLevel(Announce.Level.DEBUG);
		new RuleExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
