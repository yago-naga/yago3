package extractors;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Name;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.N4Writer;

public class WordnetExtractor extends Extractor {

	protected File wordnetFolder;

	@Override
	public List<String> input() {
		return Arrays.asList("hardWiredFacts");
	}

	@Override
	public List<String> output() {
		return Arrays.asList("wordnetClasses", "wordnetWords", "wordnetIds");
	}

	@Override
	public List<String> outputDescriptions() {
		return Arrays.asList("SubclassOf-Hierarchy from WordNet",
				"Labels and preferred meanings form Wordnet",
				"Ids from Wordnet");
	}

	/** Pattern for synset definitions */
	// s(100001740,1,'entity',n,1,11).
	public static Pattern SYNSETPATTERN = Pattern
			.compile("s\\((\\d+),\\d*,'(.*)',(.),(\\d*),(\\d*)\\)\\.");

	/** Pattern for relation definitions */
	// hyp (00001740,00001740).
	public static Pattern RELATIONPATTERN = Pattern
			.compile("\\w*\\((\\d{9}),(.*)\\)\\.");

	@Override
	public void extract(List<N4Writer> writers,
			List<FactCollection> factCollections) throws Exception {
		Announce.doing("Extracting from Wordnet");
		Collection<String> instances = new HashSet<String>(8000);
		for (String line : new FileLines(new File(wordnetFolder, "wn_ins.pl"),
				"Loading instances")) {
			line = line.replace("''", "'");
			Matcher m = RELATIONPATTERN.matcher(line);
			if (!m.matches())
				continue;
			instances.add(m.group(1));
		}
		Map<String, String> id2class = new HashMap<String, String>(80000);
		String lastId = "";
		String lastClass = "";

		for (String line : new FileLines(new File(wordnetFolder, "wn_s.pl"),
				"Loading synsets")) {
			line = line.replace("''", "'"); // TODO: Does this work for
											// wordnet_child's_game_100483935 ?
			Matcher m = SYNSETPATTERN.matcher(line);
			if (!m.matches())
				continue;
			String id = m.group(1);
			String word = m.group(2);
			String type = m.group(3);
			String numMeaning = m.group(4);
			if (instances.contains(id))
				continue;
			// The instance list does not contain all instances...
			if (Name.couldBeName(word))
				continue;
			if (!type.equals("n"))
				continue;
			if (!id.equals(lastId)) {
				id2class.put(lastId = id,
						lastClass = FactComponent.forWordnetEntity(word, id));
				writers.get(1).write(
						new Fact(null, lastClass, "<skos:prefLabel>",
								FactComponent.forString(word, "en", null)));
				writers.get(2).write(
						new Fact(null, lastClass, "<hasSynsetId>",
								FactComponent.forNumber(id)));
			}
			String wordForm=FactComponent.forString(word, "en", null);
			// add additional fact if it is preferred meaning
			if (numMeaning.equals("1")) {				
				// First check whether we do not already have such an element
				if(factCollections.get(0).getBySecondArgSlow("<isPreferredMeaningOf>", wordForm).isEmpty()) {
				writers.get(1).write(
						new Fact(null, lastClass, "<isPreferredMeaningOf>",
								wordForm));
				}
			}
			writers.get(1).write(
					new Fact(null, lastClass, "rdf:label",wordForm));
		}
		instances = null;
		for (String line : new FileLines(new File(wordnetFolder, "wn_hyp.pl"),
				"Loading subclassOf")) {
			line = line.replace("''", "'"); // TODO: Does this work for
											// wordnet_child's_game_100483935 ?
			Matcher m = RELATIONPATTERN.matcher(line);
			if (!m.matches()) {
				continue;
			}
			String arg1 = m.group(1);
			String arg2 = m.group(2);
			if (!id2class.containsKey(arg1)) {
				continue;
			}
			if (!id2class.containsKey(arg2))
				continue;
			writers.get(0).write(
					new Fact(null, id2class.get(arg1), "rdfs:subClassOf",
							id2class.get(arg2)));
		}
		Announce.done();
	}

	public WordnetExtractor(File wordnetFolder) {
		this.wordnetFolder = wordnetFolder;
	}
}
