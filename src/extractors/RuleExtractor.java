package extractors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactReader;
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
		return new FinalSet<>(PatternHardExtractor.RULES, CategoryExtractor.CATEGORTYPES,
				CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS, InfoboxExtractor.INFOBOXTYPES,
				TypeChecker.CHECKEDINFOBOXFACTS, WordnetExtractor.WORDNETCLASSES);
	}

	/** Theme of deductions */
	public static final Theme RULERESULTS = new Theme("ruleResults");

	@Override
	public Map<Theme, String> output() {
		// TODO Auto-generated method stub
		return null;
	}

	/** represents a rule */
	public static class Rule {
		public final List<FactTemplate> body;
		public final List<FactTemplate> head;
		public final int id;

		public Rule(Fact f) {
			this(FactTemplate.create(f.getArgJavaString(1)), FactTemplate.create(f.getArgJavaString(2)),1);
		}

		public Rule(List<FactTemplate> body, List<FactTemplate> head, int id) {
			this.body = body;
			this.head = head;
			this.id=id;
		}

		public Map<String, String> mapFirstTo(Fact fact) {
			return (body.get(0).mapTo(fact));
		}

		public Rule rest(Map<String, String> map) {
			return (new Rule(FactTemplate.instantiatePartially(head.subList(1, head.size()), map),
					FactTemplate.instantiatePartially(head, map),id+1));
		}

		public boolean isReadyToGo() {
			return (body.isEmpty());
		}

		public List<Fact> headFacts() {
			Map<String, String> empty = new TreeMap<>();
			return (FactTemplate.instantiate(head, empty,id));
		}
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactReader> input) throws Exception {
		FactCollection ruleFacts = new FactCollection(input.get(PatternHardExtractor.RULES));
		List<Rule> rules = new ArrayList<>();
		for (Fact f : ruleFacts.get("<implies>")) {
			rules.add(new Rule(f));
		}
		List<Rule> survivingRules = new ArrayList<>();
		do {
			for (FactReader reader : input.values()) {
				for (Fact fact : reader) {
					for (Rule r : rules) {
						Map<String, String> map = r.mapFirstTo(fact);
						if (map != null) {
							if(fact.getId()!=null) map.put("#"+r.id, fact.getId());
							survivingRules.add(r.rest(map));
						}
					}
				}
			}
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
			survivingRules.clear();
		} while (!rules.isEmpty());
	}

}
