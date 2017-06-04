package utils.termParsers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;

import basics.Fact;
import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;

/** Extracts a number from a string 
 * 
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
public class NumberParser extends LiteralParser {

  public NumberParser() throws IOException {
    super(PatternHardExtractor.NUMBERPARSER);
  }

  /** Parses a numerical expression with +, *, E */
  public static final BigDecimal parseNumerical(String expression) {
    // Remove any blanks that people added for readability
    expression = expression.replaceAll(" ", "");
    BigDecimal result = null;
    try {
      // Use a lookahead, so that we can retrieve the delimiter.
      String split[] = expression.split("(?=[\\+\\*])");
      result = new BigDecimal(split[0]);
      for (int i = 1; i < split.length; i++) {
        char operator = split[i].charAt(0);
        BigDecimal factor = new BigDecimal(split[i].substring(1));
        switch (operator) {
          case '*':
            result = result.multiply(factor);
            break;
          case '+':
            result = result.add(factor);
            break;
          default:
            Announce.warning("Faulty operator:", operator);
            return (null);
        }
      }
    } catch (Exception e) {
      Announce.warning("Cannot parse numerical expression", expression, "due to", e.toString());
      return (null);
    }
    return (result);
  }

  @Override
  public String resultEntity(Matcher resultMatch) {
    @Fact.ImplementationNote("Use toPlainString() so that subsequent regular expression type checks can identify integers")
    BigDecimal bigdec = parseNumerical(resultMatch.group(1));
    String unit = resultMatch.group(2).trim();
    if (bigdec == null) {
      Announce.warning("Cannot parse the numerical value", resultMatch.group());
      return (null);
    }
    // expand number for exponent < 100 or > -100 (e.g. 1e-99 gets expandend to 0.00...001, but 1e-100 stays)
    String bigdecString = (Math.abs(bigdec.scale()) < 100) ? bigdec.toPlainString() : bigdec.toEngineeringString();
    return (FactComponent.forStringWithDatatype(bigdecString, unit));
  }

  /* This is the old code before taking into account Thomas Rebele's patterns in 2014.
   * 
   * Extracts a number form a string public static TermParser forNumber = new
   * TermParser("number") {
   * 
   * @Override public List<String> extractList(String s) { List<String> result
   * = new ArrayList<>(); for (String num : NumberParser
   * .getNumbers(NumberParser.normalize(s))) { String[] nd =
   * NumberParser.getNumberAndUnit(num, new int[2]); if (nd.length == 1 ||
   * nd[1] == null) result.add(FactComponent.forNumber(nd[0])); else
   * result.add(FactComponent.forStringWithDatatype(nd[0],
   * FactComponent.forYagoEntity(nd[1]))); } if (result.size() == 0) {
   * Announce.debug("No number found in", s); } return (result); }
   * 
   * };
   */

}
