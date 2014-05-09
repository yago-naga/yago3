package fromWikipedia;

/**
 * MulitlingualExtractor - Yago2s
 * 
 * Superclass of all mulitlingual extractors.
 * 
 * @author Fabian
 * 
 */

public abstract class MultilingualExtractor extends Extractor {

	/** The language of this extractor */
	public String language = null;

	@Override
	public String name() {
		return (this.getClass().getName() + "(" + this.language + ")");
	}
}
