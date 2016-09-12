package fromThemes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.MultilingualExtractor;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryHierarchyExtractor;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.FactTemplate;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * CategoryMapper - YAGO2s
 *
 * Maps the facts obtained from CategoryExtractor (Previously translated for
 * other languages) to YAGO facts.
 *
 * @author Farzaneh Mahdisoltani
 *
 */
public class CategoryMapper extends MultilingualExtractor {

  public static final MultilingualTheme CATEGORYFACTS_TOREDIRECT = new MultilingualTheme("categoryFactsToBeRedirected",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");

  public static final MultilingualTheme CATEGORYFACTS_TOTYPECHECK = new MultilingualTheme("categoryFactsToBeTypechecked",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");

  public static final MultilingualTheme CATEGORYFACTS = new MultilingualTheme("categoryFacts",
      "Facts about Wikipedia instances, derived from the Wikipedia categories");

  public static final MultilingualTheme CATEGORYSOURCES = new MultilingualTheme("categorySources",
      "Sources for the facts about Wikipedia instances, derived from the Wikipedia categories");

  protected FactCollection categoryClasses;

  @Override
  public Set<Theme> inputCached() {
    return (new FinalSet<>(PatternHardExtractor.CATEGORYPATTERNS));
  }

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new HashSet<Theme>(
        Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, CategoryClassExtractor.CATEGORYCLASSES, PatternHardExtractor.LANGUAGECODEMAPPING));
    if (isEnglish()) {
      result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
      result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY.inLanguage(language));
    } else {
      result.add(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language));
      result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY_TRANSLATED.inLanguage(language));
    }
    return (result);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYFACTS_TOREDIRECT.inLanguage(language), CATEGORYSOURCES.inLanguage(language));
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new FinalSet<FollowUpExtractor>(
        new Redirector(CATEGORYFACTS_TOREDIRECT.inLanguage(language), CATEGORYFACTS_TOTYPECHECK.inLanguage(language), this),
        new TypeChecker(CATEGORYFACTS_TOTYPECHECK.inLanguage(language), CATEGORYFACTS.inLanguage(language), this));
  }

  @Override
  public void extract() throws Exception {
    // input data
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(PatternHardExtractor.CATEGORYPATTERNS.factCollection(), "<_categoryPattern>");
    FactTemplateExtractor hierarchicalCategoryPatterns = new FactTemplateExtractor(PatternHardExtractor.CATEGORYPATTERNS.factCollection(),
        "<_hierarchicalCategoryPattern>");

    categoryClasses = CategoryClassExtractor.CATEGORYCLASSES.factCollection();

    FactSource factSource;
    FactCollection wikiCatHierarchy;
    if (isEnglish()) {
      factSource = CategoryExtractor.CATEGORYMEMBERS.inLanguage(language);
      wikiCatHierarchy = CategoryHierarchyExtractor.CATEGORYHIERARCHY.inLanguage(language).factCollection();
    } else {
      factSource = CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language);
      wikiCatHierarchy = CategoryHierarchyExtractor.CATEGORYHIERARCHY_TRANSLATED.inLanguage(language).factCollection();
    }
    Map<String, String> languageMap = null;
    try {
      languageMap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");
    } catch (IOException e) {
      e.printStackTrace();
    }

    //FactCollection genderInfo = GenderExtractor.PERSONS_GENDER.factCollection();

    Theme output = CATEGORYFACTS_TOREDIRECT.inLanguage(language);
    Theme outputSource = CATEGORYSOURCES.inLanguage(language);

    Map<String, List<FactTemplate>> categoryToPatterns = new HashMap<>();

    // apply templates to facts
    Map<String, String> variables = new TreeMap<>();
    for (Fact f : factSource) {
      String source = FactComponent.wikipediaSourceURL(f.getArg(1), language);

      variables.put("$0", f.getSubject());
      List<FactTemplate> templates = categoryToPatterns.computeIfAbsent(f.getObject(), category -> {
        String cat = FactComponent.stripCat(category);
        List<FactTemplate> result = new ArrayList<>();
        result.addAll(categoryPatterns.makeTemplates(cat, language));

        // preprocess categories hierarchically
        // infer more information by using Wikipedia category hierarchy
        // for example
        //    <Shevonne_Durkin> <hasWikipediaCategory> <wikicat_American_television_actresses>
        //    <wikicat_American_television_actresses> <wikipediaSubCategoryOf>* <wikicat_Women>
        // infer
        //    <Shevonne_Durkin> <hasGender> <female>
        if (hierarchicalCategoryPatterns.patterns.size() > 0) {
          Set<String> superCategories = new HashSet<>();
          wikiSuperCategories(category, superCategories, wikiCatHierarchy);
          for (String superCat : superCategories) {
            result.addAll(hierarchicalCategoryPatterns.makeTemplates(FactComponent.stripCat(superCat), language));
          }
        }
        System.out.println(result);
        return result;
      });
      if (templates != null && templates.size() > 0) {
        List<Fact> facts = FactTemplate.instantiate(templates, variables, language, languageMap);

        /*if (facts.toString().contains("<female>")) {
          if ("<male>".equals(genderInfo.getObject(f.getSubject(), "<hasGender>"))) {
            Set<String> superCategories = new HashSet<>();
            wikiSuperCategories(f.getObject(), superCategories, wikiCatHierarchy, "wikicat: ");
          }
        }*/

        for (Fact fact : facts) {
          if (fact != null) {
            write(output, fact, outputSource, source, "CategoryMapper");
          }
        }
      }
    }
  }

  /**
   * Decide whether to apply hierarchical patterns to the super categories of 'categoryName'
   * @param categoryName
   * @return
   */
  protected boolean followSubcategoryOfEdge(String categoryName) {
    return (categoryClasses.containsSubject(categoryName));
  }

  /**
   * Adds parent categories of cat to the set, i.e. follows "&lt;wikipediaSubCategoryOf&gt;" links
   */
  protected void wikiSuperCategories(String cat, Set<String> superCats, FactCollection fc) {
    wikiSuperCategories(cat, superCats, fc, null);
  }

  protected void wikiSuperCategories(String cat, Set<String> superCats, FactCollection fc, String prefix) {
    if (!superCats.add(cat)) {
      return;
    }
    if (!followSubcategoryOfEdge(cat)) {
      /*if (prefix != null) {
        System.out.println(prefix + "SKIPPING " + cat);
      }*/
      return;
    }
    /*if (prefix != null) {
      System.out.println(prefix + cat);
    }*/
    for (String s : fc.collectObjects(cat, CategoryClassHierarchyExtractor.WIKIPEDIA_RELATION)) {
      wikiSuperCategories(s, superCats, fc, prefix == null ? null : prefix + "  ");
    }
  }

  /** Constructor from source file */
  public CategoryMapper(String lang) {
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    new CategoryMapper("en").extract(new File("c:/fabian/data/yago3"), "mapping categories into facts");
  }

}
