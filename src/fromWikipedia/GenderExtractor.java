package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * GenderExtractor - YAGO2s
 * 
 * Extracts the gender for persons in wikipedia
 * 
 * @author Edwin
 * 
 */
public class GenderExtractor extends Extractor {

	/** Wikipedia Input file */
	protected File wikipedia;

	@Override
	public File inputDataFile() {
		return wikipedia;
	}

	/** gender facts, checked if the entity is a person */
	public static final Theme PERSONS_GENDER = new Theme("personGenderFacts",
			"Gender of a person");

	/** sources */
	public static final Theme PERSONS_GENDER_SOURCES = new Theme(
			"personGenderSources", "Sources for the gender of a person");

	/** Constructor from source file */
	public GenderExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(
				TransitiveTypeExtractor.TRANSITIVETYPE,
				PatternHardExtractor.TITLEPATTERNS));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
	}

	@Override
	public Set<Theme> output() {
		return (new FinalSet<Theme>(PERSONS_GENDER, PERSONS_GENDER_SOURCES));
	}

	/** Pattern for "she" */
	private static final Pattern she = Pattern.compile("\\b(she|her)\\b",
			Pattern.CASE_INSENSITIVE);
	/** Pattern for "he" */
	private static final Pattern he = Pattern.compile("\\b(he|his)\\b",
			Pattern.CASE_INSENSITIVE);

	@Override
	public void extract() throws Exception {
		FactCollection types = TransitiveTypeExtractor.TRANSITIVETYPE
				.factCollection();
		TitleExtractor titleExtractor = new TitleExtractor("en");
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		// Announce.progressStart("Extracting Genders", 4_500_000);
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				// Announce.progressDone();
				in.close();
				return;
			case 0:
				// Announce.progressStep();
				titleEntity = titleExtractor.getTitleEntity(in);
				if (titleEntity != null) {
					if (!types.contains(titleEntity, RDFS.type, YAGO.person))
						continue;
					String page = FileLines.readBetween(in, "<text", "</text>");
					String normalizedPage = page.replaceAll(
							"[\\s\\x00-\\x1F]+", " ");
					int male = 0;
					Matcher gm = he.matcher(normalizedPage);
					while (gm.find())
						male++;
					int female = 0;
					gm = she.matcher(normalizedPage);
					while (gm.find())
						female++;
					if (male > female * 2 || (male > 10 && male > female)) {
						write(PERSONS_GENDER, new Fact(titleEntity,
								"<hasGender>", "<male>"),
								PERSONS_GENDER_SOURCES,
								FactComponent.wikipediaURL(titleEntity),
								"GenderExtractor");
					} else if (female > male * 2
							|| (female > 10 && female > male)) {
						write(PERSONS_GENDER, new Fact(titleEntity,
								"<hasGender>", "<female>"),
								PERSONS_GENDER_SOURCES,
								FactComponent.wikipediaURL(titleEntity),
								"GenderExtractor");
					}
				}
				break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new GenderExtractor(new File(
				"c:/fabian/data/wikipedia/testset/angie.xml")).extract(
				new File("c:/fabian/data/yago2s"), "test");
	}
}
