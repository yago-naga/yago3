package extractorUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.Theme;
import extractors.PatternHardExtractor;
import extractors.WordnetExtractor;

/**
 * Extracts Wikipedia title
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TitleExtractor {

	/** Holds the patterns to apply to titles*/
	protected PatternList replacer;

	/** Holds the words of wordnet*/
	protected Set<String> wordnetWords;

	/** Constructs a TitleExtractor*/
	public TitleExtractor(FactCollection titlePatternFacts, Set<String> wordnetWords) {
		replacer=new PatternList(titlePatternFacts, "<_titleReplace>");
		this.wordnetWords=wordnetWords;
	}	

	/** Constructs a TitleExtractor
	 * @throws IOException */
	public TitleExtractor(Map<Theme,FactSource> input) throws IOException {
		if(input.get(PatternHardExtractor.TITLEPATTERNS)==null || input.get(WordnetExtractor.WORDNETWORDS)==null) {
			Announce.error("The TitleExtractor needs PatternHardExtractor.TITLEPATTERNS and WordnetExtractor.WORDNETWORDS as input."+
		"This is in order to avoid that Wikipedia articles that describe common nouns (such as 'table') become instances in YAGO.");
		}
		replacer=new PatternList(input.get(PatternHardExtractor.TITLEPATTERNS), "<_titleReplace>");
		this.wordnetWords=WordnetExtractor.preferredMeanings(new FactCollection(input.get(WordnetExtractor.WORDNETWORDS))).keySet();
	}

	/** Reads the title entity, supposes that the reader is after "<title>" */
	public String getTitleEntity(Reader in) throws IOException {
		String title = FileLines.readToBoundary(in, "</title>");
		title=replacer.transform(title);
		if(title==null) return(null);
		if(wordnetWords.contains(title.toLowerCase())) return(null);
		return (FactComponent.forYagoEntity(title.replace(' ', '_')));
	}
}
