package extractors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
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
		return new FinalSet<>(PatternHardExtractor.RULES, CategoryExtractor.CATEGORYTYPES,CategoryExtractor.CATEGORYCLASSES,
				CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS, InfoboxExtractor.INFOBOXTYPES,
				TypeChecker.CHECKEDINFOBOXFACTS, WordnetExtractor.WORDNETCLASSES);
	}

	/** Theme of deductions */
	public static final Theme RULERESULTS = new Theme("ruleResults");

	@Override
	public Map<Theme, String> output() {
		return new FinalMap<>(RULERESULTS,"Results of rule applications");
	}

	/** Represents a rule */
	public static class Rule {
		/** Conditions of the rule */
		public final List<FactTemplate> body;
		/** Consequences of the rule */
		public final List<FactTemplate> head;
        /** Reference of the current fact*/
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

		/** Removes first body atom, maps remainder 
		 * @param string */
		public Rule rest(Map<String, String> map, String id) {
			Map<String,String> refMap=new TreeMap<>(map);
			if(id!=null) refMap.put("#"+reference,id);
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
			return body+"=>"+head;
		}
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Initialize
		FactCollection ruleFacts = new FactCollection(input.get(PatternHardExtractor.RULES));
		List<Rule> rules = new ArrayList<>();
		for (Fact f : ruleFacts.get("<_implies>")) {
			rules.add(new Rule(f));
		}

		// Loop
		Announce.doing("Applying rules");
		List<Rule> survivingRules = new ArrayList<>();
		do {
			// Apply all rules
			Announce.doing("Doing a pass on all facts");
			for (Entry<Theme, FactSource> reader : input.entrySet()) {
				Announce.doing("Reading", reader.getKey());
				for (Fact fact : reader.getValue()) {
					for (Rule r : rules) {
						Map<String, String> map = r.mapFirstTo(fact);
						if (map != null) {
							Announce.debug("Instantiating",r);
							Announce.debug("with",map);
							Announce.debug("yields",r.rest(map,fact.getId()));
							survivingRules.add(r.rest(map,fact.getId()));
						}
					}
				}
				Announce.done();
			}
			Announce.done();

			// See which ones got instantiated
			Announce.doing("Checking rules");
			Announce.message("Rules before:", rules.size());
			Announce.message("Rules after:", survivingRules.size());
			rules.clear();
			for (Rule r : survivingRules) {
				if (r.isReadyToGo()) {
					for (Fact h : r.headFacts()) {
						output.get(RULERESULTS).write(h);
					}
				} else {
					rules.add(r);
				}
			}
			Announce.done();
			survivingRules.clear();
		} while (!rules.isEmpty());
		Announce.done();
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
       new RuleExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
