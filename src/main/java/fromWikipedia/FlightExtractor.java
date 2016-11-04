package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import extractors.EnglishWikipediaExtractor;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.Theme;
import utils.TitleExtractor;
import utils.termParsers.WikiLinkParser;

/**
 * Extracts flight connections from Wikipedia
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

public class FlightExtractor extends EnglishWikipediaExtractor {

  public static final Theme FLIGHTS = new Theme("flights", "Flights from airport to airport");

  public static final Theme FLIGHTSOURCE = new Theme("flightSource", "Sources for flights");

  public static final Theme FLIGHTSNEEDRED = new Theme("flightsNeedRedirect", "Flights from airport to airport, need redirecting");

  public static final Theme FLIGHTSNEEDTYPE = new Theme("flightsNeedTypeCheck", "Flights from airport to airport, need type checking");

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.PREFMEANINGS);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(FLIGHTSNEEDRED, FLIGHTSOURCE);
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new FinalSet<FollowUpExtractor>(new Redirector(FLIGHTSNEEDRED, FLIGHTSNEEDTYPE, this), new TypeChecker(FLIGHTSNEEDTYPE, FLIGHTS, this));
  }

  /** Constructor from source file */
  public FlightExtractor(File wikipedia) {
    super(wikipedia);
  }

  @Override
  public void extract() throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor("en");
    WikiLinkParser wikiLinkParser = new WikiLinkParser("en");
    // Extract the information
    // Announce.progressStart("Extracting", 4_500_000);
    Reader in = FileUtils.getBufferedUTF8Reader(inputData);
    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "{{Airport-dest-list")) {
        case -1:
          // Announce.progressDone();
          in.close();
          return;
        case 0:
          // Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          Announce.debug("Title:", titleEntity);
          break;
        default:
          if (titleEntity == null) continue;
          String flights = FileLines.readTo(in, "</page>").toString();
          Announce.debug("Found box");
          String airline = null;
          for (String s : flights.split("\\||,")) {
            s = s.trim();
            if (s.contains("}}\n\n") || s.contains("}}\r\n\r\n") || s.contains("}}\r\r") || s.contains("}}\n\r\n\r")) break;
            s = Char17.decodeAmpersand(s);
            if (s.contains("[[")) {
              List<String> entities = wikiLinkParser.extractList(s);
              if ((entities.size() == 1 || entities.size() == 2)) {
                airline = entities.get(0);
                Announce.debug("Airline:", airline);
                continue;
              }
            }
            if (airline == null) continue;
            s = Char17.decodeAmpersand(s).replaceAll("\\[.*\\]", "");
            if (s.length() < 4 || s.contains("<") || s.contains("'''") || s.contains("\n") || s.contains("{{") || s.contains("]]") || s.contains("="))
              continue;
            s = s.replace("[[", "").replace("]]", "");
            s = FactComponent.forWikipediaTitle(s);
            Fact f = new Fact(titleEntity, "<isConnectedTo>", s);
            Fact by = f.metaFact("<byTransport>", airline);
            write(FLIGHTSNEEDRED, f, FLIGHTSOURCE, FactComponent.wikipediaURL(titleEntity), "FlightExtractor");
            write(FLIGHTSNEEDRED, by, FLIGHTSOURCE, FactComponent.wikipediaURL(titleEntity), "FlightExtractor");
          }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    // new FlightExtractor(new
    // File("./testCases/fromWikipedia.FlightExtractor/flightTest.xml")).extract(new
    // File("c:/fabian/data/yago2s"),"test");
    new FlightExtractor(new File("c:/fabian/data/wikipedia/testset/la_airport2.xml")).extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
