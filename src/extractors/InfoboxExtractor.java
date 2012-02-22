package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalMap;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.N4Writer;
import extractorUtils.TermExtractor;

public class InfoboxExtractor extends Extractor {

	/** Input file */
	protected File wikipedia;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				PatternHardExtractor.CATEGORYPATTERNS,
				WordnetExtractor.WORDNETWORDS,
				PatternHardExtractor.TITLEPATTERNS,
				HardExtractor.HARDWIREDFACTS));
	}

	/** Infobox facts, non-checked */
	public static final Theme DIRTYINFOBOXFACTS = new Theme(
			"dirtyInfoxboxFacts");
	/** Types derived from infoboxes */
	public static final Theme INFOBOXTYPES = new Theme("infoboxTypes");

	@Override
	public Map<Theme, String> output() {
		return new FinalMap<Theme, String>(
				DIRTYINFOBOXFACTS,
				"Facts extracted from the Wikipedia infoboxes - still to be type-checked",
				INFOBOXTYPES, "Types extracted from Wikipedia infoboxes");
	}

	/** normalizes an attribute name*/
	public static String normalizeAttribute(String a) {
		return(a.trim().toLowerCase().replace("_","").replace(" ", "").replaceAll("\\d", ""));
	}
	
	/** Extracts a relation from a string */
	protected void extract(String entity, String string, String relation,
			Map<String,String> preferredMeanings, FactCollection factCollection, N4Writer n4Writer, List<Pair<Pattern, String>> replacements) throws IOException {
		for(Pair<Pattern,String> replacement : replacements) {
			string=replacement.first.matcher(string).replaceAll(replacement.second);
		}
		string=string.trim();
		if(string.length()==0) return;
		String cls;
		boolean inverse;
		if(relation.endsWith("-")) {
			inverse=true;
			relation=Char.cutLast(string);
			cls=factCollection.getArg2(relation, "rdfs:domain");
		} else {
			inverse=false;
			cls=factCollection.getArg2(relation, "rdfs:range");			
		}
		if(cls==null) {
			Announce.warning("Unknown relation to extract:",relation);
			cls="rdfs:Resource";
		}
		TermExtractor extractor=cls.equals("rdf:Class")?new TermExtractor.ForClass(preferredMeanings):TermExtractor.forType(cls);
		Announce.debug("Relation",relation,"with type checker",extractor.getClass().getSimpleName());
		String syntaxChecker=factCollection.getArg2(cls, "<_hasTypeCheckPattern>");
		if(syntaxChecker!=null) syntaxChecker=FactComponent.stripQuotes(Char.decodeBackslash(syntaxChecker));
		for(String object : extractor.extractList(string)) {
			if(syntaxChecker!=null && !object.matches(syntaxChecker)) {
				Announce.debug("Extraction",object, "does not match typecheck",syntaxChecker);
				continue;
			}
			if(inverse) n4Writer.write(new Fact(object, relation,entity));
			else n4Writer.write(new Fact(entity, relation,object));
			if(factCollection.contains(relation, "rdf:type", "<yagoFunction>")) break;
		}
	}
	
	/** reads an environment, returns the char on which we finish */
	public static int readEnvironment(Reader in, StringBuilder b)
			throws IOException {
		while (true) {
			int c;
			switch (c = in.read()) {
			case -1:
				return (-1);
			case '}':
				return (c);
			case '{':
				in.read();
				b.append("{{");
				while (c != -1 && c != '}') {
					b.append((char) c);
					c = readEnvironment(in, b);
				}
				in.read();
				b.append("}}");
				break;
			case '[':
				in.read();
				b.append("[[");
				while (c != -1 && c != ']') {
					b.append((char) c);
					c = readEnvironment(in, b);
				}
				in.read();
				b.append("]]");
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
	public static Map<String, String> readInfobox(Reader in) throws IOException {
		Map<String, String> result = new TreeMap<String, String>();
		while (true) {
			String attribute = normalizeAttribute(FileLines.readTo(in, '=', '}').toString());
			if (attribute.length() == 0)
				return (result);
			StringBuilder value = new StringBuilder();
			int c = readEnvironment(in, value);
			result.put(attribute, value.toString());
			if (c == '}' || c == -1)
				break;
		}
		return (result);
	}

	@Override
	public void extract(Map<Theme, N4Writer> writers,
			Map<Theme, FactCollection> factCollections) throws Exception {
		Announce.doing("Compiling infobox patterns");
		Map<String, Set<String>> patterns = new HashMap<String, Set<String>>();		
		for (Fact fact : factCollections.get(
				PatternHardExtractor.INFOBOXPATTERNS).get("<_infoboxPattern>")) {
			D.addKeyValue(patterns,normalizeAttribute(fact.getArgNoQuotes(1)), fact.getArg(2),
					TreeSet.class);
		}
		if (patterns.isEmpty()) {
			Announce.failed();
			throw new Exception("No infobox patterns found");
		}
		Announce.done();
		Announce.doing("Compiling infobox replacements");
		List<Pair<Pattern,String>> replacements = new ArrayList<Pair<Pattern,String>>();		
		for (Fact fact : factCollections.get(
				PatternHardExtractor.INFOBOXPATTERNS).get("<_infoboxReplace>")) {
			replacements.add(new Pair<Pattern,String>(fact.getArgPattern(1),fact.getArgNoQuotes(2)));
		}
		Announce.done();

		// Still preparing...
		Announce.doing("Compiling non-conceptual words");
		Set<String> nonconceptual = new TreeSet<String>();
		for (Fact fact : factCollections.get(PatternHardExtractor.CATEGORYPATTERNS)
				.getBySecondArgSlow("rdf:type", "<_yagoNonConceptualWord>")) {
			nonconceptual.add(fact.getArgNoQuotes(1));
		}
		if (nonconceptual.isEmpty()) {
			Announce.failed();
			throw new Exception("No non-conceptual words found");
		}
		Map<String, String> preferredMeaning = new HashMap<String, String>();
		for (FactCollection fc : factCollections.values()) {
			for (Fact fact : fc.get("<isPreferredMeaningOf>")) {
				preferredMeaning.put(fact.getArgNoQuotes(2), fact.getArg(1));
			}
		}
		if (preferredMeaning.isEmpty()) {
			Announce.failed();
			throw new Exception("No preferred meanings found");
		}
		Announce.done();

		// Extract the information
		Announce.doing("Extracting");
		Reader in = new BufferedReader(new FileReader(wikipedia));
		String titleEntity = null;
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "{{Infobox",
					"{{ Infobox")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = CategoryExtractor.getTitleEntity(in,
						factCollections.get(PatternHardExtractor.TITLEPATTERNS));
				break;
			default:
				if (titleEntity == null)
					continue;
				String cls = FileLines.readTo(in, '}', '|').toString();
				String type = CategoryExtractor.category2class(cls,
						nonconceptual, preferredMeaning);
				if (type != null) {
					writers.get(INFOBOXTYPES).write(
							new Fact(null, titleEntity, "rdf:type", type));
				}
				Map<String, String> attributes = readInfobox(in);
				for(String attribute : attributes.keySet()) {
					Set<String> relations=patterns.get(attribute);
					if(relations==null) continue;
					for(String relation : relations) {
						extract(titleEntity, attributes.get(attribute),relation,preferredMeaning, factCollections.get(HardExtractor.HARDWIREDFACTS),writers.get(DIRTYINFOBOXFACTS), replacements);
					}
				}
			}
		}
	}	

	/** Constructor from source file */
	public InfoboxExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

	public static void main(String[] args) throws Exception {
		new PatternHardExtractor(new File("./data")).extract(new File(
				"c:/fabian/data/yago2s"), "test");
		Announce.setLevel(Announce.Level.DEBUG);
		new InfoboxExtractor(new File("./testCases/wikitest.xml")).extract(
				new File("c:/fabian/data/yago2s"), "test");
	}
}
