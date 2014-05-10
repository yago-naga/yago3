package extractors;

import java.util.Arrays;
import java.util.List;

import javatools.administrative.Announce;

/**
 * MultilingualExtractor - Yago2s
 * 
 * Superclass of all multilingual extractors. This class determines which
 * Wikipedia versions can be read!
 * 
 * By convention, all subclasses have a constructor that takes as argument the
 * language.
 * 
 * @author Fabian
 * 
 */

public abstract class MultilingualExtractor extends Extractor {

	/** List of language suffixes from most English to least English. */
	public static List<String> wikipediaLanguages = Arrays.asList("en", "de",
			"fr", "es", "it");

	/** The language of this extractor */
	public final String language;

	@Override
	public String name() {
		return (this.getClass().getName() + "(" + this.language + ")");
	}

	public MultilingualExtractor(String lan) {
		this.language = lan;
	}

	/** Creates an extractor given by name */
	public static Extractor forName(Class<MultilingualExtractor> className,
			String language) {
		Announce.doing("Creating extractor", className + "(" + language + ")");
		if (language == null) {
			Announce.error("Language is null");
		}
		Extractor extractor=null;
		try {
			extractor = className.getConstructor(String.class).newInstance(
					language);
		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

}
