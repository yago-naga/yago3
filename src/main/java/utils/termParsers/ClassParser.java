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

package utils.termParsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.parsers.PlingStemmer;

/**  
 * Extracts a wordnet class form a string 
 * 
*/
public class ClassParser extends TermParser {

  /** Preferred meanings to map names to classes */
  public Map<String, String> preferredMeanings;

  /** Loads the preferred meanings from WordnetExtractor.PREFMEANINGS */
  public ClassParser() throws IOException {
    if (!WordnetExtractor.PREFMEANINGS.isAvailableForReading()) {
      Announce.error(WordnetExtractor.PREFMEANINGS, "must be available for reading.", "Consider caching the theme by declaring it in inputCached()");
    }
    this.preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings();
  }

  /** Needs the preferred meanings */
  public ClassParser(Map<String, String> preferredMeanings) {
    this.preferredMeanings = preferredMeanings;
  }

  @Override
  public List<String> extractList(String s) {
    List<String> result = new ArrayList<String>(3);
    for (String word : s.split(",|\n")) {
      word = word.trim().replace("[", "").replace("]", "");
      // Announce.debug(word);
      if (word.length() < 4) continue;
      String meaning = preferredMeanings.get(word);
      if (meaning == null) meaning = preferredMeanings.get(PlingStemmer.stem(word));
      if (meaning == null) meaning = preferredMeanings.get(word.toLowerCase());
      if (meaning == null) meaning = preferredMeanings.get(PlingStemmer.stem(word.toLowerCase()));
      if (meaning == null) continue;
      // Announce.debug("Match",meaning);
      result.add(meaning);
    }
    if (result.size() == 0) Announce.debug("Could not find class in", s);
    return (result);
  }

}
