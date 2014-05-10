package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryClassExtractor;

/**
 * YAGO2s - ClassExtractor
 * 
 * Deduplicates all type subclass facts and puts them into the right themes.
 * 
 * This is different from the FactExtractor, because its output is useful for
 * many extractors that deliver input for the FactExtractor.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class ClassExtractor extends SimpleDeduplicator {

	@Override
	public Set<Theme> input() {
		HashSet<Theme> input = new HashSet<Theme>(Arrays.asList(
				HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETCLASSES,
				CategoryClassExtractor.CATEGORYCLASSES
		// GeoNamesClassMapper.GEONAMESCLASSES
				));
		return input;
	}

	/** The YAGO taxonomy */
	public static final Theme YAGOTAXONOMY = new Theme(
			"yagoTaxonomy",
			"The entire YAGO taxonomy. These are all rdfs:subClassOf facts derived from multilingual Wikipedia and from WordNet.",
			ThemeGroup.TAXONOMY);

	@Override
	public Theme myOutput() {
		return YAGOTAXONOMY;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		return fact.getRelation().equals(RDFS.subclassOf);
	}

	public static void main(String[] args) throws Exception {
		new ClassExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
