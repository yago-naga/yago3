package followUp;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import fromOtherSources.DictionaryExtractor;
import fromWikipedia.WikiInfoExtractor;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * EntityTranslator - YAGO2s
 *
 * Translates the subjects and objects of the input themes to the most English
 * language.
 *
 * @author Farzaneh Mahdisoltani
 *
 */

public class EntityTranslator extends FollowUpExtractor {

  /** Target language */
  protected String language;

  /** Object dictionary */
  protected Theme objectDictionaryTheme;

  /** Keep facts even if they cannot be translated */
  protected boolean gracefulTranslation;

  @Override
  public Set<Theme> input() {
    // Do not use a FinalSet here because
    // objectDictionary might be equivalent to
    // entityDictionary
    return (new HashSet<>(Arrays.asList(checkMe, DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language), objectDictionaryTheme)));
  }

  @Override
  public Set<Theme> inputCached() {
    // Do not use a FinalSet here because
    // objectDictionary might be equivalent to
    // entiyDictionary
    return (new HashSet<>(Arrays.asList(DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language), objectDictionaryTheme)));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(checked);
  }

  /**
   * Translates the object as an entity, or returns it simply if it's a
   * literal. To be overwritten in subclasses.
   */
  protected String translateObject(String object, Map<String, String> dictionary) {
    if (FactComponent.isLiteral(object)) return (object);
    return (dictionary.get(object));
  }

  @Override
  public void extract() throws Exception {
    Map<String, String> subjectDictionary = DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language).dictionary();
    Map<String, String> objectDictionary = objectDictionaryTheme.dictionary();

    boolean baseFactWasTranslated = false;

    for (Fact f : checkMe) {
      // Translate metafacts if the preceding basefact was translated as well.
      if (FactComponent.isFactId(f.getSubject()) && baseFactWasTranslated) {
        String translatedObject = translateObject(f.getObject(), objectDictionary);
        if (translatedObject != null) {
          checked.write(new Fact(f.getId(), f.getSubject(), f.getRelation(), translatedObject));
        }
      } else {
        baseFactWasTranslated = false;
        String translatedSubject = subjectDictionary.get(f.getSubject());
        if (translatedSubject == null) {
          if (gracefulTranslation) {
            translatedSubject = f.getSubject();
          } else {
            continue;
          }
        }
        String translatedObject = translateObject(f.getObject(), objectDictionary);
        if (translatedObject == null) {
          if (gracefulTranslation) {
            translatedObject = f.getObject();
          } else {
            continue;
          }
        }
        checked.write(new Fact(f.getId(), translatedSubject, f.getRelation(), translatedObject));
        baseFactWasTranslated = true;
      }
    }
  }

  public EntityTranslator(Theme in, Theme out, Extractor parent, boolean graceful) {
    super(in, out, parent);
    this.language = in.language();
    if (language == null || FactComponent.isEnglish(language))
      throw new RuntimeException("Don't translate English. This is useless and very costly.");
    // By default, we translate entities.
    // May be overwritten in subclasses
    objectDictionaryTheme = DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language);

    this.gracefulTranslation = graceful;
  }

  public EntityTranslator(Theme in, Theme out, Extractor parent) {
    this(in, out, parent, false);
  }

  public static void main(String... args) throws Exception {
    String language = "ro";

    Theme in = WikiInfoExtractor.WIKIINFONEEDSTRANSLATION.inLanguage("ro");
    in.assignToFolder(new File(args[0]));
    FactCollection fc = in.factCollection();
    for (Fact f : fc) {
      System.out.println("fact: " + f);
    }

    new EntityTranslator(WikiInfoExtractor.WIKIINFONEEDSTRANSLATION.inLanguage(language), WikiInfoExtractor.WIKIINFO.inLanguage(language), null, true)
        .extract(new File(args[0]), "none");
  }

}
