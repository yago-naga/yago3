package utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;


import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TransitiveTypeExtractor;


import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.Theme;

/**
 * Extracts Wikipedia title
 * 
 * This tool requires PatternHardExtractor.TITLEPATTERNS and 
 * - either WordnetExtractor.WORDNETWORDS
 * - or TransitiveTypeExtractor.TRANSITIVETYPE
 * 
 * It does a profound check whether this entity should become a YAGO entity.
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TitleExtractor {

  /** Holds the patterns to apply to titles*/
  protected PatternList replacer;

  /** Holds the words of wordnet*/
  protected Set<String> wordnetWords;

  /** Holds all entities of Wikipedia */
  protected Set<String> entities;

  /** Constructs a TitleExtractor*/
  public TitleExtractor(FactCollection titlePatternFacts, Set<String> wordnetWords) {
    replacer = new PatternList(titlePatternFacts, "<_titleReplace>");
    this.wordnetWords = wordnetWords;
  }

  /** Constructs a TitleExtractor
   * @throws IOException */
  public TitleExtractor(Map<Theme, FactSource> input) throws IOException {
    if (input.get(PatternHardExtractor.TITLEPATTERNS) == null) {
      Announce.error("The TitleExtractor needs PatternHardExtractor.TITLEPATTERNS as input.");
    }
    if (input.get(WordnetExtractor.WORDNETWORDS) == null && input.get(TransitiveTypeExtractor.TRANSITIVETYPE) == null) {
      Announce.error("The TitleExtractor needs WordnetExtractor.WORDNETWORDS or TransitiveTypeExtractor.TRANSITIVETYPE as input."
          + "This is in order to avoid that Wikipedia articles that describe common nouns (such as 'table') become instances in YAGO.");
    }
    replacer = new PatternList(input.get(PatternHardExtractor.TITLEPATTERNS), "<_titleReplace>");
    if (input.get(TransitiveTypeExtractor.TRANSITIVETYPE) != null) {
      this.entities=TransitiveTypeExtractor.entities(input);
    } else {
      this.wordnetWords = WordnetExtractor.preferredMeanings(new FactCollection(input.get(WordnetExtractor.WORDNETWORDS))).keySet();
    }
  }

  /** Reads the title entity, supposes that the reader is after "<title>" */
  public String getTitleEntity(Reader in) throws IOException {
    String title = FileLines.readToBoundary(in, "</title>");
    title = replacer.transform(Char.decodeAmpersand(title));
    if (title == null) return (null);
    if (wordnetWords!=null && wordnetWords.contains(title.toLowerCase())) return (null);
    String entity=FactComponent.forYagoEntity(title.replace(' ', '_'));
    if(entities!=null && !entities.contains(entity)) return(null);
    return(entity);
  }
  
}
