package extractorUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javatools.filehandlers.FileLines;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactReader;
import basics.Theme;
import extractors.PatternHardExtractor;

/**
 * Extracts Wikipedia title
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TitleExtractor {

	/** Holds the patterns to apply to titles*/
	protected PatternList replacer;
	
	public TitleExtractor(FactCollection titlePatternFacts) {
		replacer=new PatternList(titlePatternFacts, "<_titleReplace>");
	}
	
	public TitleExtractor(Map<Theme,FactReader> factCollections) throws IOException {
		this(new FactCollection(factCollections.get(PatternHardExtractor.TITLEPATTERNS)));
	}
	
	/** Reads the title entity, supposes that the reader is after "<title>" */
	public String getTitleEntity(Reader in) throws IOException {
		String title = FileLines.readToBoundary(in, "</title>");
		title=replacer.transform(title);
		if(title==null) return(null);
		return (FactComponent.forYagoEntity(title.replace(' ', '_')));
	}
}
