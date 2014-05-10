package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import deduplicators.ClassExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import extractors.Extractor;

/**
 * YAGO2s - SimpleTypeExtractor
 * 
 * Produces a simplified taxonomy of just 3 layers.
 * 
 * @author Fabian M. Suchanek
 * 
 */

public class SimpleTypeExtractor extends Extractor {

	/** Branches of YAGO, order matters! */
	public static final List<String> yagoBranches = Arrays.asList(YAGO.person,
			YAGO.organization, YAGO.building, YAGO.location, YAGO.artifact,
			YAGO.abstraction, YAGO.physicalEntity);

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CoherentTypeExtractor.YAGOTYPES,
				ClassExtractor.YAGOTAXONOMY);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(ClassExtractor.YAGOTAXONOMY);
	}

	/** The theme of simple types */
	public static final Theme SIMPLETYPES = new Theme(
			"yagoSimpleTypes",
			"A simplified rdf:type system. This theme contains all instances, and links them with rdf:type facts to the leaf level of WordNet. Use with yagoSimpleTaxonomy.",
			Theme.ThemeGroup.SIMPLETAX);

	/** Simple taxonomy */
	public static final Theme SIMPLETAXONOMY = new Theme(
			"yagoSimpleTaxonomy",
			"A simplified rdfs:subClassOf taxonomy. This taxonomy contains just WordNet leaves, the main YAGO branches, and "
					+ YAGO.entity + ". Use with " + SIMPLETYPES + ".",
			ThemeGroup.SIMPLETAX);

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(SIMPLETYPES, SIMPLETAXONOMY);
	}

	@Override
	public void extract() throws Exception {
		FactCollection types = new FactCollection();
		FactCollection taxonomy = ClassExtractor.YAGOTAXONOMY.factCollection();
		Set<String> leafClasses = new HashSet<>();
		Announce.doing("Loading YAGO types");
		for (Fact f : CoherentTypeExtractor.YAGOTYPES.factSource()) {
			if (!f.getRelation().equals(RDFS.type))
				continue;
			String clss = f.getArg(2);
			if (clss.startsWith("<wikicategory"))
				clss = taxonomy.getObject(clss, RDFS.subclassOf);
			leafClasses.add(clss);
			types.add(new Fact(f.getArg(1), RDFS.type, clss));
		}
		Announce.done();

		Announce.doing("Writing types");
		for (Fact f : types) {
			SIMPLETYPES.write(f);
		}
		Announce.done();
		types = null;

		Announce.doing("Writing classes");
		for (String branch : yagoBranches) {
			SIMPLETAXONOMY
					.write(new Fact(branch, RDFS.subclassOf, YAGO.entity));
			for (String branch2 : yagoBranches) {
				if (branch != branch2) {
					SIMPLETAXONOMY.write(new Fact(branch, RDFS.disjoint,
							branch2));
				}
			}
		}
		for (String clss : leafClasses) {
			String branch = yagoBranch(clss, taxonomy);
			if (branch == null) {
				// Announce.warning("No branch for", clss);
			} else {
				SIMPLETAXONOMY.write(new Fact(clss, RDFS.subclassOf, branch));
			}
		}
		Announce.done();

	}

	/** returns the super-branch that this class belongs to */
	public static String yagoBranch(String clss, FactCollection taxonomy) {
		Set<String> supr = taxonomy.superClasses(clss);
		for (String b : yagoBranches) {
			if (supr.contains(b))
				return (b);
		}
		return (null);
	}

	public static void main(String[] args) throws Exception {
		new SimpleTypeExtractor()
				.extract(new File("D:/data2/yago2s"), "test\n");
	}

}
