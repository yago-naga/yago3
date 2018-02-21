/*
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

import java.io.File;

import javatools.administrative.Announce;

/**
 * YAGO2s - MultilingualWikipediaExtractor
 * 
 * An extractor that extracts from Wikipedia in different languages.
 * 
 * By convention, these classes have to have a constructor of with two
 * arguments: language and wikipedia
 * 
*/

public abstract class MultilingualWikipediaExtractor extends MultilingualExtractor {

  /** Data file */
  protected final File wikipedia;

  public MultilingualWikipediaExtractor(String lan, File wikipedia) {
    super(lan);
    this.wikipedia = wikipedia;
  }

  /** Creates an extractor with a given name */
  public static Extractor forName(Class<MultilingualWikipediaExtractor> className, String language, File wikipedia) {
    Announce.doing("Creating extractor", className + "(" + language + ")");
    if (language == null) {
      throw new RuntimeException("Language is null");
    }
    Extractor extractor = null;
    try {
      extractor = className.getConstructor(String.class, File.class).newInstance(language, wikipedia);
    } catch (Exception ex) {
      Announce.error(ex);
    }
    Announce.done();
    return (extractor);
  }

}
