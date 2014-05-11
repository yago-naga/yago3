package fromThemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import deduplicators.ClassExtractor;
import extractors.Extractor;

/**
 * YAGO2s - TransitiveTypeExtractor
 * 
 * Extracts all transitive rdf:type facts.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TransitiveTypeExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(ClassExtractor.YAGOTAXONOMY,
				CoherentTypeExtractor.YAGOTYPES);
	}

	/** All type facts */
	public static final Theme TRANSITIVETYPE = new Theme("yagoTransitiveType",
			"Transitive closure of all rdf:type/rdfs:subClassOf facts",
			ThemeGroup.TAXONOMY);

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(TRANSITIVETYPE);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(ClassExtractor.YAGOTAXONOMY);
	}

	@Override
	public void extract() throws Exception {
		FactCollection classes = ClassExtractor.YAGOTAXONOMY.factCollection();
		Map<String, Set<String>> yagoTaxonomy = new HashMap<>();
		Announce.doing("Computing the transitive closure");
		for (Fact f : CoherentTypeExtractor.YAGOTYPES) {
			if (f.getRelation().equals(RDFS.type)) {
				D.addKeyValue(yagoTaxonomy, f.getArg(1), f.getArg(2),
						TreeSet.class);
				for (String c : classes.superClasses(f.getArg(2))) {
					D.addKeyValue(yagoTaxonomy, f.getArg(1), c, TreeSet.class);
				}
			}
		}
		Announce.done();
		Announce.doing("Writing data");
		for (Entry<String, Set<String>> type : yagoTaxonomy.entrySet()) {
			for (String c : type.getValue()) {
				Fact f = new Fact(type.getKey(), RDFS.type, c);
				f.makeId();
				TRANSITIVETYPE.write(f);
			}
		}
		Announce.done();
		Announce.done();
	}

	public static void main(String[] args) throws Exception {
		new TransitiveTypeExtractor().extract(new File("D:/data2/yago2s"),
				"test");
	}
}
