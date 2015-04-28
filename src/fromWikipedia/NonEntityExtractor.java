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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts all articles that are non-entities (concepts).
 *
 * @author Johannes Hoffart
 *
 */
public class NonEntityExtractor extends MultilingualWikipediaExtractor {

  private static Pattern emptyStringPattern = Pattern.compile("\\s+");

  private static Pattern abstractPattern = Pattern.compile("(?is)(.*?)==");

  private static Pattern linkPattern = Pattern.compile("\\[\\[.*?\\]\\]]");

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

    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "<text>")) {
        case -1:
          Announce.done();
          in.close();
          return;
        case 0:
          String title = FileLines.readToBoundary(in, "</title>");
          title = replacer.transform(title);
          if (title == null) {
            continue;
          }
          titleEntity = FactComponent.forWikipediaTitle(title);
          if (!entities.contains(titleEntity)) {
            Fact f = new Fact(titleEntity, RDFS.type, "<NonEntityArticle>");
            if (isEnglish()) {
              NONENTITIES.inLanguage(language).write(f);
            } else {
              NONENTITIESNEEDSTRANSLATION.inLanguage(language).write(f);
            }
          }
        case 1:
          if (titleEntity == null) {
            continue;
          }
          String text = FileLines.readToBoundary(in, "</text>");

          // Count links.
          Matcher linkMatcher = linkPattern.matcher(text);
          int linkCount = 0;
          while (linkMatcher.find()) {
            ++linkCount;
          }
          Fact f = new Fact(titleEntity, "<hasLinkCount>", FactComponent.forNumber(linkCount));
          if (isEnglish()) {
            NONENTITIES.inLanguage(language).write(f);
          } else {
            NONENTITIESNEEDSTRANSLATION.inLanguage(language).write(f);
          }

          // Extract abstract.
          Matcher textMatcher = abstractPattern.matcher(text);
          if (textMatcher.find()) {
            String abstr = textMatcher.group(1);
            abstr = cleanText(abstr);
            if (abstr != null) {
              f = new Fact(titleEntity, "<hasAbstract>", FactComponent.forString(abstr));
              if (isEnglish()) {
                NONENTITIES.inLanguage(language).write(f);
              } else {
                NONENTITIESNEEDSTRANSLATION.inLanguage(language).write(f);
              }
            }
          }
        default:
          break;
      }
    }
  }

  private String cleanText(String s) {
    StringBuilder sb = new StringBuilder();

    int brackets = 0;

    for (int i = 0; i < s.length(); i++) {
      char current = s.charAt(i);

      if (current == '{') {
        brackets++;
      } else if (current == '}') {
        brackets--;
      } else if (brackets == 0) {
        sb.append(current);
      }
    }

    String clean = sb.toString().trim();

    clean = clean.replaceAll("\\s+", " ");
    // Leave Wikipedia links.
//    clean = clean.replaceAll("\\[\\[[^\\]\n]+?\\|([^\\]\n]+?)\\]\\]", "$1");
//    clean = clean.replaceAll("\\[\\[([^\\]\n]+?)\\]\\]", "$1");
    clean = clean.replaceAll("\\[https?:.*?\\]", "");
    clean = clean.replaceAll("'{2,}", "");


    if (!emptyStringPattern.matcher(clean).matches()) {
      return null;
    } else {
      return clean;
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
