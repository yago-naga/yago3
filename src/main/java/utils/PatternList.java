package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactSource;
import javatools.administrative.Announce;
import javatools.datatypes.Pair;

/**
 * Replaces patterns by strings
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
public class PatternList {

  /** Holds the patterns to apply */
  public final List<Pair<Pattern, String>> patterns = new ArrayList<Pair<Pattern, String>>();

  /**
   * Constructor
   * 
   * @throws IOException
   */
  public PatternList(Theme facts, String relation) throws IOException {
    if (!facts.isAvailableForReading()) throw new RuntimeException("Theme " + facts + " has to be available before using a "
        + this.getClass().getSimpleName() + "! Consider caching it by declaring it in inputCached() of the extracor.");
    load(new FactCollection(facts), relation);
  }

  /**
   * Constructor
   * 
   * @throws IOException
   */
  public PatternList(FactSource facts, String relation) throws IOException {
    this(new FactCollection(facts), relation);
  }

  /** Constructor */
  public PatternList(FactCollection facts, String relation) {
    load(facts, relation);
  }

  /** Loads all patterns */
  protected void load(FactCollection facts, String relation) {
    Announce.doing("Loading patterns of", relation);
    for (Fact fact : facts.getFactsWithRelation(relation)) {
      patterns.add(new Pair<Pattern, String>(fact.getArgPattern(1), fact.getArgJavaString(2)));
    }
    if (patterns.isEmpty()) {
      Announce.warning("No patterns found!");
    }
    Announce.done();
  }

  /** TRUE to print the result after each pattern application*/
  public static boolean printDebug = false;

  /** Replaces all patterns in the string */
  public String transform(String input) {
    if (input == null) return (null);
    if (printDebug) System.out.println("Input: " + input);
    for (Pair<Pattern, String> pattern : patterns) {
      if (printDebug) System.out.println("Pattern: " + pattern);
      String previous = input;
      input = pattern.first.matcher(input).replaceAll(pattern.second);
      if (printDebug && !previous.equals(input)) System.out.println("--------> " + input);
      if (input.contains("NIL") && pattern.second.equals("NIL")) return (null);
    }
    return (input);
  }
}
