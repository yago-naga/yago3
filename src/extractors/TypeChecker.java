package extractors;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactReader;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;

public class TypeChecker extends Extractor {

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(InfoboxExtractor.DIRTYINFOBOXFACTS, HardExtractor.HARDWIREDFACTS,
				WordnetExtractor.WORDNETCLASSES, CategoryExtractor.CATEGORTYPES));
	}

	/** The output of this extractor */
	public static final Theme CHECKEDINFOBOXFACTS = new Theme("checkedInfoboxFacts");

	@Override
	public Map<Theme, String> output() {
		return new FinalMap<>(CHECKEDINFOBOXFACTS, "The facts extracted from the infoboxes, checked for types");
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactReader> input) throws Exception {
		FactCollection types = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
		types.load(input.get(CategoryExtractor.CATEGORTYPES));
		types.load(input.get(HardExtractor.HARDWIREDFACTS));
		FactWriter out = output.get(CHECKEDINFOBOXFACTS);
		Announce.doing("Type checking facts");
		for (Fact fact : input.get(InfoboxExtractor.DIRTYINFOBOXFACTS)) {
			if (FactComponent.isLiteral(fact.getArg(2))) {
				out.write(fact);
				continue;
			}
			String domain = types.getArg2(fact.getRelation(), RDFS.domain);
			if (!check(fact.getArg(1), domain, types)) {
				Announce.debug("Domain check failed", fact);
				continue;
			}
			String range = types.getArg2(fact.getRelation(), RDFS.range);
			if (check(fact.getArg(2), range, types))
				out.write(fact);
			else
				Announce.debug("Range check failed", fact);
		}
		Announce.done();
	}

	/** Checks whether an entity is of a type */
	protected boolean check(String entity, String type, FactCollection types) {
		if (type == null)
			type = YAGO.entity;
		return (types.instanceOf(entity, type));
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new TypeChecker().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
