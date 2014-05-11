package extractors;

import java.io.File;

import javatools.administrative.Announce;

/**
 * YAGO2s - MultilingualWikipediaExtractor
 * 
 * An extractor that extracts from Wikipedia in different languages.
 * 
 * By convention, these classes have to have a constructor of with two
 * arguments: language and wikipedia
 * 
 * @author Fabian
 * 
 */
public abstract class MultilingualWikipediaExtractor extends
		MultilingualExtractor {

	/** Data file */
	protected final File wikipedia;

	public MultilingualWikipediaExtractor(String lan, File wikipedia) {
		super(lan);
		this.wikipedia = wikipedia;
	}

	/** Creates an extractor with a given name */
	public static Extractor forName(
			Class<MultilingualWikipediaExtractor> className, String language,
			File wikipedia) {
		Announce.doing("Creating extractor", className + "(" + language + ")");
		if (language == null) {
			throw new RuntimeException("Language is null");
		}
		Extractor extractor = null;
		try {
			extractor = className.getConstructor(String.class, File.class)
					.newInstance(language, wikipedia);
		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

}
