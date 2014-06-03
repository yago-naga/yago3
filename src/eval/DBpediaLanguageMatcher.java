package eval;

import fromThemes.AttributeMatcher.CustomAttributeMatcher;
import fromThemes.InfoboxTermExtractor;

/** Matches German infobox terms to DBpedia properties */
public class DBpediaLanguageMatcher extends CustomAttributeMatcher {

	public DBpediaLanguageMatcher() {
		super(InfoboxTermExtractor.INFOBOXTERMSTRANSLATED.inLanguage("de"),
				DbpediaExtractor.DBPEDIAFACTS, "dde");
	}

}
