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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import utils.PatternList;

/**
 * Extract entities and terms from Wikipedia strings
 * 
*/
public abstract class TermParser {

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Returns a parser for a YAGO type. Deprecated, use allParsers() instead.
   * @throws IOException
   */
  @Deprecated
  public static TermParser forType(String type) throws IOException {
    switch (type) {
      case RDFS.clss:
        Announce.error("Call TermParser.forClass() for classes!");
        return (null);
      case YAGO.entity:
      case "rdf:Resource":
        return (new WikiLinkParser("eng"));
      case "xsd:date":
        return (new DateParser());
      case "<yagoLanString>":
      case "xsd:string":
      case "<yagoTLD>":
      case "<yagoISBN>":
      case "<yagoIdentifier>":
        return (new StringParser());
      case "<yagoURL>":
        return (new UrlParser());
      case "xsd:decimal":
      case "<degrees>":
      case "<m2>":
      case "<yagoMonetaryValue>":
      case "<percent>":
      case "</km2>":
      case "xsd:integer":
      case "xsd:duration":
      case "<g>":
      case "<m>":
      case "<s>":
      case "<yago0to100>":
      case "xsd:nonNegativeInteger":
      case "<yagoFraction>":
        return (new NumberParser());
    }
    return (new WikiLinkParser("eng"));
  }

  /**
   * Returns all available parsers
   * 
   * @throws IOException
   */
  public static List<TermParser> allParsers(Map<String, String> preferredMeanings, String language) throws IOException {
    List<TermParser> all = new ArrayList<>();
    if (FactComponent.isEnglish(language)) all.add(new ClassParser(preferredMeanings));
    all.add(new WikiLinkParser(language));
    all.add(new NumberParser());
    all.add(new StringParser());
    all.add(new DateParser());
    all.add(new UrlParser());
    return all;
  }

  /** Extracts multiple entities from a string. Return NULL if this fails. */
  public abstract List<String> extractList(String s);

  /** Test one particular case */
  public void test(String expression) {
    Announce.setLevel(Announce.Level.DEBUG);
    D.p(extractList(expression));
    D.exit();
  }

  /** Test method*/
  public static void main(String[] args) throws Exception {
    PatternHardExtractor.STRINGPARSER.assignToFolder(new File("./data"));
    PatternHardExtractor.DATEPARSER.assignToFolder(new File("./data"));
    PatternHardExtractor.NUMBERPARSER.assignToFolder(new File("./data"));
    WordnetExtractor.PREFMEANINGS.assignToFolder(new File("../../data/yago3"));
    PatternList.printDebug = true;
    TermParser parsi = new NumberParser();
    parsi.test("10% of all");
  }
}
