package finalExtractors;

import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.DisambiguationPageExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.PersonNameExtractor;
import extractors.RuleExtractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - LabelExtractor
 * 
 * Writes all labels
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LabelExtractor extends Extractor {

	/** relations that I treat */
	public static final Set<String> LABELRELATIONS = new FinalSet<>(RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>");

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS, HardExtractor.HARDWIREDFACTS,
				RuleExtractor.RULERESULTS, InfoboxExtractor.INFOBOXFACTS, PersonNameExtractor.PERSONNAMES, CategoryExtractor.CATEGORYFACTS, CategoryExtractor.CATEGORYCLASSES, WordnetExtractor.WORDNETWORDS);
	}

	/** All facts of YAGO */
	public static final Theme YAGOLABELS = new Theme("yagoLabels", "All labels of YAGO things");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOLABELS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		for (Theme theme : input.keySet()) {
			Announce.doing("Reading", theme);
			for (Fact fact : input.get(theme)) {
				if (!LABELRELATIONS.contains(fact.getRelation()))
					continue;
				output.get(YAGOLABELS).write(fact);
			}
			Announce.done();
		}
		Announce.done();
	}

}
