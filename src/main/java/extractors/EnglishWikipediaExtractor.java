package extractors;

import java.io.File;

/**
 * Superclass of all extractors that extract from the English Wikipedia
 * 
 * @author Fabian
 * 
 */
public abstract class EnglishWikipediaExtractor extends DataExtractor {

	protected File wikipedia() {
		return (inputData);
	}

	public EnglishWikipediaExtractor(File wikipedia) {
		super(wikipedia);
	}
}
