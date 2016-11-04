package utils.termParsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.FactComponent;
import javatools.administrative.Announce;

/**
 * Extracts a URL from a string
 * 
 * This could in principle be done by a LiteralParser, but since URLs can
 * contain underscores, the built-in pattern matching would be derailed.
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
public class UrlParser extends TermParser {

  // also needs to match \ for yago-encoded stuff
  private static List<Pattern> urlPatterns = Arrays.asList(Pattern.compile("http[s]?://([-\\w\\./\\\\]+)"),
      Pattern.compile("(www\\.[-\\w\\./\\\\]+)"));

  @Override
  public List<String> extractList(String s) {
    List<String> urls = new ArrayList<String>(3);

    for (Pattern p : urlPatterns) {
      Matcher m = p.matcher(s);
      while (m.find()) {
        String url = FactComponent.forUri("http://" + m.group(1));
        urls.add(url);
      }
    }

    if (urls.size() == 0) Announce.debug("Could not find URL in", s);
    return urls;
  }
}
