package utils.termParsers;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;
import utils.FactTemplate;

/**
 * Extracts dates from a string
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
public class DateParser extends LiteralParser {

  public DateParser() throws IOException {
    super(PatternHardExtractor.DATEPARSER);
  }

  @Override
  public String resultEntity(Matcher resultMatch) {
    return FactComponent.forDate(resultMatch.group(1).trim());
  }

  public static void main(String[] args) throws Exception {
    File y = new File("/home/tr/tmp/yago3-debug");
    new PatternHardExtractor(new File("./data")).extract(new File("/home/tr/tmp/yago3-debug"), "test");
    PatternHardExtractor.DATEPARSER.assignToFolder(y);
    DateParser p = FactTemplate.dateParser();
    System.out.println(p.extractList("8th of January 100"));
    System.out.println(p.extractList("8th of January 100 BC"));
    System.out.println(p.extractList("8th of January 1998"));
    System.out.println(p.extractList("8th of January 1998 BC"));
    System.out.println(p.extractList("2nd century"));
    System.out.println(p.extractList("2nd century BC"));
    System.out.println(p.extractList("2nd millennium"));
    System.out.println(p.extractList("2nd millennium BC"));

    System.out.println("---");
    System.out.println(p.extractList("1230s BC"));
    System.out.println(p.extractList("120s BC"));
    System.out.println(p.extractList("10s BC"));
    System.out.println(p.extractList("0s BC"));
    System.out.println(p.extractList("0s"));
    System.out.println(p.extractList("10s"));
    System.out.println(p.extractList("120s"));
    System.out.println(p.extractList("1230s"));
    System.out.println("---");

    System.out.println(p.extractList("1234 BC"));
    System.out.println(p.extractList("123 BC"));
    System.out.println(p.extractList("12 BC"));
    System.out.println(p.extractList("1 BC"));
    System.out.println(p.extractList("AD 1"));
    System.out.println(p.extractList("AD 12"));
    System.out.println(p.extractList("AD 123"));
    System.out.println(p.extractList("AD 1234"));

  }
}
