package followUp;

import java.io.File;
import java.util.Map;

import extractors.Extractor;
import fromOtherSources.DictionaryExtractor;
import fromWikipedia.CategoryExtractor;
import utils.Theme;

/**
 * CategoryTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English
 * language. Objects are categories.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryTranslator extends EntityTranslator {

  @Override
  protected String translateObject(String object, Map<String, String> dictionary) {
    return dictionary.get(object);
  }

  public CategoryTranslator(Theme in, Theme out, Extractor parent) {
    super(in, out, parent);
    objectDictionaryTheme = DictionaryExtractor.CATEGORY_DICTIONARY.inLanguage(language);
  }

  public static void main(String[] args) throws Exception {
    Theme res = new Theme("result", "");
    new CategoryTranslator(CategoryExtractor.CATEGORYMEMBERS.inLanguage("de"), res, null).extract(new File("c:/fabian/data/yago3"), "test");
  }

}
