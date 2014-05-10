package deduplicators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.CategoryMapper;
import fromThemes.CategoryTypeExtractor;
import fromThemes.InfoboxMapper;
import fromThemes.PersonNameExtractor;
import fromThemes.RuleExtractor;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.TemporalInfoboxExtractor;

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
				TemporalInfoboxExtractor.TEMPORALINFOBOXSOURCES,
				FlightIATAcodeExtractor.AIRPORT_CODE_SOURCE));
		input.add(CoordinateExtractor.COORDINATE_SOURCES);
		input.addAll(InfoboxMapper.INFOBOXSOURCES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(CategoryMapper.CATEGORYSOURCES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(CategoryTypeExtractor.CATEGORYTYPESOURCES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
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
