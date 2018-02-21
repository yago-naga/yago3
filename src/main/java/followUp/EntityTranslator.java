/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Farzaneh Mahdisoltani, with contributions from Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

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
*/

public class EntityTranslator extends FollowUpExtractor {

  /** Target language */
  protected String language;

  /** Object dictionary */
  protected Theme objectDictionaryTheme;
  
  /** Subject dictionary */
  protected Theme subjectDictionaryTheme;

  /** Keep facts even if they cannot be translated */
  protected boolean gracefulTranslation;

  @Override
  public Set<Theme> input() {
    // Do not use a FinalSet here because
    // objectDictionary might be equivalent to
    // entityDictionary
    return (new HashSet<>(Arrays.asList(checkMe, subjectDictionaryTheme, objectDictionaryTheme)));
  }

  @Override
  public Set<Theme> inputCached() {
    // Do not use a FinalSet here because
    // objectDictionary might be equivalent to
    // entiyDictionary
    return (new HashSet<>(Arrays.asList(subjectDictionaryTheme, objectDictionaryTheme)));
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
    Map<String, String> subjectDictionary = subjectDictionaryTheme.dictionary();
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
    
    subjectDictionaryTheme = DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language);

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
