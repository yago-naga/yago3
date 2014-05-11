package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import extractors.EnglishWikipediaExtractor;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;

/**
 * YAGO2s - FlightIATAcodeExtractor
 * 
 * Extracts all the IATA airport codes from "List of airports by IATA code"
 * wikipedia pages.
 * 
 * @author Edwin Lewis-Kelham
 * 
 */
public class FlightIATAcodeExtractor extends EnglishWikipediaExtractor {

	public static final Theme AIRPORT_CODE = new Theme("hasAirportCode",
			"The airport code for each airport");

	public static final Theme AIRPORT_CODE_SOURCE = new Theme(
			"hasAirportCodeSource", "Sources for airport code");

	public static final Theme AIRPORT_CODE_NEEDRED = new Theme(
			"hasAirportCodeNeedRedirect", "Airports need redirecting check");

	public static final Theme AIRPORT_CODE_NEEDTYPE = new Theme(
			"hasAirportCodeNeedTypeCheck", "Airports need type checking");

	@Override
	public Set<Theme> input() {
		return new FinalSet<>();
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(AIRPORT_CODE_NEEDRED, AIRPORT_CODE_SOURCE);
	}

	@Override
	public Set<FollowUpExtractor> followUp() {
		return new FinalSet<FollowUpExtractor>(new Redirector(
				AIRPORT_CODE_NEEDRED, AIRPORT_CODE_NEEDTYPE, this),
				new TypeChecker(AIRPORT_CODE_NEEDTYPE, AIRPORT_CODE, this));
	}

	/** Constructor from source file */
	public FlightIATAcodeExtractor(File wikipedia) {
		super(wikipedia);
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Announce.doing("Extracting");
		Reader in = FileUtils.getBufferedUTF8Reader(inputData);
		while (true) {
			switch (FileLines.findIgnoreCase(in,
					"{{Lists of airports by IATA code}}")) {
			case -1:
				Announce.done();
				in.close();
				return;
			default:
				String IATAlist = FileLines.readTo(in, "</page>").toString();
				Pattern p = Pattern.compile("==[A-Z]==");
				Matcher m = p.matcher(IATAlist);
				// get source page info
				String source = "List of airports by IATA code: ";
				try {
					while (m.find()) {
						source = source + m.group().substring(2, 3);
					}
				} catch (Exception e) {
					Announce.warning("Was not able to parse the ID for the List of airports by IATA code.");
				}
				source = FactComponent.wikipediaURL(source.trim().replace(' ',
						'_'));
				// get each line that contains a IATA code and a airport
				p = Pattern
						.compile("\\|\\s*(?:[A-Z][A-Z][A-Z]\\s*\\|\\|.*||\\s\\[\\[.*\\]\\])\\s*\\|\\|");
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
						if (data[2].indexOf('|') != -1
								&& data[2].indexOf('|') < end)
							end = data[2].indexOf('|');
						String entity = FactComponent.forYagoEntity(data[2]
								.substring(start, end).replace(' ', '_'));
						Fact f = new Fact(entity, "<hasAirportCode>", IATA);
						write(AIRPORT_CODE_NEEDRED, f, AIRPORT_CODE_SOURCE,
								source, "FlightIATAcodeExtractor");
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new FlightIATAcodeExtractor(
				new File(
						"./testCases/fromWikipedia.FlightIATAcodeExtractor/iatatest.xml"))
				.extract(new File(
						"./testCases/fromWikipedia.FlightIATAcodeExtractor/"),
						"test");
	}
}
