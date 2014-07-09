package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.MultilingualTheme;
import utils.PatternList;
import utils.Theme;
import utils.Theme.ThemeGroup;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import extractors.MultilingualWikipediaExtractor;
import followUp.FollowUpExtractor;
import followUp.InfoboxTemplateTranslator;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * YAGO2s - InfoboxExtractor
 * 
 * Extracts facts from infoboxes for all languages.
 * 
 * @author Fabian M. Suchanek
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxExtractor extends MultilingualWikipediaExtractor {

	public static final MultilingualTheme INFOBOX_ATTRIBUTES = new MultilingualTheme(
			"yagoInfoboxAttributes", "Raw facts from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	public static final MultilingualTheme INFOBOX_ATTRIBUTE_SOURCES = new MultilingualTheme(
			"yagoInfoboxAttributeSources",
			"Sources for the raw facts from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	public static final MultilingualTheme INFOBOX_TEMPLATES = new MultilingualTheme(
			"yagoInfoboxTemplates",
			"Raw infobox templates from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	public static final MultilingualTheme INFOBOX_TEMPLATES_TRANSLATED = new MultilingualTheme(
			"infoboxTemplatesTranslated",
			"Templates from the Wikipedia infoboxes, translated",
			ThemeGroup.WIKIPEDIA);

	public static final MultilingualTheme INFOBOX_TEMPLATE_SOURCES = new MultilingualTheme(
			"yagoInfoboxTemplateSources",
			"Sources for the raw types from the Wikipedia infoboxes",
			ThemeGroup.WIKIPEDIA);

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(PatternHardExtractor.INFOBOXPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<Theme>(PatternHardExtractor.INFOBOXPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS);
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(INFOBOX_ATTRIBUTES.inLanguage(language),
				INFOBOX_ATTRIBUTE_SOURCES.inLanguage(language),
				INFOBOX_TEMPLATES.inLanguage(language),
				INFOBOX_TEMPLATE_SOURCES.inLanguage(language));
	}

	@Override
	public Set<FollowUpExtractor> followUp() {
		if (isEnglish())
			return (Collections.emptySet());
		return (new FinalSet<FollowUpExtractor>(new InfoboxTemplateTranslator(
				INFOBOX_TEMPLATES.inLanguage(this.language),
				INFOBOX_TEMPLATES_TRANSLATED.inLanguage(this.language), this)));
	}

	/** normalizes an attribute name */
	@ImplementationNote("Be aggressive here: numbers have to go away, so that city1=city2."
			+ "Bad characters such as TABs are poisonous and have to leave. "
			+ "Spaces and underbars have to go."
			+ "Still accept non-latin characters."
			+ "Lowercase because manual infobox patterns might be in uppercase."
			+ "Vertical bars have to stay, "
			+ "because they indicate several collated attributes that we will split later.")
	public static String normalizeAttribute(String a) {
		return (a.trim().toLowerCase().replaceAll("[^\\p{L}|]", ""));
	}

	/** For cleaning up values */
	protected PatternList valueCleaner;
	
	protected Map<String, String> unitDictionary;

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
						Char17.decodeAmpersand(Char17.decodeAmpersand(valueStr)),
						TreeSet.class);
			}
			if (c == '}' || c == -1 || c == -2) {
				break;
			}
		}

		return (result);
	}

	@Override
	@ImplementationNote("We cannot search for '{{ infobox' because the Polish Wikipedia puts the template name first")
	public void extract() throws Exception {

		TitleExtractor titleExtractor = new TitleExtractor(language);
		valueCleaner = new PatternList(
				PatternHardExtractor.INFOBOXPATTERNS.factCollection(),
				"<_infoboxReplace>");
		Announce.doing("Loading patterns of", "<_hasPredefinedUnit>");
		unitDictionary = PatternHardExtractor.INFOBOXPATTERNS.factCollection()
				.getMap("<_hasPredefinedUnit>");
		Announce.done();
		
		String typeRelation = FactComponent
				.forInfoboxTypeRelation(this.language);
		// Extract the information
		// Announce.progressStart("Extracting", 4_500_000);
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		while (true) {
			/* nested comments not supported */
			switch (FileLines.findIgnoreCase(in, "<title>", "<comment>", "{{")) {
			case -1:
				// Announce.progressDone();
				in.close();
				return;
			case 0:
				// Announce.progressStep();
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
				FileLines.readToBoundary(in, "</comment>");
				break;
			default:
				if (titleEntity == null)
					continue;
				String cls = FileLines.readTo(in, '}', '|').toString();

				if (!cls.contains("infobox") && !cls.contains("Infobox")) {
					continue;
				}
				cls = cls.replace("infobox", "").replace("Infobox", "");

				cls = Char17.decodeAmpersand(cls);
				cls = valueCleaner.transform(cls);
				// Let's avoid writing nonsense here
				if (cls != null && cls.length() > 3) {
					cls = FactComponent.forInfoboxTemplate(cls, language);
					write(INFOBOX_TEMPLATES.inLanguage(language), new Fact(
							titleEntity, typeRelation, cls),
							INFOBOX_TEMPLATE_SOURCES.inLanguage(language),
							FactComponent.wikipediaURL(titleEntity),
							"InfoboxExtractor");
				}
				Map<String, Set<String>> attributes = readInfobox(in);

				for (String attribute : attributes.keySet()) {
					for (String value : attributes.get(attribute)) {
						value = valueCleaner.transform(value);
						// Here, too, avoid nonsense
						if (value != null && !value.isEmpty()) {
							
							String object;
							attribute = FactComponent.forInfoboxAttribute(this.language, attribute);
							//If the object should have an unit specified, and the object is numerical
							if(unitDictionary.containsKey(attribute) && value.matches("[-+]?\\d+(\\.\\d+)?")) {
								
								object = FactComponent.forStringWithDatatype(value,
												unitDictionary.get(attribute));
								
							} else {
								
								object = FactComponent.forStringWithLanguage(value, language);
							
							}
							
							write(INFOBOX_ATTRIBUTES.inLanguage(language),
									new Fact(titleEntity, attribute, object),
									INFOBOX_ATTRIBUTE_SOURCES
											.inLanguage(language),
									FactComponent.wikipediaURL(titleEntity),
									"Infobox Extractor");
						}
					}
				}

			}
		}
	}

	/** Constructor from source file */
	public InfoboxExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

	public static void main(String[] args) throws Exception {
		new InfoboxExtractor("pl", new File(
				"/home/jbiega/Downloads/pl_wiki.xml")).extract(new File(
				"/home/jbiega/data/yago2s"), "Test");

	}
}