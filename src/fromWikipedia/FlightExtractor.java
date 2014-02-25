package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import utils.TermExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.Redirector;
import fromThemes.TypeChecker;

public class FlightExtractor extends Extractor {

  public static final Theme FLIGHTS = new Theme("flights", "Flights from airport to airport");

  public static final Theme FLIGHTSOURCE = new Theme("flightSource", "Sources for flights");

  public static final Theme FLIGHTSNEEDRED = new Theme("flightsNeedRedirect", "Flights from airport to airport, need redirecting");

  public static final Theme FLIGHTSNEEDTYPE = new Theme("flightsNeedTypeCheck", "Flights from airport to airport, need type checking");

  protected File wikipedia;

  @Override
  public File inputDataFile() {   
    return wikipedia;
  }

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(FLIGHTSNEEDRED, FLIGHTSOURCE);
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
    		new Redirector(FLIGHTSNEEDRED, FLIGHTSNEEDTYPE, this, decodeLang(this.wikipedia.getName())), 
    		new TypeChecker(FLIGHTSNEEDTYPE, FLIGHTS, this)));
  }

  /** Constructor from source file */
  public FlightExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    //Announce.progressStart("Extracting", 4_500_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "{{Airport-dest-list")) {
        case -1:
          //Announce.progressDone();
          in.close();
          return;
        case 0:
          //Announce.progressStep();
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
            if (s.contains("[[")) {
              List<String> entities = TermExtractor.forWikiLink.extractList(s);
              if ((entities.size() == 1 || entities.size() == 2)) {
                airline = entities.get(0);
                Announce.debug("Airline:", airline);
                continue;
              }
            }
            if (airline == null) continue;
            s = Char.decodeAmpersand(s).replaceAll("\\[.*\\]", "");
            if (s.length() < 4 || s.contains("<") || s.contains("'''") || s.contains("\n") || s.contains("]]") || s.contains("=")) continue;
            s=s.replace("[[","").replace("]]", "");
            s = FactComponent.forWikipediaTitle(s);
            Fact f = new Fact(titleEntity, "<isConnectedTo>", s);
            Fact by = f.metaFact("<byTransport>", airline);
            write(output.get(FLIGHTSNEEDRED), f, output.get(FLIGHTSOURCE), titleEntity, "FlightExtractor");
            write(output.get(FLIGHTSNEEDRED), by, output.get(FLIGHTSOURCE), titleEntity, "FlightExtractor");
          }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    //new FlightExtractor(new File("./testCases/fromWikipedia.FlightExtractor/flightTest.xml")).extract(new File("c:/fabian/data/yago2s"),"test");
    new FlightExtractor(new File("c:/fabian/data/wikipedia/testset/la_airport2.xml")).extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
