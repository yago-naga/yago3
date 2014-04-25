package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts facts from Wikipedia categories.
 * 
 * @author Fabian
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryExtractor extends Extractor {

  protected File wikipedia;

  public static final HashMap<String, Theme> CATEGORYMEMBERS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CATEGORYMEMBERSBOTHTRANSLATED_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CATEGORYMEMBERSSOURCES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      CATEGORYMEMBERS_MAP.put(s, new Theme("categoryMembers" + Extractor.langPostfixes.get(s),
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be tranlsated", ThemeGroup.OTHER));
      CATEGORYMEMBERSBOTHTRANSLATED_MAP.put(s, new Theme("categoryMembersBothTranslated" + Extractor.langPostfixes.get(s),
          "Category Members facts with translated subjects and objects.", ThemeGroup.OTHER));
      CATEGORYMEMBERSSOURCES_MAP.put(s, new Theme("categoryMembersSources" + Extractor.langPostfixes.get(s), "The sources of category facts",
          ThemeGroup.OTHER));
    }
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS,
        InterLanguageLinks.INTERLANGUAGELINKS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYMEMBERSSOURCES_MAP.get(language), CATEGORYMEMBERS_MAP.get(language));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
        new Translator(CATEGORYMEMBERS_MAP.get(this.language), CATEGORYMEMBERSBOTHTRANSLATED_MAP.get(this.language), this.language, "Category"),
        new CategoryMapper(this.language), new CategoryTypeExtractor(this.language)));
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    //Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      /** categoryWord holds the synonym of the word "Category" in different languages. 
       * It is needed to distinguish the category part in Wiki pages. 
       * 
       * TODO: categoryWord does not change during the run, does it? You can
       * take it outside of the loop.
       */
      String categoryWord = InterLanguageLinksDictionary.getCatDictionary(input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language);
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "[[" + categoryWord + ":")) {
        case -1:
          //Announce.progressDone();
          in.close();
          return;
        case 0:
          //Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
        case 2:

          if (titleEntity == null) {
            continue;
          }
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          write(writers, CATEGORYMEMBERS_MAP.get(language),
              new Fact(titleEntity, "<hasWikiCategory/" + this.language + ">", FactComponent.forString(category)),
              CATEGORYMEMBERSSOURCES_MAP.get(language), FactComponent.wikipediaURL(titleEntity), "CategoryExtractor");
          break;
        case 3:
          titleEntity = null;
          break;
      }
    }
  }

  /** Finds the language from the name of the input file, 
   * assuming that the first part of the name before the
   *  underline is equal to the language */
  public static String decodeLang(String fileName) {
    if (!fileName.contains("_")) return "en";
    return fileName.split("_")[0];
  }

  /** Constructor from source file */
  public CategoryExtractor(File wikipedia) {
    this(wikipedia, decodeLang(wikipedia.getName()));
  }

  public CategoryExtractor(File wikipedia, String lang) {
    this.wikipedia = wikipedia;
    this.language = lang;
  }

  public static void main(String[] args) throws Exception {

    new CategoryExtractor(new File("D:/en_wikitest.xml")).extract(new File("D:/data3/yago2s"), "Test on 1 wikipedia article");

  }

}
