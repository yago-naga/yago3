package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.NumberParser;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import extractors.EnglishWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * Extracts coord data from Wikipedia. Implements
 * http://en.wikipedia.org/wiki/Template:Coord
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class CoordinateExtractor extends EnglishWikipediaExtractor {

	/** gender facts, checked if the entity is a person */
	public static final Theme COORDINATES = new Theme("coordinateFacts",
			"Coordinates from Wikipedia articles");

	/** sources */
	public static final Theme COORDINATE_SOURCES = new Theme(
			"coordinateSources", "Sources for coordinate facts from Wikipedia");

	/** Constructor from source file */
	public CoordinateExtractor(File wikipedia) {
		super(wikipedia);
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(WordnetExtractor.PREFMEANINGS,
				PatternHardExtractor.TITLEPATTERNS);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<Theme>(WordnetExtractor.PREFMEANINGS,
				PatternHardExtractor.TITLEPATTERNS);
	}

	@Override
	public Set<Theme> output() {
		return (new FinalSet<Theme>(COORDINATE_SOURCES, COORDINATES));
	}

	public static final String bar = "\\s*\\|\\s*";

	/** {{Coord|57|18|22|N|4|27|32|W|display=title}} */
	public static final Pattern coordPattern1 = Pattern.compile("coord" + bar
			+ "(\\d+)" + bar + "(\\d+)" + bar + "([\\d\\.]+)" + bar + "(.)"
			+ bar + "(\\d+)" + bar + "(\\d+)" + bar + "([\\d\\.]+)" + bar
			+ "(.)");

	/** {{Coord|44.112|N|87.913|W|display=title}} */
	public static final Pattern coordPattern2 = Pattern.compile("coord" + bar
			+ "([\\d\\.]+)" + bar + "(.)" + bar + "([\\d\\.]+)" + bar + "(.)");

	/** {{Coord|44.112|-87.913|display=title}} */
	public static final Pattern coordPattern3 = Pattern.compile("coord" + bar
			+ "([\\-\\d\\.]+)" + bar + "([\\-\\d\\.]+)");

	protected void writeCoords(String entity, String lat, String lon)
			throws IOException {
		Double la = NumberParser.getDouble(lat);
		if (la != null)
			write(COORDINATES,
					new Fact(entity, "<hasLatitude>", FactComponent
							.forDegree(la)), COORDINATE_SOURCES,
					FactComponent.wikipediaURL(entity), "CoordinateExtractor");
		Double lo = NumberParser.getDouble(lon);
		if (lo != null)
			write(COORDINATES,
					new Fact(entity, "<hasLongitude>", FactComponent
							.forDegree(lo)), COORDINATE_SOURCES,
					FactComponent.wikipediaURL(entity), "CoordinateExtractor");
		Announce.debug(" Latitude", lat, la);
		Announce.debug(" Longitude", lon, lo);
	}

	@Override
	public void extract() throws Exception {
		TitleExtractor titleExtractor = new TitleExtractor("en");
		Reader in = FileUtils.getBufferedUTF8Reader(inputData);
		String titleEntity = null;
		// Announce.progressStart("Extracting coordinates", 4_500_000);
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "{{coord",
					"{{ coord")) {
			case -1:
				// Announce.progressDone();
				in.close();
				return;
			case 0:
				// Announce.progressStep();
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
			case 2:
				if (titleEntity == null)
					break;
				String val = "coord " + FileLines.readTo(in, '}').toString();
				if (!val.contains("title"))
					break;
				Announce.debug(val);
				Matcher m = coordPattern1.matcher(val);
				if (m.find()) {
					writeCoords(titleEntity,
							m.group(1) + " degrees " + m.group(2) + " minutes "
									+ m.group(3) + " seconds " + m.group(4),
							m.group(5) + " degrees " + m.group(6) + " minutes "
									+ m.group(7) + " seconds " + m.group(8));
					break;
				}
				m = coordPattern2.matcher(val);
				if (m.find()) {
					writeCoords(
							titleEntity,
							m.group(1) + " degrees 0 minutes 0 seconds "
									+ m.group(2),
							m.group(3) + " degrees 0 minutes 0 seconds "
									+ m.group(4));
					break;
				}
				m = coordPattern3.matcher(val);
				if (m.find()) {
					writeCoords(titleEntity, m.group(1)
							+ " degrees 0 minutes 0 seconds N", m.group(2)
							+ " degrees 0 minutes 0 seconds E");
					break;
				}

			}
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new CoordinateExtractor(
				new File(
						"c:/Fabian/eclipseProjects\\yago2s\\testCases\\fromWikipedia.CoordinateExtractor\\de/wikipedia/de_wikitest.xml"))
				.extract(new File("c:/fabian/data/yago3"), "test");
	}

}
