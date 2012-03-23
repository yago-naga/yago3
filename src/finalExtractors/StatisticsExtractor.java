package finalExtractors;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import extractors.Extractor;
import extractors.WordnetExtractor;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;

/**
 * YAGO2s - StatisticsExtractor
 * 
 * Extracts statistics about YAGO themes etc.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class StatisticsExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(TypeExtractor.YAGOTAXONOMY, TypeExtractor.YAGOTYPES, FactExtractor.YAGOFACTS,
				FactExtractor.YAGOLABELS, FactExtractor.YAGOMETAFACTS, FactExtractor.YAGOSCHEMA,FactExtractor.YAGOSOURCES, WordnetExtractor.WORDNETIDS);
	}

	/** YAGO statistics theme */
	public static final Theme STATISTICS = new Theme("yagoStatistics", "Statistics about YAGO");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(STATISTICS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Map<String, Integer> relations = new TreeMap<>();
		Set<String> instances = new TreeSet<>();
		Map<String, Integer> classes = new TreeMap<>();
		FactWriter out = output.get(STATISTICS);
		Announce.doing("Making YAGO statistics");
		for (Theme t : input.keySet()) {
			Announce.doing("Analyzing", t);
			int counter = 0;
			for (Fact f : input.get(t)) {
				counter++;
				if ((f.getRelation().equals(RDFS.domain) || f.getRelation().equals(RDFS.range))
						&& !relations.containsKey(f.getArg(1)))
					relations.put(f.getArg(1), 0);
				D.addKeyValue(relations, f.getRelation(), 1);
				if (f.getRelation().equals(RDFS.type)) {
					instances.add(f.getArg(1));
					D.addKeyValue(classes, f.getArg(2), 1);
				}
				if (f.getRelation().equals(RDFS.subclassOf)) {
					D.addKeyValue(classes, f.getArg(2), 0);
					D.addKeyValue(classes, f.getArg(1), 0);
				}
			}
			out.write(new Fact(FactComponent.forTheme(t), YAGO.hasNumber, FactComponent
					.forNumber(counter)));
			Announce.done();
		}
		Announce.doing("Writing results");
		for (String rel : relations.keySet()) {
			if (relations.get(rel) == 0)
				Announce.warning("Relation without facts:", rel);
			else
				out.write(new Fact(rel, YAGO.hasNumber, FactComponent.forNumber(relations.get(rel))));
		}
		for (String cls : classes.keySet()) {
			if (classes.get(cls) > 0)
				out.write(new Fact(cls, YAGO.hasNumber, FactComponent.forNumber(classes.get(cls))));
		}
		Announce.done();
		Announce.message(instances.size(), "things");
		Announce.message(classes.size(), "classes");
		out.write(new Fact(YAGO.yago, FactComponent.forYagoEntity("hasNumberOfThings"), FactComponent
				.forNumber(instances.size())));
		out.write(new Fact(YAGO.yago, FactComponent.forYagoEntity("hasNumberOfClasses"), FactComponent
				.forNumber(classes.size())));
		Announce.done();
	}

	public static void main(String[] args) throws Exception {
		new StatisticsExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
