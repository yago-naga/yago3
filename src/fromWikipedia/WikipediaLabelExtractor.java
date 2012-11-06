package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * WikipediaLabelExtractor - YAGO2s
 * 
 * Extracts labels from categories and multilingual links
 * 
 * @author Fabian
 * 
 */
public class WikipediaLabelExtractor extends Extractor {

  /** The file from which we read */
  protected File wikipedia;

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS, TransitiveTypeExtractor.TRANSITIVETYPE,
        PatternHardExtractor.LANGUAGECODEMAPPING));
  }

  /** Facts deduced from categories */
  public static final Theme MULTILINGUALLABELS = new Theme("yagoMultilingualInstanceLabels",
      "Names for the Wikipedia instances in multiple languages", ThemeGroup.MULTILINGUAL);

  /** Facts deduced from categories */
  public static final Theme WIKIPEDIALABELS = new Theme("wikipediaLabels", "Labels derived from the name of the instance in Wikipedia");

  /** Sources */
  public static final Theme WIKIPEDIALABELSOURCES = new Theme("wikipediaLabelSources", "Sources for the Wikipedia labels");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WIKIPEDIALABELSOURCES, WIKIPEDIALABELS, MULTILINGUALLABELS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    Map<String,String> lanMap=new FactCollection(input.get(PatternHardExtractor.LANGUAGECODEMAPPING)).asStringMap("<hasThreeLetterLanguageCode>");
    TitleExtractor titleExtractor = new TitleExtractor(input);
    Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[")) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        case 0:
          Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          if (titleEntity != null) {
            for (String name : namesOf(titleEntity)) {
              write(writers, WIKIPEDIALABELS, new Fact(titleEntity, RDFS.label, name), WIKIPEDIALABELSOURCES,
                  FactComponent.wikipediaURL(titleEntity), "WikipediaLabelExtractor from simple name heuristics");
            }
            String name=FactComponent.forStringWithLanguage(preferredName(titleEntity),"eng");
            write(writers, WIKIPEDIALABELS, new Fact(titleEntity, YAGO.hasPreferredName, name), WIKIPEDIALABELSOURCES,
                FactComponent.wikipediaURL(titleEntity), "WikipediaLabelExtractor from title");            
            write(writers, WIKIPEDIALABELS, new Fact(titleEntity, YAGO.isPreferredMeaningOf, name), WIKIPEDIALABELSOURCES,
                FactComponent.wikipediaURL(titleEntity), "WikipediaLabelExtractor from title");            
          }
          break;
        case 1:
          if (titleEntity == null) continue;
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          if (category.toLowerCase().startsWith("category:")) continue;
          int colon = category.indexOf(':');
          if (colon != -1 && colon < 8 && category.substring(0, colon).matches("[a-z\\-]+")) {
            writers.get(MULTILINGUALLABELS).write(
                new Fact(titleEntity, RDFS.label, FactComponent.forStringWithLanguage(category.substring(colon + 1), D.getOr(lanMap, category.substring(0, colon),category.substring(0, colon)))));
          }
      }
    }
  }

  /** returns the (trivial) names of an entity */
  public static Set<String> namesOf(String titleEntity) {
    Set<String> result = new TreeSet<>();
    String name = preferredName(titleEntity);
    result.add(FactComponent.forStringWithLanguage(name, "eng"));
    String norm = Char.normalize(name);
    if (!norm.contains("[?]")) result.add(FactComponent.forStringWithLanguage(norm, "eng"));
    if (name.contains(" (")) {
      result.add(FactComponent.forStringWithLanguage(name.substring(0, name.indexOf(" (")).trim(), "eng"));
    }
    if (name.contains(",") && !name.contains("(")) {
      result.add(FactComponent.forStringWithLanguage(name.substring(0, name.indexOf(",")).trim(), "eng"));
    }
    return (result);
  }

  /** returns the preferred name*/
  public static String preferredName(String titleEntity) {
    if (titleEntity.startsWith("<")) titleEntity = titleEntity.substring(1);
    if (titleEntity.endsWith(">")) titleEntity = Char.cutLast(titleEntity);
    return (Char.decode(titleEntity.replace('_', ' ')));
  }

  /** Constructor from source file */
  public WikipediaLabelExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new WikipediaLabelExtractor(new File("c:/fabian/data/wikipedia/testset/angie.xml")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
  }
}
