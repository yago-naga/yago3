package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

public class FlightIATAcodeExtractor extends Extractor {

  public static final Theme AIRPORT_CODE = new Theme("hasAirportCode", "The airport code for each airport");

  public static final Theme AIRPORT_CODE_SOURCE = new Theme("hasAirportCodeSource", "Sources for airport code");

  protected File wikipedia;

  @Override
  public Set<Theme> input() {
    return new FinalSet<>();
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(AIRPORT_CODE, AIRPORT_CODE_SOURCE);
  }

  /** Constructor from source file */
  public FlightIATAcodeExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    // Extract the information
    Announce.progressStart("Extracting", 4_500_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    while (true) {
      switch (FileLines.findIgnoreCase(in, "{{Lists of airports by IATA code}}")) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        default:
          String IATAlist = FileLines.readTo(in, "</page>").toString();
          Pattern p = Pattern.compile("==[A-Z]==");
          Matcher m = p.matcher(IATAlist);
          String source = "List of airports by IATA code: ";
          try {
            while (m.find()) {
              source = source + m.group().substring(2, 3);
            }
          } catch (Exception e) {
            //ignore
          }
          source = source.trim().replace(' ', '_');
          p = Pattern.compile("\\|\\s*(?:[A-Z][A-Z][A-Z]\\s*\\|\\|.*||\\s\\[\\[.*\\]\\])\\s*\\|\\|");
          m = p.matcher(IATAlist);
          while (m.find()) {
            String map = m.group();
            String[] data = map.split("\\|\\|");
            if (data.length >= 3) {
              String IATA = data[0].substring(1).trim();
              if (IATA.length() != 3) {
                continue;
              }
              IATA = FactComponent.forString(IATA);
              int start = data[2].indexOf("[[") + 2;
              int end = data[2].indexOf("]]");
              if (start < 0 || end < 0) {
                continue;
              }
              String entity = FactComponent.forYagoEntity(data[2].substring(start, end).replace(' ', '_'));
              Fact f = new Fact(entity, "<hasAirportCode>", IATA);
              write(output.get(AIRPORT_CODE), f, output.get(AIRPORT_CODE_SOURCE), source, "FlightIATAcodeExtractor");
            }
          }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new FlightIATAcodeExtractor(new File("./testCases/fromWikipedia.FlightIATAcodeExtractor/iatatest.xml")).extract(new File(
        "./testCases/fromWikipedia.FlightIATAcodeExtractor/"), "test");
  }
}
