package eval;

import java.util.Set;

import utils.Theme;
import extractors.Extractor;
import fromThemes.InfoboxTermExtractor;

/** Extracts the terms from the new Wikipedia */
public class NewWikipediaTermExtractor extends Extractor {

	/** we have a small term extractor who does the work */
	protected InfoboxTermExtractor it;

	public static Theme NEWWIKITERMS=InfoboxTermExtractor.INFOBOXTERMS_TOREDIRECT.inLanguage("new");
	
	@Override
	public Set<Theme> input() {
		return it.input();
	}

	@Override
	public Set<Theme> output() {
		return it.output();
	}

	@Override
	public void extract() throws Exception {
		it.extract();
	}

	public NewWikipediaTermExtractor() {
		super();
		it = new InfoboxTermExtractor("new");
	}
}
