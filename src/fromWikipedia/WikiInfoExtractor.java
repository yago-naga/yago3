package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import extractors.EnglishWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * YAGO2s - Wikipedia Info Extractor
 * 
 * Extracts the size of the Wikipedia pages, outlinks, etc.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class WikiInfoExtractor extends EnglishWikipediaExtractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE,
				PatternHardExtractor.TITLEPATTERNS);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE,
				PatternHardExtractor.TITLEPATTERNS);
	}

	/** The importance scores for the type facts */
	public static final Theme WIKIINFO = new Theme(
			"yagoWikipediaInfo",
			"Stores the sizes, outlinks, and URLs of the Wikipedia articles of the YAGO entities.",
			Theme.ThemeGroup.OTHER);

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(WIKIINFO);
	}

	@Override
	public void extract() throws Exception {
		Set<String> entities = TransitiveTypeExtractor.TRANSITIVETYPE
				.factCollection().getSubjects();
		TitleExtractor titleExtractor = new TitleExtractor("en");
		// Extract the information
		// Announce.progressStart("Extracting", 3_900_000);
		Reader in = FileUtils.getBufferedUTF8Reader(inputData);
		while (FileLines.scrollTo(in, "<title>")) {
			String entity = titleExtractor.getTitleEntity(in);
			if (entity == null)
				continue;
			if (!FileLines.scrollTo(in, "<text"))
				continue;
			if (!FileLines.scrollTo(in, ">"))
				continue;
			String page = FileLines.readToBoundary(in, "</text>");
			if (page == null)
				continue;
			WIKIINFO.write(new Fact(entity, "<hasWikipediaArticleLength>",
					FactComponent.forNumber(page.length())));
			WIKIINFO.write(new Fact(entity, "<hasWikipediaUrl>", FactComponent
					.wikipediaURL(entity)));
			Set<String> targets = new HashSet<>();
			for (int pos = page.indexOf("[["); pos != -1; pos = page.indexOf(
					"[[", pos + 2)) {
				int endPos = page.indexOf(']', pos);
				if (endPos == -1)
					continue;
				String target = page.substring(pos + 2, endPos);
				endPos = target.indexOf('|');
				if (endPos != -1)
					target = target.substring(0, endPos);
				target = FactComponent.forWikipediaTitle(target);
				if (!entities.contains(target))
					continue;
				targets.add(target);
			}
			for (String target : targets)
				WIKIINFO.write(new Fact(entity, "<linksTo>", target));
		}
	}

	public WikiInfoExtractor(File wikipediaFile) {
		super(wikipediaFile);
	}

	public static void main(String[] args) throws Exception {
		new WikiInfoExtractor(
				new File(
						"c:/Fabian/eclipseProjects/yago2s/testCases/extractors.CategoryExtractor/wikitest.xml"))
				.extract(new File("c:/fabian/data/yago3"),
						"Test on 1 wikipedia article\n");
	}
}
