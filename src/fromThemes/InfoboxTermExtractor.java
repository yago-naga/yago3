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
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.Translator;
import fromWikipedia.Extractor;
import fromWikipedia.InfoboxExtractor;

public class InfoboxTermExtractor extends Extractor {

	public static final HashMap<String, Theme> INFOBOXTERMS_TOREDIRECT_MAP = new HashMap<String, Theme>();
	public static final HashMap<String, Theme> INFOBOXTERMS_MAP = new HashMap<String, Theme>();
	public static final HashMap<String, Theme> INFOBOXATTSTRANSLATED_MAP = new HashMap<String, Theme>();

	static {
		for (String s : Extractor.languages) {
			INFOBOXTERMS_TOREDIRECT_MAP
					.put(s,
							new Theme(
									"infoboxTermsToBeRedirected"
											+ Extractor.langPostfixes.get(s),
									"Attribute terms of infobox, still to be redirected",
									ThemeGroup.OTHER));
			INFOBOXTERMS_MAP.put(s, new Theme("infoboxTerms"
					+ Extractor.langPostfixes.get(s),
					"Attribute terms of infobox", ThemeGroup.OTHER));
			INFOBOXATTSTRANSLATED_MAP.put(s,
					new Theme("infoboxAttributesTranslated"
							+ Extractor.langPostfixes.get(s),
							"Attribute terms of infobox translated",
							ThemeGroup.OTHER));
		}
	}

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				InfoboxExtractor.INFOBOXATTS_MAP.get(this.language)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				INFOBOXTERMS_TOREDIRECT_MAP.get(this.language));
	}

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(
				new Redirector(INFOBOXTERMS_TOREDIRECT_MAP.get(language),
						INFOBOXTERMS_MAP.get(language), this, this.language),
				new Translator(INFOBOXTERMS_MAP.get(language),
						INFOBOXATTSTRANSLATED_MAP.get(this.language),
						this.language, "Entity")));
	}

	@Override
	public void extract(Map<Theme, FactWriter> output,
			Map<Theme, FactSource> input) throws Exception {

		FactWriter out = output.get(INFOBOXTERMS_TOREDIRECT_MAP.get(language));

		FactCollection infoboxPatterns = new FactCollection(
				input.get(PatternHardExtractor.INFOBOXPATTERNS));
		PatternList replacements = new PatternList(infoboxPatterns,
				"<_infoboxReplace>");

		Map<String, String> combinations = infoboxPatterns
				.asStringMap("<_infoboxCombine>");

		Map<String, Set<String>> attributes = new TreeMap<String, Set<String>>();
		String prevEntity = "";

		for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP
				.get(this.language))) {

			String attribute = FactComponent.stripBrackets(FactComponent
					.stripPrefix(f.getRelation()));
			String value = f.getArgJavaString(2);

			if (value == null) {
				continue;
			}
			if (!f.getArg(1).equals(prevEntity)) {
				process(prevEntity, attributes, combinations, replacements, out);

				prevEntity = f.getArg(1);
				attributes.clear();
				D.addKeyValue(attributes, attribute, value, TreeSet.class);
			} else {
				D.addKeyValue(attributes, attribute, value, TreeSet.class);
			}
		}
		process(prevEntity, attributes, combinations, replacements, out);
	}

	protected void process(String entity, Map<String, Set<String>> attributes,
			Map<String, String> combinations, PatternList replacements,
			FactWriter out) throws IOException {

		attributes = processCombinations(entity, attributes, combinations);

		for (String attr : attributes.keySet()) {
			for (String val : attributes.get(attr)) {
				for (TermExtractor extractor : TermExtractor.all()) {

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

	private Map<String, Set<String>> processCombinations(String entity,
			Map<String, Set<String>> attributes,
			Map<String, String> combinations) throws IOException {
		if (!attributes.isEmpty()) {
			attributes = applyCombination(attributes, combinations);
		}
		return attributes;
	}

	public Map<String, Set<String>> applyCombination(
			Map<String, Set<String>> result, Map<String, String> combinations) {
		// Map<String, Set<String>> result = new TreeMap<String, Set<String>>();

		// D.addKeyValue(result, originalAttribute, originalValue,
		// TreeSet.class);

		// for (Fact f : input){
		// Apply combinations
		next: for (String code : combinations.keySet()) {
			StringBuilder val = new StringBuilder();

			for (String attribute : code.split(">")) {
				int scanTo = attribute.indexOf('<');
				if (scanTo != -1) {
					val.append(attribute.substring(0, scanTo));
					String attr = attribute.substring(scanTo + 1);
					// Do we want to exclude the existence of an attribute?
					if (attr.startsWith("~")) {
						attr = attr.substring(1);
						if (result.get(InfoboxExtractor
								.normalizeAttribute(attr)) != null) {
							continue next;
						}
						continue;
					}
					String newVal = D.pick(result.get(InfoboxExtractor
							.normalizeAttribute(attr)));
					if (newVal == null) {
						continue next;
					}
					val.append(newVal);
				} else {
					val.append(attribute);
				}
			}

			D.addKeyValue(
					result,
					InfoboxExtractor.normalizeAttribute(combinations.get(code)),
					val.toString(), TreeSet.class);
		}

		return result;
	}

	public InfoboxTermExtractor(String lang) {
		super();
		this.language = lang;
	}

	public static void main(String[] args) throws Exception {
		InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
		extractor.extract(new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");

		new Redirector(INFOBOXTERMS_TOREDIRECT_MAP.get("en"),
				INFOBOXTERMS_MAP.get("en"), extractor, "en").extract(new File(
				"/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");

		new Translator(INFOBOXTERMS_MAP.get("en"),
				INFOBOXATTSTRANSLATED_MAP.get("en"), "en", "Entity").extract(
				new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");
	}

}
