package finalExtractors;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.InfoboxExtractor;

/**
 * YAGO2s - TypeExtractor
 * 
 * Merges all type data into one type file
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TypeExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(InfoboxExtractor.INFOBOXTYPES, CategoryExtractor.CATEGORYTYPES);
	}

	/** Final types */
	public static final Theme YAGOTYPES = new Theme("yagoTypes", "Types of YAGO");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOTYPES);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Map<String, Set<String>> types = new TreeMap<>();
		Announce.doing("Reading type facts");
		for (Theme theme : input.keySet()) {
			Announce.doing("Reading", theme);
			for (Fact f : input.get(theme)) {
				if (!f.getRelation().equals(RDFS.type))
					continue;
				D.addKeyValue(types, f.getArg(1), f.getArg(2), TreeSet.class);
			}
			Announce.done();
		}
		Announce.done();
		Announce.doing("Writing type facts");
		for (String entity : types.keySet()) {
			for (String clss : types.get(entity)) {
				output.get(YAGOTYPES).write(new Fact(entity, RDFS.type, clss));
			}
		}
		Announce.done();
	}

}
