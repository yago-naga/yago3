package fromThemes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import fromWikipedia.CategoryMapper;
import fromWikipedia.CategoryTypeExtractor;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.InfoboxMapper;
import fromWikipedia.PersonNameExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import fromWikipedia.WikidataLabelExtractor;

/**
 * YAGO2s - SourceExtractor
 * 
 * Deduplicates all source facts. This extractor is different from FactExtractor
 * so that it can run in parallel.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class SourceExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		Set<Theme> input = new HashSet<Theme>(Arrays.asList(
				PersonNameExtractor.PERSONNAMESOURCES,
				RuleExtractor.RULESOURCES,
				WikidataLabelExtractor.WIKIPEDIALABELSOURCES,
				FlightExtractor.FLIGHTSOURCE,
				CoordinateExtractor.COORDINATE_SOURCES,
				TemporalInfoboxExtractor.TEMPORALINFOBOXSOURCES,
				FlightIATAcodeExtractor.AIRPORT_CODE_SOURCE));
		input.addAll(InfoboxMapper.INFOBOXSOURCES.inAllLanguages());
		input.addAll(CategoryMapper.CATEGORYSOURCES.inAllLanguages());
		input.addAll(CategoryTypeExtractor.CATEGORYTYPESOURCES.inAllLanguages());
		return input;
	}

	/** All source facts of YAGO */
	public static final Theme YAGOSOURCES = new Theme("yagoSources",
			"All sources of YAGO facts", ThemeGroup.META);

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOSOURCES);
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Extracting sources");
		for (Theme theme : input()) {
			Announce.doing("Extracting sources from", theme);
			for (Fact fact : theme.factSource()) {
				if (fact.getRelation().equals(YAGO.extractionSource)
						|| fact.getRelation().equals(YAGO.extractionTechnique)) {
					YAGOSOURCES.write(fact);
				}
			}
			Announce.done();
		}
		Announce.done();
	}
}
