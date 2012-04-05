package finalExtractors;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - SimpleTaxonomyExtractor
 * 
 * Extracts a simple taxonomy of just Wikicategories, wordnet leaves and main
 * branches
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class SimpleTaxonomyExtractor extends Extractor {

	/** Branches of YAGO, order matters! */
	public static final List<String> yagoBranches = new FinalSet<>(YAGO.person, YAGO.organization, YAGO.artifact,
			YAGO.location, YAGO.abstraction, YAGO.physicalEntity);

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYCLASSES, WordnetExtractor.WORDNETCLASSES);
	}

	/** Simple taxonomy */
	public static final Theme SIMPLETAXONOMY = new Theme(
			"yagoSimpleTaxonomy",
			"A subset of the YAGO taxonomy, which contains just Wikipedia categories,\n WordNet leaves, the main YAGO branches, and "
					+ YAGO.entity + ".");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(SIMPLETAXONOMY);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		FactCollection wordnet = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
		Set<String> done = new HashSet<>();
		Announce.doing("Extracting simple taxonomy");
		for (String b : yagoBranches) {
			output.get(SIMPLETAXONOMY).write(new Fact(b, RDFS.subclassOf, YAGO.entity));
		}
		for (Fact f : input.get(CategoryExtractor.CATEGORYCLASSES)) {
			if (!f.getRelation().equals(RDFS.subclassOf))
				continue;
			if (done.contains(f.getArg(1)))
				continue;
			done.add(f.getArg(1));
			if (done.contains(f.getArg(2))) {
				output.get(SIMPLETAXONOMY).write(f);
				continue;
			}
			String superBranch = yagoBranch(f.getArg(2), wordnet);
			if (superBranch == null)
				continue;
			output.get(SIMPLETAXONOMY).write(f);
			output.get(SIMPLETAXONOMY).write(new Fact(f.getArg(2), RDFS.subclassOf, superBranch));
			done.add(f.getArg(2));
		}
		Announce.done();
	}

	/** returns the super-branch that this wordnet class belongs to */
	public static String yagoBranch(String wordnetclass, FactCollection wordnetClasses) {
		Set<String> supr = wordnetClasses.superClasses(wordnetclass);
		for (String b : yagoBranches) {
			if (supr.contains(b))
				return (b);
		}
		return (null);
	}

	public static void main(String[] args) throws Exception {
		new SimpleTaxonomyExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
