package fromWikipedia;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts all articles that are non-entities (concepts).
 *
 * @author Johannes Hoffart
 *
 */
public class NonEntityExtractor extends MultilingualWikipediaExtractor {

  @Override public Set<Theme> input() {
    return new HashSet<>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS, TransitiveTypeExtractor.TRANSITIVETYPE));
  }

  @Override public Set<Theme> inputCached() {
    return new HashSet<>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS, TransitiveTypeExtractor.TRANSITIVETYPE));
  }

  public static final MultilingualTheme NONENTITIESNEEDSTRANSLATION = new MultilingualTheme("nonEntitiesNeedsTranslation",
      "Non-entity articles (needs translation)");

  public static final MultilingualTheme NONENTITIES = new MultilingualTheme("nonEntities", "Non-entity article");

  @Override public Set<Theme> output() {
    if (isEnglish()) {
      return new FinalSet<>(NONENTITIES.inLanguage(language));
    } else {
      return new FinalSet<>(NONENTITIESNEEDSTRANSLATION.inLanguage(language));
    }
  }

  @Override public Set<FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> result = new HashSet<>();

    if (!isEnglish()) {
      result.add(new EntityTranslator(NONENTITIESNEEDSTRANSLATION.inLanguage(language), NONENTITIES.inLanguage(this.language), this));
    }
    return result;
  }

  @Override public void extract() throws Exception {
    // Extract the information
    Announce.doing("Extracting context facts");

    BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    Set<String> entities = TransitiveTypeExtractor.TRANSITIVETYPE.factCollection().getSubjects();
    PatternList replacer = new PatternList(PatternHardExtractor.TITLEPATTERNS.factCollection(), "<_titleReplace>");

    String title = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>")) {
        case -1:
          Announce.done();
          in.close();
          return;
        case 0:
          title = replacer.transform(title);
          if (title == null) {
            continue;
          }
          String entity = FactComponent.forForeignYagoEntity(title, language);
          if (!entities.contains(entity)) {
            Fact f = new Fact(FactComponent.forWikipediaTitle(title), RDFS.type, "<NonEntityArticle>");
            if (isEnglish()) {
              NONENTITIES.inLanguage(language).write(f);
            } else {
              NONENTITIESNEEDSTRANSLATION.inLanguage(language).write(f);
            }
          }
        default:
          break;
      }
    }
  }

  /**
   * Needs Wikipedia as input
   *
   * @param wikipedia
   *            Wikipedia XML dump
   */
  public NonEntityExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

}
