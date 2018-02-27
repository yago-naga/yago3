/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek, with contributions from Thomas Rebele.

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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.PatternList;
import utils.Theme;

/**
 * Superclass for classes that extract literals from Wikipedia strings by help
 * of "mapsTo" patterns.

*/
public abstract class LiteralParser extends TermParser {

  /** Holds the pattern that indicates a result */
  public static final Pattern resultPattern = Pattern.compile("_result_([^_]++)_([^_]*+)_");

  /** Holds the pattern list */
  public final PatternList patternList;

  /**
   * Constructs a LiteralParser from a theme that contains patterns
   * 
   * @throws IOException
   */
  protected LiteralParser(Theme patterns) throws IOException {
    patternList = new PatternList(patterns, "<mapsTo>");
  }

  /** Produces a result entity from a String */
  public abstract String resultEntity(Matcher resultMatch);

  @Override
  public List<String> extractList(String input) {
    input = patternList.transform(input);
    if (input == null) return (Collections.emptyList());
    List<String> result = new ArrayList<>();
    // Announce.debug("Done, transformed:", input);
    Matcher m = resultPattern.matcher(input);
    while (m.find()) {
      // Announce.debug("Result entity:", m.group());
      String resultEntity = resultEntity(m);
      // Announce.debug("Result entity transformed:", resultEntity);
      if (resultEntity != null) result.add(resultEntity);
    }
    return (result);
  }

  /** Test method */
  public static void main(String[] args) throws Exception {
  }
}
