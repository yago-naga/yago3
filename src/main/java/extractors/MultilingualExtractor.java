/*
 * Superclass of all multilingual extractors. This class determines which
 * Wikipedia versions can be read!
 * 
 * By convention, all subclasses have a constructor that takes as argument the
 * language.
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

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

package extractors;

import java.util.Arrays;
import java.util.List;

import basics.Fact;
import basics.FactComponent;
import javatools.administrative.Announce;

/**
 * MultilingualExtractor - Yago2s
 * 
*/

public abstract class MultilingualExtractor extends Extractor {

  /** List of language suffixes from most English to least English. */
  @Fact.ImplementationNote("The order is important, because " + "(1) the name for an entity that exists in several languages "
      + "will be the most-English name " + "(2) if two facts contradict, the one in the first language will prevail.")
  public static List<String> wikipediaLanguages = Arrays.asList("en", "fr");

  /** List of all languages except English */
  public static List<String> allLanguagesExceptEnglish() {
    return (wikipediaLanguages.subList(1, wikipediaLanguages.size()));
  }

  /** The language of this extractor */
  public final String language;

  /** TRUE if the language is english */
  public boolean isEnglish() {
    return (FactComponent.isEnglish(language));
  }

  @Override
  public String name() {
    return (this.getClass().getName() + "(" + this.language + ")");
  }

  public MultilingualExtractor(String lan) {
    this.language = lan;
  }

  /** Creates an extractor given by name */
  public static Extractor forName(Class<MultilingualExtractor> className, String language) {
    Announce.doing("Creating extractor", className + "(" + language + ")");
    if (language == null) {
      throw new RuntimeException("Language is null");
    }
    Extractor extractor = null;
    try {
      extractor = className.getConstructor(String.class).newInstance(language);
    } catch (Exception ex) {
      Announce.error(ex);
    }
    Announce.done();
    return (extractor);
  }
}
