package retired;

import java.io.File;
import java.util.Set;

import utils.Theme;
import extractors.DataExtractor;
import fromWikipedia.InfoboxExtractor;

/**
 * Extracts from the new wikipedia. This has to be a data extractor, because we
 * want to call it without introducing a new language.
 */
public class NewWikipediaInfoboxExtractor extends DataExtractor {

	/** We have a small infobx extractor that does the work */
	protected final InfoboxExtractor infex;

	/** Our output theme */
	public static Theme NEWWIKIPEDIAATTRIBUTES = InfoboxExtractor.INFOBOX_ATTRIBUTES
			.inLanguage("new");

	@Override
	public Set<Theme> input() {
		return infex.input();
	}

	@Override
	public Set<Theme> output() {
		return infex.output();
	}

	@Override
	public Set<Theme> inputCached() {
		return infex.inputCached();
	}

	public NewWikipediaInfoboxExtractor(File input) {
		super(input);
		infex = new InfoboxExtractor("new", input);
	}

	@Override
	public void extract() throws Exception {
		infex.extract();
	}

}
