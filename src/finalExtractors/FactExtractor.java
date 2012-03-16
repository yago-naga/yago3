package finalExtractors;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.RuleExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all facts
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS,
				InfoboxExtractor.INFOBOXFACTS);
	}

	/** All facts of YAGO */
	public static final Theme YAGOFACTS = new Theme("yagoFacts", "All instance facts of YAGO");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Set<String> relationsToDo = new TreeSet<>();
		relationsToDo.add("<wasBornOnDate>");
		Set<String> relationsDone = new TreeSet<>();
		while (!relationsToDo.isEmpty()) {
			String relation = D.pick(relationsToDo);
			relationsToDo.remove(relation);
			relationsDone.add(relation);
			Announce.doing("Reading", relation);
			FactCollection facts = new FactCollection();
			for (Theme theme : input.keySet()) {
				Announce.doing("Reading", theme);
				for (Fact fact : input.get(theme)) {
					if (!relationsDone.contains(fact.getRelation())
							&& !LabelExtractor.LABELRELATIONS.contains(fact.getRelation())
							&& !SchemaExtractor.SCHEMARELATIONS.contains(fact.getRelation())
							&& !fact.getRelation().startsWith("<_") && !fact.getRelation().equals(RDFS.type)&& !fact.getRelation().equals(RDFS.subclassOf))
						relationsToDo.add(fact.getRelation());
					if (!relation.equals(fact.getRelation()))
						continue;
					facts.add(fact);
				}
				Announce.done();
			}
			Announce.done();
			Announce.doing("Writing", relation);
			for (Fact fact : facts)
				output.get(YAGOFACTS).write(fact);
			Announce.done();			
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new FactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
	}
}
