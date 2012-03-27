package extractors;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import extractorUtils.FactTemplateExtractor;
import extractorUtils.TitleExtractor;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * TemporalCategoryExtractor - YAGO2s
 * 
 * Extract temporal facts from categories. It uses the patterns
 * /data/_categoryTemporalPatterns.ttl for the extraction.
 * 
 * @author Erdal Kuzey
 * 
 */
public class TemporalCategoryExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new TypeChecker(
				DIRTYCATEGORYFACTS, CATEGORYFACTS)));
	}

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.TEMPORALCATEGORYPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS, HardExtractor.HARDWIREDFACTS));
	}

	/** Facts deduced from categories */
	public static final Theme DIRTYCATEGORYFACTS = new Theme(
			"categoryTemporalFactsDirty",
			"Facts derived from the categories - still to be type checked");
	/** Facts deduced from categories */
	public static final Theme CATEGORYFACTS = new Theme(
			"categoryTemporalFacts", "Facts derived from the categories");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYCATEGORYFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers,
			Map<Theme, FactSource> input) throws IOException {
		FactCollection categoryPatternCollection = new FactCollection(
				input.get(PatternHardExtractor.TEMPORALCATEGORYPATTERNS));
		FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(
				categoryPatternCollection, "<_categoryPattern>");
		TitleExtractor titleExtractor = new TitleExtractor(input);
		Announce.progressStart("Extracting", 3_900_000);
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		FactCollection facts = new FactCollection();
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:")) {
			case -1:
				flush(titleEntity, facts, writers);
				Announce.progressDone();
				in.close();
				return;
			case 0:
				Announce.progressStep();
				flush(titleEntity, facts, writers);
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
				if (titleEntity == null)
					continue;
				String category = FileLines.readTo(in, "]]").toString();
				if (!category.endsWith("]]"))
					continue;
				category = category.substring(0, category.length() - 2);
				for (Fact fact : categoryPatterns
						.extract(category, titleEntity)) {
					if (fact != null)
						facts.add(fact);
				}
			}
		}

	}

	/** Writes the facts */
	public static void flush(String entity, FactCollection facts,
			Map<Theme, FactWriter> writers) throws IOException {
		if (entity == null)
			return;

		for (Fact fact : facts) {
			writers.get(DIRTYCATEGORYFACTS).write(fact);
		}
		facts.clear();
	}

	public TemporalCategoryExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}
}
