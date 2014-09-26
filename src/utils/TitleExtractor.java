package utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * Extracts Wikipedia title
 * 
 * This tool requires PatternHardExtractor.TITLEPATTERNS and - either
 * WordnetExtractor.WORDNETWORDS - or TransitiveTypeExtractor.TRANSITIVETYPE
 * 
 * It does a profound check whether this entity should become a YAGO entity.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TitleExtractor {

	/** Holds the patterns to apply to titles */
	protected PatternList replacer;

	/** Holds the words of wordnet */
	protected Set<String> wordnetWords;

	/** Holds all entities of Wikipedia */
	protected Set<String> entities;

	/** Language of Wikipedia */
	protected String language;

	/** Constructs a TitleExtractor */
	public TitleExtractor(FactCollection titlePatternFacts,
			Set<String> wordnetWords) {
		replacer = new PatternList(titlePatternFacts, "<_titleReplace>");
		this.wordnetWords = wordnetWords;
	}

	/**
	 * Constructs a TitleExtractor
	 * 
	 * @throws IOException
	 */
	public TitleExtractor(String language) throws IOException {
		if (!PatternHardExtractor.TITLEPATTERNS.isAvailableForReading()) {
			throw new RuntimeException(
					"The TitleExtractor needs PatternHardExtractor.TITLEPATTERNS as input.");
		}
		replacer = new PatternList(
				PatternHardExtractor.TITLEPATTERNS.factCollection(),
				"<_titleReplace>");
		if (FactComponent.isEnglish(language)
				&& !WordnetExtractor.PREFMEANINGS.isAvailableForReading()
				&& !TransitiveTypeExtractor.TRANSITIVETYPE
						.isAvailableForReading()) {
			Announce.error("The English TitleExtractor needs WordnetExtractor.PREFMEANINGS or TransitiveTypeExtractor.TRANSITIVETYPE as input. "
					+ "This is in order to avoid that Wikipedia articles that describe common nouns (such as 'table') become instances in YAGO.");
		}
		if (TransitiveTypeExtractor.TRANSITIVETYPE.isAvailableForReading()) {
			this.entities = TransitiveTypeExtractor.TRANSITIVETYPE
					.factCollection().getSubjects();
		} else if (FactComponent.isEnglish(language)) {
			this.wordnetWords = WordnetExtractor.PREFMEANINGS.factCollection()
					.getPreferredMeanings().keySet();
		}
		this.language = language;
	}

	/** Transforms the entity name to a YAGO entity, returns NULL if bad */
	public String createTitleEntity(String title) {
		title = replacer.transform(title);
		if (title == null)
			return (null);
		if (wordnetWords != null && wordnetWords.contains(title.toLowerCase()))
			return (null);
		String entity = FactComponent.forForeignYagoEntity(title, language);
		if (entities != null && !entities.contains(entity))
			return (null);
		return (entity);
	}

	/** Reads the title entity, supposes that the reader is after "<title>" */
	public String getTitleEntity(Reader in) throws IOException {
		String title = FileLines.readToBoundary(in, "</title>");
		title = Char17.decodeAmpersand(title);
		return (createTitleEntity(title));
	}
}
