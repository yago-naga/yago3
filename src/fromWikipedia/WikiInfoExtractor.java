package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

/**
 * YAGO2s - Wikipedia Info Extractor
 *
 * Extracts the size of the Wikipedia pages, outlinks, etc.
 *
 * @author Fabian M. Suchanek
 *
 */
public class WikiInfoExtractor extends MultilingualWikipediaExtractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.TITLEPATTERNS);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.TITLEPATTERNS);
  }

  public static final MultilingualTheme WIKIINFONEEDSTRANSLATION = new MultilingualTheme("wikipediaInfoNeedsTranslation",
      "Stores the sizes, outlinks, and URLs of the Wikipedia articles of the YAGO entities.");

  public static final MultilingualTheme WIKIINFONEEDSTYPECHECK = new MultilingualTheme("wikipediaInfoNeedsTypeCheck",
      "Stores the sizes, outlinks, and URLs of the Wikipedia articles of the YAGO entities.");

  public static final MultilingualTheme WIKIINFO = new MultilingualTheme("yagoWikipediaInfo",
      "Stores the sizes, outlinks, and URLs of the Wikipedia articles of the YAGO entities.", Theme.ThemeGroup.OTHER);

  @Override
  public Set<Theme> output() {
    if (isEnglish()) {
      return new FinalSet<Theme>(WIKIINFO.inLanguage(language));
    } else {
      return new FinalSet<Theme>(WIKIINFONEEDSTRANSLATION.inLanguage(language));
    }
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    HashSet<FollowUpExtractor> s = new HashSet<>();
    if (!isEnglish()) {
      s.add(new EntityTranslator(WIKIINFONEEDSTRANSLATION.inLanguage(language), WIKIINFONEEDSTYPECHECK.inLanguage(language), this, true));
    }
    s.add(new TypeChecker(WIKIINFONEEDSTYPECHECK.inLanguage(language), WIKIINFO.inLanguage(language), this));
    return s;
  }

  @Override
  public void extract() throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor(language);
    // Extract the information
    // Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    while (FileLines.scrollTo(in, "<title>")) {
      String entity = titleExtractor.getTitleEntity(in);
      if (entity == null) continue;
      if (!FileLines.scrollTo(in, "<text")) continue;
      if (!FileLines.scrollTo(in, ">")) continue;
      String page = FileLines.readToBoundary(in, "</text>");
      if (page == null) continue;
      if (isEnglish()) {
        WIKIINFO.inLanguage(language).write(new Fact(entity, "<hasWikipediaArticleLength>", FactComponent.forNumber(page.length())));
        WIKIINFO.inLanguage(language).write(new Fact(entity, "<hasWikipediaUrl>", FactComponent.wikipediaURL(entity, language)));
      } else {
        // If the article size of non-English WPs is needed, this needs to be captured properly.
        //				WIKIINFONEEDSTRANSLATION.inLanguage(language).write(new Fact(entity, "<hasWikipediaArticleLength>", FactComponent.forNumber(page.length())));
        WIKIINFONEEDSTRANSLATION.inLanguage(language).write(new Fact(entity, "<hasWikipediaUrl>", FactComponent.wikipediaURL(entity, language)));
      }
      Set<String> targets = new HashSet<>();
      for (int pos = page.indexOf("[["); pos != -1; pos = page.indexOf("[[", pos + 2)) {
        int endPos = page.indexOf(']', pos);
        if (endPos == -1) continue;
        String target = page.substring(pos + 2, endPos);
        endPos = target.indexOf('|');
        if (endPos != -1) target = target.substring(0, endPos);
        target = FactComponent.forForeignWikipediaTitle(target, language);
        targets.add(target);
      }

      MultilingualTheme out = WIKIINFONEEDSTYPECHECK;
      if (!isEnglish()) {
        out = WIKIINFONEEDSTRANSLATION;
      }

      for (String target : targets) {
        out.inLanguage(language).write(new Fact(entity, "<linksTo>", target));
      }
    }
  }

  public WikiInfoExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

  public static void main(String[] args) throws Exception {
    /*    new WikiInfoExtractor("en", new File("c:/Fabian/eclipseProjects/yago2s/testCases/extractors.CategoryExtractor/wikitest.xml")).extract(new File(
        "c:/fabian/data/yago3"), "Test on 1 wikipedia article\n");*/

    new WikiInfoExtractor("ro", new File(args[1])).extract(new File(args[0]), "WikiInfoExtractor test");

  }
}
