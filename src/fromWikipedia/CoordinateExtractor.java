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
import javatools.util.FileUtils;
import utils.Theme;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
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

	/** vertical bar*/
	public static final String bar = "\\s*\\|\\s*(?:display\\s*=\\s*title\\s*\\|)?";

	/** Direction indicator*/
	public static final String dir = "([NSWE])";

	/** Integer*/
	public static final String integer = "(-?\\d+)";

	/** Float*/
	public static final String flote= "(-?[\\d\\.]+)";

	/** {{Coord|57|18|22|N|4|27|32|W|display=title}} */
	public static final Pattern coordPattern1 = Pattern.compile("(?i)coord"
			+ bar + integer + bar + integer + bar + flote + bar
			+ dir + bar + integer + bar + integer + bar + flote
			+ bar + dir);

	/** {{Coord|44.112|N|87.913|W|display=title}} */
	public static final Pattern coordPattern2 = Pattern.compile("(?i)coord"
			+ bar + flote + bar + dir + bar + flote + bar
			+ dir);

	/** {{Coord|57|18|N|4|27|W|display=title}} */
	public static final Pattern coordPattern3 = Pattern.compile("(?i)coord"
			+ bar + integer + bar + flote + bar
			+ dir + bar + integer + bar + flote
			+ bar + dir);

	/** {{Coord|44.112|-87.913|display=title}} */
	public static final Pattern coordPattern4 = Pattern.compile("(?i)coord"
			+ bar + flote + bar + flote);

	protected void writeCoords(String entity, Double la, Double lo)
			throws IOException {
		if (la != null)
			write(COORDINATES,
					new Fact(entity, "<hasLatitude>", FactComponent
							.forDegree(la)), COORDINATE_SOURCES,
					FactComponent.wikipediaURL(entity), "CoordinateExtractor");
		if (lo != null)
			write(COORDINATES,
					new Fact(entity, "<hasLongitude>", FactComponent
							.forDegree(lo)), COORDINATE_SOURCES,
					FactComponent.wikipediaURL(entity), "CoordinateExtractor");
		Announce.debug(" Latitude", la);
		Announce.debug(" Longitude", lo);
	}

	/** Converts a latitude/longitude expression to a Double (or NULL) */
	public static Double getCoord(String degree, String minute, String second,
			String eastWestNorthSouth) {
		int sign = 1;
		if (!eastWestNorthSouth.isEmpty()
				&& "SswW".indexOf(eastWestNorthSouth.charAt(0)) != -1)
			sign = -1;
		try {
			double deg = Double.parseDouble(degree);
			double min = Double.parseDouble(minute);
			double sec = Double.parseDouble(second);
			return ((deg + min / 60 + sec / 60 / 60) * sign);
		} catch (NumberFormatException ex) {
			return (null);
		}
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
					writeCoords(
							titleEntity,
							getCoord(m.group(1), m.group(2), m.group(3),
									m.group(4)),
							getCoord(m.group(5), m.group(6), m.group(7),
									m.group(8)));
					break;
				}
				m = coordPattern2.matcher(val);
				if (m.find()) {
					writeCoords(titleEntity,
							getCoord(m.group(1), "0", "0", m.group(2)),
							getCoord(m.group(3), "0", "0", m.group(4)));
					break;
				}
				m = coordPattern3.matcher(val);
				if (m.find()) {
					writeCoords(titleEntity,
							getCoord(m.group(1), m.group(2), "0", m.group(3)),
							getCoord(m.group(4), m.group(5), "0", m.group(6)));
					break;
				}
				m = coordPattern4.matcher(val);
				if (m.find()) {
					writeCoords(titleEntity,
							getCoord(m.group(1), "0", "0", "N"),
							getCoord(m.group(2), "0", "0", "E"));
					break;
				}

			}
		}
	}

	public static void main(String[] args) throws Exception {
		//Announce.setLevel(Announce.Level.DEBUG);
		new CoordinateExtractor(
				new File(
						"/san/jbiega/multiWikiSymlinks/en_wiki-20121201-pages-articles.xml"))
				.extract(new File("/san/suchanek/yago3"), "Single run.");
	}

}
