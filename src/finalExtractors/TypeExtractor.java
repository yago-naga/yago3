package finalExtractors;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - TypeExtractor
 * 
 * Deduplicates all type and subclass facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TypeExtractor extends FactExtractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYTYPES, CategoryExtractor.CATEGORYCLASSES,
				HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETCLASSES, InfoboxExtractor.INFOBOXTYPES);
	}

	/** Final types */
	public static final Theme YAGOTYPES = new Theme("yagoTypes", "Types of YAGO");
	/** The YAGO taxonomy */
	public static final Theme YAGOTAXONOMY = new Theme("yagoTaxonomy", "The entire YAGO taxonomy");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOTAXONOMY, YAGOTYPES);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		for(String relation : Arrays.asList(RDFS.subclassOf,RDFS.type)) {
			Announce.doing("Reading", relation);
			FactCollection facts = new FactCollection();
			for (Theme theme : input.keySet()) {
				Announce.doing("Reading", theme);
				for (Fact fact : input.get(theme)) {
					if (!relation.equals(fact.getRelation()))
						continue;
					facts.add(fact);
				}
				Announce.done();
			}
			Announce.done();
			Announce.doing("Writing", relation);
			FactWriter w = relation.equals(RDFS.subclassOf)?output.get(YAGOTAXONOMY):output.get(YAGOTYPES);
			for (Fact fact : facts)
				w.write(fact);
			Announce.done();
		}
	}

}
