package finalExtractors;

import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - Extracts the final YAGO taxonomy
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TaxonomyExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYCLASSES, WordnetExtractor.WORDNETCLASSES,
				WordnetExtractor.WORDNETWORDS);
	}

	/** The YGAO taxonomy */
	public static final Theme YAGOTAXONOMY = new Theme("yagoTaxonomy", "The entire YAGO taxonomy");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOTAXONOMY);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Announce.doing("Reading class facts");
		for (Theme theme : input.keySet()) {
			Announce.doing("Reading", theme);
			for (Fact f : input.get(theme)) {
				output.get(YAGOTAXONOMY).write(f);
			}
			Announce.done();
		}
		Announce.done();
	}

}
