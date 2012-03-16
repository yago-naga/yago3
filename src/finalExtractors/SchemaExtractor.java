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
import extractors.TypeChecker;

/**
 * YAGO2s - SchemaExtractor
 * 
 * Writes the schema
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class SchemaExtractor extends Extractor {

	/** relations that I treat */
	public static final Set<String> SCHEMARELATIONS = new FinalSet<>(RDFS.domain, RDFS.range, RDFS.subpropertyOf);

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS, HardExtractor.HARDWIREDFACTS,
				RuleExtractor.RULERESULTS, InfoboxExtractor.INFOBOXFACTS, PersonNameExtractor.PERSONNAMES, CategoryExtractor.CATEGORYFACTS);
	}

	/** All facts of YAGO */
	public static final Theme YAGOSCHEMA = new Theme("yagoSchema", "The schema of YAGO");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(YAGOSCHEMA);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		for (Theme theme : input.keySet()) {
			Announce.doing("Reading", theme);
			for (Fact fact : input.get(theme)) {
				if (!SCHEMARELATIONS.contains(fact.getRelation()))
					continue;
				output.get(YAGOSCHEMA).write(fact);
			}
			Announce.done();
		}
		Announce.done();
	}

}
