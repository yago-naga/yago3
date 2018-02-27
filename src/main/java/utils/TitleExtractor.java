/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

package utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import basics.FactComponent;
import extractors.Extractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CoherentTypeExtractor;
import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;

/**
 * Extracts Wikipedia title
 *
 * This tool requires PatternHardExtractor.TITLEPATTERNS and - either
 * WordnetExtractor.WORDNETWORDS - or CoherentTypeExtractor.YAGOTYPES
 *
 * It does a profound check whether this entity should become a YAGO entity.
 *
*/

public class TitleExtractor {

  /** Holds the patterns to apply to titles */
  protected PatternList replacer;

  /** Holds the words of wordnet -- only for English title extractors*/
  protected Set<String> wordnetWords;

  /** Holds all entities of Wikipedia -- only for English title extractors */
  public final Set<String> entities;

  /** Language of Wikipedia */
  protected String language;

  /** Constructs a TitleExtractor */
  public TitleExtractor(Theme titlePatternFacts, Set<String> wordnetWords) throws IOException {
    replacer = new PatternList(titlePatternFacts, "<_titleReplace>");
    this.wordnetWords = wordnetWords;
    entities = null;
  }

  /**
   * Constructs a TitleExtractor
   *
   * @throws IOException
   */
  public TitleExtractor(String language) throws IOException {
    if (!PatternHardExtractor.TITLEPATTERNS.isAvailableForReading()) {
      throw new RuntimeException("The TitleExtractor needs PatternHardExtractor.TITLEPATTERNS as input.");
    }
    replacer = new PatternList(PatternHardExtractor.TITLEPATTERNS, "<_titleReplace>");
    if (FactComponent.isEnglish(language) && !Extractor.includeConcepts) {
      if (CoherentTypeExtractor.YAGOTYPES.isAvailableForReading()) {
        this.entities = CoherentTypeExtractor.YAGOTYPES.factCollection().getSubjects();
        this.wordnetWords = null;
      } else if (WordnetExtractor.PREFMEANINGS.isAvailableForReading()) {
        this.wordnetWords = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings().keySet();
        this.entities = null;
      } else {
        Announce.error("The English TitleExtractor needs WordnetExtractor.PREFMEANINGS or CoherentTypeExtractor.YAGOTYPES as input. "
            + "This is in order to avoid that Wikipedia articles that describe common nouns (such as 'table') become instances in YAGO.");
        this.entities = null;
        this.wordnetWords = null;
      }
    } else {
      this.entities = null;
      this.wordnetWords = null;
    }
    this.language = language;
  }

  /** Transforms the entity name to a YAGO entity, returns NULL if bad */
  public String createTitleEntity(String title) {
    title = replacer.transform(title);
    if (title == null) return null;
    if (wordnetWords != null && wordnetWords.contains(title.toLowerCase())) return (null);
    String entity = FactComponent.forForeignYagoEntity(title, language);
    if (entities != null && !entities.contains(entity)) return (null);
    return (entity);
  }

  /** Reads the title entity, supposes that the reader is after "<title>" */
  public String getTitleEntity(Reader in) throws IOException {
    String title = FileLines.readToBoundary(in, "</title>");
    title = Char17.decodeAmpersand(title);
    return (createTitleEntity(title));
  }

  /** TitleExtractor which only supports create/getTitleEntityRaw(...) */
  public static TitleExtractor rawExtractor(String language) throws IOException {
    TitleExtractor e = new TitleExtractor(PatternHardExtractor.TITLEPATTERNS, null);
    e.language = language;
    return e;
  }

  /** Transforms the entity name to a YAGO entity, without checkes */
  public String createTitleEntityRaw(String title) {
    title = replacer.transform(title);
    if (title == null) return null;
    return FactComponent.forForeignYagoEntity(title, language);
  }

  /** Reads the title entity without checks, supposes that the reader is after "<title>" */
  public String getTitleEntityRaw(Reader in) throws IOException {
    String title = FileLines.readToBoundary(in, "</title>");
    title = Char17.decodeAmpersand(title);
    return (createTitleEntityRaw(title));
  }

}
