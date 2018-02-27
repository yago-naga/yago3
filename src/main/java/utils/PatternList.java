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

package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactSource;
import javatools.administrative.Announce;
import javatools.datatypes.Pair;

/**
 * Replaces patterns by strings
 *
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
    load(facts, relation);
  }

  /**
   * Constructor
   *
   * @throws IOException
   */
  public PatternList(FactSource facts, String relation) throws IOException {
    load(facts, relation);
  }

  /** Constructor */
  /*public PatternList(FactCollection facts, String relation) {
    load(facts, relation);
  }*/

  /**
   * Constructor by array of patterns.
   *
   * @param _patterns List of pattern and their replace string.
   */
  public PatternList(List<Pair<Pattern, String>> _patterns) {
    for (Pair<Pattern, String> pattern : _patterns) {
      patterns.add(pattern);
    }
  }

  /** Loads all patterns */
  protected void load(FactSource facts, String relation) {
    Announce.doing("Loading patterns of", relation);
    for (Fact fact : facts) {
      if (relation.equals(fact.getRelation())) {
        patterns.add(new Pair<Pattern, String>(fact.getArgPattern(1), fact.getArgJavaString(2)));
      }
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
      String previous = input;
      input = pattern.first.matcher(input).replaceAll(pattern.second);
      if (printDebug && !previous.equals(input)) {
        System.out.println("Pattern: " + pattern);
        System.out.println("--------> " + input);
      }
      if (input.contains("NIL") && pattern.second.equals("NIL")) return (null);
    }
    return (input);
  }

  /** 
   * Transforms and tracks provenance of pattern.
   * Example: find numbers
   * input:     a123b
   * output:    a_result_1_b
   * startIdx:  011111111114 (as list 0,1,1,...)
   * endIdx:    144444444445 (as list 1,4,4,...)
   * 
   * @param input string to transform
   * @param startIdx empty list; will contain start indices of replaced string(s)
   * @param endIdx empty list; will contain (exclusive) end indices of replaced string(s)
   * @return
   */
  public String transformWithProvenance(String input, List<Integer> startIdx, List<Integer> endIdx) {
    if (input == null) return (null);
    if (printDebug) System.out.println("Input: " + input);

    List<Integer> oldStartIdx = new ArrayList<>(), oldEndIdx = new ArrayList<>();
    for (int i = 0; i < input.length(); i++) {
      oldStartIdx.add(i);
      oldEndIdx.add(i + 1);
    }

    for (Pair<Pattern, String> pattern : patterns) {
      if (printDebug) System.out.println("Pattern: " + pattern);
      String previous = input;
      Matcher m = pattern.first.matcher(input);

      // based on Matcher.replaceAll(...)
      boolean result = m.find();
      if (result) {
        List<Integer> newStartIdx = new ArrayList<>(), newEndIdx = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        int mPrevEnd = 0;
        do {
          // add chars between previous match and current match
          for (int i = 0; i < m.start() - mPrevEnd; i++) {
            newStartIdx.add(oldStartIdx.get(mPrevEnd + i));
            newEndIdx.add(oldEndIdx.get(mPrevEnd + i));
          }

          m.appendReplacement(sb, pattern.second);

          // calculate indices for replacement
          int inputStart = Collections.min(oldStartIdx.subList(m.start(), m.end()));
          int inputEnd = Collections.max(oldEndIdx.subList(m.start(), m.end()));

          // add indices
          for (int i = newStartIdx.size(); i < sb.length(); i++) {
            newStartIdx.add(inputStart);
            newEndIdx.add(inputEnd);
          }
          mPrevEnd = m.end();
          result = m.find();
        } while (result);

        // add tail
        for (int i = mPrevEnd; i < oldStartIdx.size(); i++) {
          newStartIdx.add(oldStartIdx.get(i));
          newEndIdx.add(oldEndIdx.get(i));
        }
        m.appendTail(sb);

        input = sb.toString();
        oldStartIdx = newStartIdx;
        oldEndIdx = newEndIdx;
      }

      if (printDebug && !previous.equals(input)) System.out.println("--------> " + input);
      if (input.contains("NIL") && pattern.second.equals("NIL")) return (null);
    }
    startIdx.addAll(oldStartIdx);
    endIdx.addAll(oldEndIdx);
    return (input);
  }
}
