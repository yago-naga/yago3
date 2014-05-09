package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.InfoboxTermExtractor;

/**
 * YAGO2s - InfoboxExtractor
 * 
 * Extracts facts from infoboxes for all languages.
 * 
 * @author Fabian M. Suchanek
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxExtractor extends MultilingualExtractor {

	/** Input file */
	protected File wikipedia;

	public static final Theme INFOBOX_ATTRIBUTES = new Theme(
			"yagoInfoboxAttributes", "en",
			"Raw facts from the Wikipedia infoboxes", ThemeGroup.WIKIPEDIA);

	public static final Theme INFOBOX_ATTRIBUTE_SOURCES = new Theme(
			"yagoInfoboxAttributeSources", "en",
			"Sources for the raw facts from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	public static final Theme INFOBOX_TYPES = new Theme("yagoInfoboxTypes",
			"en", "Raw types from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	public static final Theme INFOBOX_TYPES_TRANSLATED = new Theme(
			"infoboxTypesTranslated", "en",
			"Types from the Wikipedia infoboxes, translated",
			ThemeGroup.WIKIPEDIA);

	public static final Theme INFOBOX_TYPE_SOURCES = new Theme(
			"yagoInfoboxTypeSources", "en",
			"Sources for the raw types from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	@Override
	public File inputDataFile() {
		return wikipedia;
	}

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(INFOBOX_ATTRIBUTES.inLanguage(language),
				INFOBOX_ATTRIBUTE_SOURCES.inLanguage(language),
				INFOBOX_TYPES.inLanguage(language),
				INFOBOX_TYPE_SOURCES.inLanguage(language));
	}

	@Override
	public Set<Extractor> followUp() {
		Set<Extractor> input = new HashSet<Extractor>(Arrays.asList(
				new Translator(INFOBOX_TYPES.inLanguage(this.language),
						INFOBOX_TYPES_TRANSLATED.inLanguage(this.language),
						this.language, Translator.ObjectType.InfoboxType),
				new InfoboxTypeExtractor(this.language),
				new InfoboxTermExtractor(this.language), new InfoboxMapper(
						this.language)));

		if (!this.language.equals("en")) {
			input.add(new AttributeMatcher(this.language));
		}
		return input;
	}

	/** normalizes an attribute name */
	public static String normalizeAttribute(String a) {
		return (a.trim().toLowerCase().replace("_", "").replace(" ", "")
				.replace("-", "").replaceAll("\\d", ""));
	}

	// /** Extracts a relation from a string */
	// protected void extract(String entity, String string, String relation,
	// String attribute, Map<String, String> preferredMeanings,
	// FactCollection factCollection, Map<Theme, FactWriter> writers,
	// PatternList replacements) throws IOException {
	//
	// string = replacements.transform(Char.decodeAmpersand(string));
	// string = string.replace("$0", FactComponent.stripBrackets(entity));
	//
	// string = string.trim();
	// if (string.length() == 0)
	// return;
	//
	// // Check inverse
	// boolean inverse;
	// String expectedDatatype;
	// if (relation.endsWith("->")) {
	// inverse = true;
	// relation = Char.cutLast(Char.cutLast(relation)) + '>';
	// expectedDatatype = factCollection.getArg2(relation, RDFS.domain);
	// } else {
	// inverse = false;
	// expectedDatatype = factCollection.getArg2(relation, RDFS.range);
	// }
	// if (expectedDatatype == null) {
	// Announce.warning("Unknown relation to extract:", relation);
	// expectedDatatype = YAGO.entity;
	// }
	//
	// // Get the term extractor
	// TermExtractor extractor = expectedDatatype.equals(RDFS.clss) ? new
	// TermExtractor.ForClass(
	// preferredMeanings) : TermExtractor.forType(expectedDatatype);
	// String syntaxChecker = FactComponent.asJavaString(factCollection
	// .getArg2(expectedDatatype, "<_hasTypeCheckPattern>"));
	//
	// // Extract all terms
	// List<String> objects = extractor.extractList(string);
	// for (String object : objects) {
	// // Check syntax
	// if (syntaxChecker != null
	// && FactComponent.asJavaString(object) != null
	// && !FactComponent.asJavaString(object).matches(
	// syntaxChecker)) {
	// Announce.debug("Extraction", object, "for", entity, relation,
	// "does not match syntax check", syntaxChecker);
	// continue;
	// }
	// // Check data type
	// if (FactComponent.isLiteral(object)) {
	// String parsedDatatype = FactComponent.getDatatype(object);
	// if (parsedDatatype == null)
	// parsedDatatype = YAGO.string;
	// if (syntaxChecker != null
	// && factCollection.isSubClassOf(expectedDatatype,
	// parsedDatatype)) {
	// // If the syntax check went through, we are fine
	// object = FactComponent
	// .setDataType(object, expectedDatatype);
	// } else {
	// // For other stuff, we check if the datatype is OK
	// if (!factCollection.isSubClassOf(parsedDatatype,
	// expectedDatatype)) {
	// Announce.debug("Extraction", object, "for", entity,
	// relation, "does not match type check",
	// expectedDatatype);
	// continue;
	// }
	// }
	// }
	// if (inverse) {
	// Fact fact = new Fact(object, relation, entity);
	// write(writers, INFOBOXATTS_MAP.get(language), fact,
	// INFOBOXATTSOURCES_MAP.get(language),
	// FactComponent.wikipediaURL(entity),
	// "InfoboxExtractor from " + attribute);
	// } else {
	// Fact fact = new Fact(entity, relation, object);
	// write(writers, INFOBOXATTS_MAP.get(language), fact,
	// INFOBOXATTSOURCES_MAP.get(language),
	// FactComponent.wikipediaURL(entity),
	// "InfoboxExtractor from " + attribute);
	// }
	//
	// if (factCollection.contains(relation, RDFS.type, YAGO.function))
	// break;
	// }
	// }

	/** reads an environment, returns the char on which we finish */
	public static int readEnvironment(Reader in, StringBuilder b)
			throws IOException {
		final int MAX = 4000;
		while (true) {
			if (b.length() > MAX)
				return (-2);
			int c;
			switch (c = in.read()) {
			case -1:
				return (-1);
			case '}':
				return ('}');
			case '{':
				while (c != -1 && c != '}') {
					b.append((char) c);
					c = readEnvironment(in, b);
					if (c == -2)
						return (-2);
				}
				b.append("}");
				break;
			case '[':
				while (c != -1 && c != ']') {
					b.append((char) c);
					c = readEnvironment(in, b);
					if (c == -2)
						return (-2);
				}
				b.append("]");
				break;
			case ']':
				return (']');
			case '|':
				return ('|');
			default:
				b.append((char) c);
			}
		}
	}

	/** reads an infobox */
	public static Map<String, Set<String>> readInfobox(Reader in)
			throws IOException {
		Map<String, Set<String>> result = new TreeMap<String, Set<String>>();

		while (true) {
			StringBuilder attr = new StringBuilder();
			int r = FileLines.find(in, attr, "</page>", "=", "}");
			if (r == -1 || r == 0) {
				return result;
			}
			String attribute = normalizeAttribute(attr.toString().trim());
			if (attribute.length() == 0)
				return result;

			StringBuilder value = new StringBuilder();
			int c = readEnvironment(in, value);
			String valueStr = value.toString().trim();
			if (!valueStr.isEmpty()) {
				if (attribute.contains("|")) {
					String[] parts = attribute.split("\\|");
					if (parts.length > 0)
						attribute = parts[parts.length - 1];
				}

				D.addKeyValue(result, attribute,
						Char.decodeAmpersand(valueStr), TreeSet.class);
			}
			if (c == '}' || c == -1 || c == -2) {
				break;
			}
		}

		return (result);
	}

	@Override
	public void extract() throws Exception {

		TitleExtractor titleExtractor = new TitleExtractor(language);
		String typeRelation = FactComponent
				.forInfoboxTypeRelation(this.language);

		// Extract the information
		// Announce.progressStart("Extracting", 4_500_000);
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		while (true) {
			/* nested comments not supported */
			switch (FileLines.findIgnoreCase(in, "<title>", "{{Infobox",
					"{{ Infobox", "<comment>")) {
			case -1:
				// Announce.progressDone();
				in.close();
				return;
			case 0:
				// Announce.progressStep();
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 3:
				FileLines.readToBoundary(in, "</comment>");
				break;
			default:
				if (titleEntity == null)
					continue;
				String cls = FileLines.readTo(in, '}', '|').toString().trim()
						.toLowerCase();

				if (Character.isDigit(Char.last(cls)))
					cls = Char.cutLast(cls);

				INFOBOX_TYPES.inLanguage(language).write(
						new Fact(titleEntity, typeRelation, FactComponent
								.forString(cls)));

				Map<String, Set<String>> attributes = readInfobox(in);

				/* new version */
				for (String attribute : attributes.keySet()) {
					for (String value : attributes.get(attribute)) {
						write(INFOBOX_ATTRIBUTES.inLanguage(language),
								new Fact(titleEntity, FactComponent
										.forInfoboxAttribute(this.language,
												attribute), FactComponent
										.forString(value)),
								INFOBOX_ATTRIBUTE_SOURCES.inLanguage(language),
								FactComponent.wikipediaURL(titleEntity),
								"Infobox Extractor");
					}
				}

			}
		}
	}

	/** Constructor from source file */
	public InfoboxExtractor(File wikipedia, String lang) {
		this.wikipedia = wikipedia;
		this.language = lang;
	}

	public InfoboxExtractor(File wikipedia) {
		this(wikipedia, decodeLang(wikipedia.getName()));
	}

	public static void main(String[] args) throws Exception {
		new InfoboxExtractor(new File("/home/jbiega/Downloads/en_pol.xml"))
				.extract(new File("/home/jbiega/data/yago2s/"),
						"Test on 1 wikipedia article");

	}
}