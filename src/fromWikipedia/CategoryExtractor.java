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
import utils.FactTemplateExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
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
 * Extracts facts from categories
 * 
 * @author Fabian
 * @author Farzaneh Mahdisoltani
 * 
 */
public class CategoryExtractor extends Extractor {

  protected File wikipedia;
  protected String language;
  public static final HashMap<String, Theme> CATEGORYMEMBERSHIP_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> CATEGORYMEMBSOURCES_MAP = new HashMap<String, Theme>();
  
  
  static {
    for (String s : Extractor.languages) {
      CATEGORYMEMBERSHIP_MAP.put(s, new Theme("categoryMembership" + Extractor.langPostfixes.get(s), 
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected", ThemeGroup.OTHER));
      CATEGORYMEMBSOURCES_MAP.put(s, new Theme("categoryMembSources" +  Extractor.langPostfixes.get(s), "The sources of category facts", ThemeGroup.OTHER));
    }
  }
  
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(
        PatternHardExtractor.CATEGORYPATTERNS,
        PatternHardExtractor.TITLEPATTERNS, 
        WordnetExtractor.WORDNETWORDS,
        InterLanguageLinks.INTERLANGUAGELINKS
        ));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYMEMBSOURCES_MAP.get(language), CATEGORYMEMBERSHIP_MAP.get(language));
  }
  
  @Override
  public Set<Extractor> followUp() {
	if (this.language.equals("en")) {
	  return new HashSet<Extractor> (Arrays.asList(new CategoryMapperEN()));
	} else {
	  return new HashSet<Extractor> (Arrays.asList(
				new CategoryTranslator(this.language),
				new CategoryMapperMulti(this.language)));
	}
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),
        "<_categoryPattern>");
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      String categoryWord  = InterLanguageLinksDictionary.getCatDictionary(input.get( InterLanguageLinks.INTERLANGUAGELINKS)).get(language); 
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:" , "[["+categoryWord /*"[[Kategorie:","#REDIRECT"*/)) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        case 0:
          Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
        case 2:
          
          if (titleEntity == null){ continue;}
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          write(writers, CATEGORYMEMBERSHIP_MAP.get(language), new Fact(titleEntity, "<hasWikiCategory/" + this.language+ ">",
              FactComponent.forString(category)),CATEGORYMEMBSOURCES_MAP.get(language), 
              FactComponent.wikipediaURL(titleEntity), "CategoryExtractor" );
          break;
        case 3:
          // Redirect pages have to go away
          titleEntity=null;
          break;
      }
    }
  }


  @Override
  public File inputDataFile() {   
    return wikipedia;
  }
  
  /* Finds the language from the name of the input file, 
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
    Announce.setLevel(Announce.Level.DEBUG);
//    new HardExtractor(new File("D:/data/")).extract(new File("D:/data2/yago2s/"), "test");
//    new PatternHardExtractor(new File("D:/data")).extract(new File("D:/data2/yago2s/"), "test");
    //new CategoryExtractor(new File("D:/en_wikitest.xml")).extract(new File("D:/Data2/yago2s"), "Test on 1 wikipedia article");
    new CategoryExtractor(new File("D:/de_wikitest.xml")).extract(new File("D:/data2/yago2s"), "Test on 1 wikipedia article");
  }
}
