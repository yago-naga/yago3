package fromThemes;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromWikipedia.Extractor;

/**
 * YAGO2s - RelationChecker
 * 
 * Checks whether every relation has domain, range, gloss, and type
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class RelationChecker extends Extractor {

	/** relations that indicate a relation */
	public static final Set<String> relationsAboutRelations = new FinalSet<>(
			RDFS.domain, RDFS.range, RDFS.subpropertyOf);

	public static void check(FactCollection hardFacts) {
		Announce.doing("Checking relations");
		Set<String> relations = new HashSet<>();
		for (Fact f : hardFacts) {
			relations.add(f.getRelation());
			if (relationsAboutRelations.contains(f.getRelation()))
				relations.add(f.getArg(1));
			if (f.getRelation().equals(YAGO.hasGloss)
					&& f.getArg(2).contains("$"))
				relations.add(f.getArg(1));
		}
		Announce.message(relations.size(), "relations");
		for (String relation : relations) {
			for (String req : new String[] { RDFS.domain, RDFS.range,
					RDFS.type, YAGO.hasGloss }) {
				if (hardFacts.getFactsWithSubjectAndRelation(relation, req).isEmpty())
					Announce.warning(relation, "does not have", req);
			}
		}
		Announce.done();
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>();
	}

	@Override
	public void extract() throws Exception {
		check(HardExtractor.HARDWIREDFACTS.factCollection());
	}

	public static void main(String[] args) throws Exception {
		new HardExtractor(new File("../basics2s/data")).extract(new File(
				"c:/fabian/data/yago2s"), "check");
		new RelationChecker().extract(new File("c:/fabian/data/yago2s"),
				"check");
	}
}
