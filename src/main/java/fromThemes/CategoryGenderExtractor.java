package fromThemes;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.MultilingualExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * CategoryGenderExtractor - YAGO2s
 *
 * Extracts genders from the category membership facts.
 *
 * @author Fabian
 *
 */
public class CategoryGenderExtractor extends MultilingualExtractor {

  public CategoryGenderExtractor(String lan) {
    super(lan);
  }

  /** Sources for category facts */
  public static final MultilingualTheme CATEGORYGENDERSOURCES = new MultilingualTheme("categoryGenderSources",
      "Sources for the genders derived from the Wikipedia categories");

  /** Genders deduced from categories, type checked */
  public static final MultilingualTheme CATEGORYGENDER = new MultilingualTheme("categoryGenders",
      "Gender facts derived from the categories, type-checked");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>();
    if (isEnglish()) result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
    else result.add(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language));
    result.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYGENDER.inLanguage(language), CATEGORYGENDERSOURCES.inLanguage(language));
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
  }

  @Override
  public void extract() throws Exception {

    Theme output = CATEGORYGENDER.inLanguage(language);
    Theme sourceOutput = CATEGORYGENDERSOURCES.inLanguage(language);
    Theme input = input().iterator().next();
    FactCollection types = TransitiveTypeExtractor.TRANSITIVETYPE.factCollection();

    String lastEntity = "";
    // Run through all category memberships
    for (Fact f : input) {
      if (!f.getRelation().equals("<hasWikipediaCategory>")) continue;
      if (!types.contains(f.getSubject(), RDFS.type, YAGO.person)) continue;
      String category = f.getObject();
      if (category.startsWith("<wikicat_Male_") && !lastEntity.equals(f.getSubject())) {
        write(output, new Fact(f.getSubject(), "<hasGender>", "<male>"), sourceOutput, FactComponent.forYagoEntity(f.getSubject()),
            "Gender from category");
        lastEntity = f.getSubject();
      } else if (category.startsWith("<wikicat_Female_") && !lastEntity.equals(f.getSubject())) {
        write(output, new Fact(f.getSubject(), "<hasGender>", "<female>"), sourceOutput, FactComponent.forYagoEntity(f.getSubject()),
            "Gender from category");
        lastEntity = f.getSubject();
      }
    }

  }

  public static void main(String[] args) throws Exception {
    new CategoryGenderExtractor("fr").extract(new File("c:/fabian/data/yago3"), "Test");
    new CategoryGenderExtractor("en").extract(new File("c:/fabian/data/yago3"), "Test");
  }

}
