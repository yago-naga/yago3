package retired;

import java.io.File;
import java.io.IOException;

import javatools.administrative.Announce;
import utils.FactCollection;

/**
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Thomas Rebele.

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
public class TransitionChecker {

  public static boolean check(File gold, File... result) throws IOException {
    Announce.doing("Checking", gold.getName());
    FactCollection goldStandard = new FactCollection(gold, true);
    FactCollection results = new FactCollection();
    for (File f : result)
      results.loadFast(f);
    if (results.checkEqual(goldStandard, "new", "old")) {
      Announce.done();
      return (true);
    } else {
      Announce.failed();
      return (false);
    }
  }

  public static void main(String[] args) throws Exception {
    // check(new
    // File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryClasses.ttl"),
    // new File("c:/fabian/data/yago2s/wikipediaClasses.ttl"));
    // check(new
    // File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryFactsDirty.ttl"),
    // new File("c:/fabian/data/yago2s/categoryFactsToBeRedirected.ttl"));
    // check(new
    // File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryTypes.ttl"),
    // new File("c:/fabian/data/yago2s/yagoTypes.ttl"));
    // check(new
    // File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.InfoboxExtractor\\infoboxfactsVeryDirty.ttl"),
    // new File("c:/fabian/data/yago2s/infoboxFactsToBeRedirected.ttl"));
    // check(new
    // File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.InfoboxExtractor\\infoboxTypes.ttl"),
    // new File("c:/fabian/data/yago2s/yagoTypes.ttl"));
  }
}
