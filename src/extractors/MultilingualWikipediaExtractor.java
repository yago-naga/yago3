package extractors;

import java.io.File;

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
		MultilingualDataExtractor {

	public MultilingualWikipediaExtractor(String lan, File wikipedia) {
		super(lan, wikipedia);
	}

}
