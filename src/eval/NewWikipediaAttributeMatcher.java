package eval;

import fromThemes.AttributeMatcher.CustomAttributeMatcher;
import fromThemes.InfoboxMapper;

/**
 * Matches the attributes of the new Wikipedia to YAGO predicates.
 */
public class NewWikipediaAttributeMatcher extends CustomAttributeMatcher {

	public NewWikipediaAttributeMatcher() {
		super(NewWikipediaTermExtractor.NEWWIKITERMS,
				InfoboxMapper.INFOBOXFACTS.inEnglish(), "new");
	}

}
