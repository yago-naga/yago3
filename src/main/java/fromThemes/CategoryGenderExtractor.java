package fromThemes;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import extractors.MultilingualExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromWikipedia.CategoryExtractor;
import javatools.datatypes.FinalSet;
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

  /** Types deduced from categories */
  public static final MultilingualTheme CATEGORYGENDERSTOCHECK = new MultilingualTheme("categoryGendersToCheck",
      "Gender facts derived from the categories, still to be type-checked");

  /** Types deduced from categories, type checked */
  public static final MultilingualTheme CATEGORYGENDERSCHECKED = new MultilingualTheme("categoryGenders",
      "Gender facts derived from the categories, type-checked");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>();
    if (isEnglish()) result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
    else result.add(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language));
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYGENDERSTOCHECK.inLanguage(language), CATEGORYGENDERSOURCES.inLanguage(language));
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new FinalSet<>(new TypeChecker(CATEGORYGENDERSTOCHECK.inLanguage(language), CATEGORYGENDERSCHECKED.inLanguage(language)));
  }

  @Override
  public void extract() throws Exception {

    Theme output = CATEGORYGENDERSTOCHECK.inLanguage(language);
    Theme sourceOutput = CATEGORYGENDERSOURCES.inLanguage(language);
    Theme input = input().iterator().next();

    String lastEntity = "";
    // Run through all category memberships
    for (Fact f : input) {
      if (!f.getRelation().equals("<hasWikipediaCategory>")) continue;
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
