package extractors;

import java.io.File;

import javatools.administrative.Announce;

/**
 * YAGO2s - MultilingualDataExtractor
 * 
 * An extractor that extracts from a single source but exists in different
 * languages.
 * 
 * By convention, these classes have to have a constructor of with two
 * arguments: language and data file
 * 
 * @author Fabian
 * 
 */

public abstract class MultilingualDataExtractor extends MultilingualExtractor {

	/** Data file */
	protected final File inputData;

	public MultilingualDataExtractor(String lan, File input) {
		super(lan);
		inputData = input;
	}

	/** Creates an extractor given by name */
	public static Extractor forName(Class<MultilingualDataExtractor> className,
			String language, File input) {
		Announce.doing("Creating extractor", className + "(" + language + ", "
				+ input + ")");
		Extractor extractor = null;
		try {
			extractor = className.getConstructor(String.class, File.class)
					.newInstance(language, input);
		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

}
