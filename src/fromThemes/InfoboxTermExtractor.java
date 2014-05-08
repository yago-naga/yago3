package fromThemes;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.PatternList;
import utils.TermExtractor;

import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;

import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.Translator;
import fromWikipedia.Extractor;
import fromWikipedia.InfoboxExtractor;

public class InfoboxTermExtractor extends Extractor {

	public static final Theme INFOBOXTERMS = new Theme("infoboxTerms", "en",
			"The attribute facts of the Wikipedia infoboxes, split into terms");
	public static final Theme INFOBOXTERMS_TOREDIRECT = new Theme(
			"infoboxTermsToBeRedirected",
			"en",
			"The attribute facts of the Wikipedia infoboxes, split into terms, still to be redirected.");
	public static final Theme INFOBOXATTSTRANSLATED = new Theme(
			"infoboxAttributesTranslated",
			"en",
			"The attribute facts of the Wikipedia infoboxes, split into terms, redirected, subject translated");

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.WORDNETWORDS, HardExtractor.HARDWIREDFACTS,
				InfoboxExtractor.INFOBOXRAW.inLanguage(this.language)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				INFOBOXTERMS_TOREDIRECT.inLanguage(this.language));
	}

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(
				new Redirector(INFOBOXTERMS_TOREDIRECT
						.inLanguage(this.language), INFOBOXTERMS
						.inLanguage(this.language), this, this.language),
				new Translator(INFOBOXTERMS.inLanguage(language),
						INFOBOXATTSTRANSLATED.inLanguage(this.language),
						this.language, "Entity")));
	}

	@Override
	public void extract(Map<Theme, FactWriter> output,
			Map<Theme, FactSource> input) throws Exception {

		FactWriter out = output.get(INFOBOXTERMS_TOREDIRECT
				.inLanguage(language));

		PatternList replacements = new PatternList(new FactCollection(
				input.get(PatternHardExtractor.INFOBOXPATTERNS)),
				"<_infoboxReplace>");
		Map<String, String> preferredMeanings = WordnetExtractor
				.preferredMeanings(input);

		Map<String, Set<String>> attributes = new TreeMap<String, Set<String>>();
		String prevEntity = "";

		for (Fact f : input.get(InfoboxExtractor.INFOBOXRAW
				.inLanguage(this.language))) {

			String attribute = FactComponent.stripBrackets(FactComponent
					.stripPrefix(f.getRelation()));
			String value = f.getArgJavaString(2);

			if (value == null)
				continue;

			if (!f.getArg(1).equals(prevEntity)) {
				process(prevEntity, attributes, replacements,
						preferredMeanings, out);

				prevEntity = f.getArg(1);
				attributes.clear();
			}
			D.addKeyValue(attributes, attribute, value, HashSet.class);
		}
		process(prevEntity, attributes, replacements, preferredMeanings, out);
	}

	protected void process(String entity, Map<String, Set<String>> attributes,
			PatternList replacements, Map<String, String> preferredMeanings,
			FactWriter out) throws IOException {

		for (String attr : attributes.keySet()) {
			for (String val : attributes.get(attr)) {
				for (TermExtractor extractor : TermExtractor
						.all(preferredMeanings)) {

					val = replacements.transform(Char.decodeAmpersand(val));
					val = val
							.replace("$0", FactComponent.stripBrackets(entity));
					val = val.trim();
					if (val.length() == 0)
						continue;

					List<String> objects = extractor.extractList(val);

					for (String object : objects) {
						Fact fact = new Fact(entity,
								InfoboxExtractor.addPrefix(this.language,
										FactComponent.forYagoEntity(attr)),
								object);
						out.write(fact);
					}
				}
			}
		}
	}

	public InfoboxTermExtractor(String lang) {
		super();
		this.language = lang;
	}

	public static void main(String[] args) throws Exception {
		InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
		extractor.extract(new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");
	}

}
