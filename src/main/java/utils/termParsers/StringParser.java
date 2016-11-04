package utils.termParsers;

import java.io.IOException;
import java.util.regex.Matcher;

import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;

/**
 * Extracts a string from a Wikipedia string
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
public class StringParser extends LiteralParser {

  public StringParser() throws IOException {
    super(PatternHardExtractor.STRINGPARSER);
  }

  @Override
  public String resultEntity(Matcher resultMatch) {
    return (FactComponent.forString(resultMatch.group(1)));
  }

  /* This is the old code before taking into account Thomas Rebele's patterns in 2014.
   * 
   * Extracts a YAGO string from a string public static TermParser forString =
   * new TermParser("string") {
   * 
   * @Override public List<String> extractList(String s) { return (new
   * utils.literalParsers.StringParser(
   * PatternHardExtractor.STRINGPARSER).extract(s));
   * 
   * s = s.trim(); List<String> result = new ArrayList<String>(3); if
   * (s.startsWith("[[")) { for (String link : forWikiLink.extractList(s)) {
   * result.add(FactComponent.forString(FactComponent
   * .stripBracketsAndLanguage(link).replace('_', ' '))); } return (result); }
   * for (String w : s.split(";|,?\n|'''|''|, ?;|\"")) { w =
   * w.replaceAll("\\(.*\\)", ""); // Remove bracketed parts w =
   * w.replace("(", "").replace(")", ""); // remove remaining // brackets w =
   * w.trim(); w = Char17.decodeAmpersand(w); // Before: // w.length() > 2 &&
   * !w.contains("{{") && !w.contains("[[") if
   * (w.matches("[\\p{Alnum} ]{2,}")) result.add(FactComponent.forString(w));
   * } if (result.size() == 0) Announce.debug("Could not find string in", s);
   * return (result);
   * 
   * } };
   */
}
