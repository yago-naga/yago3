package fromThemes;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * YAGO2s - CoherentTypeExtractor
 * 
 * Extracts the coherent types from previous types
 * 
 * @author Farzaneh
 * 
 */
public class CoherentTypeExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		Set<Theme> result = new TreeSet<Theme>();
		result.add(WordnetExtractor.WORDNETCLASSES);
		result.add(HardExtractor.HARDWIREDFACTS);
		result.add(CategoryClassExtractor.CATEGORYCLASSES);
		result.addAll(InfoboxTypeExtractor.INFOBOXTYPES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		result.addAll(CategoryTypeExtractor.CATEGORYTYPES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		return result;
	}

	/** All types of YAGO */
	public static final Theme YAGOTYPES = new Theme("yagoTypes",
			"The coherent types extracted from different wikipedias");

	public static final Theme YAGOTYPESSOURCES = new Theme("yagoTypesSources",
			"Sources for the coherent types extracted from different wikipedias");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(YAGOTYPES, YAGOTYPESSOURCES);
	}

	/** Caches the YAGO branches */
	protected Map<String, String> yagoBranches;

	/** Holds the entire class taxonomy */
	protected FactCollection subclassFacts;

	/** Holds the entire type facts */
	protected FactCollection typeFacts;

	/** Maps a type fact id to the theme where it's from */
	protected Map<String, Theme> sources;

	/** Maps a Theme to its number of type facts */
	protected IntHashMap<Theme> numTypeFacts;

	@Override
	public void extract() throws Exception {

		yagoBranches = new HashMap<String, String>();
		subclassFacts = new FactCollection();
		typeFacts = new FactCollection();
		sources = new HashMap<>();
		for (Theme theme : input()) {
			for (Fact f : theme) {
				if (f.getRelation().equals(RDFS.type)) {
					f.makeId();
					// Add only the first source
					if (!sources.containsKey(f.getId()))
						sources.put(f.getId(), theme);
					typeFacts.justAdd(f);
				}
				if (f.getRelation().equals(RDFS.subclassOf))
					subclassFacts.justAdd(f);
			}
		}
		numTypeFacts = new IntHashMap<>();
		for (String currentEntity : typeFacts.getSubjects()) {
			flush(currentEntity,
					typeFacts.collectObjects(currentEntity, RDFS.type));
		}
		try (Writer w = FileUtils.getBufferedUTF8Writer(new File(YAGOTYPES
				.file().getParent(), "_typeStatistics.tsv"))) {
			for (Theme theme : numTypeFacts) {
				w.write(theme.name + "\t" + numTypeFacts.get(theme) + "\n");
			}
		}
		yagoBranches = null;
		typeFacts = null;
		subclassFacts = null;
		sources = null;
		Announce.done();
	}

	/** Returns the YAGO branch for a class */
	public String yagoBranchForClass(String arg) {
		if (yagoBranches.containsKey(arg))
			return (yagoBranches.get(arg));
		String yagoBranch = SimpleTypeExtractor.yagoBranch(arg, subclassFacts);
		if (yagoBranch != null) {
			yagoBranches.put(arg, yagoBranch);
			return (yagoBranch);
		}
		return null;
	}

	/** Returns the YAGO branch for a an entity */
	public String yagoBranchForEntity(String entity, Set<String> types) {
		IntHashMap<String> branches = new IntHashMap<>();

		for (String type : types) {
			String yagoBranch = yagoBranchForClass(type);
			if (yagoBranch != null) {
				Announce.debug(entity, type, yagoBranch);
				branches.increase(yagoBranch);
				// Give higher priority to the stuff extracted from infoboxes
				if (type.startsWith("<wordnet"))
					branches.increase(yagoBranch);
			}
		}
		String bestSoFar = null;
		for (String candidate : branches.keys()) {
			if (bestSoFar == null
					|| branches.get(candidate) > branches.get(bestSoFar)
					|| branches.get(candidate) == branches.get(bestSoFar)
					&& SimpleTypeExtractor.yagoBranches.indexOf(candidate) < SimpleTypeExtractor.yagoBranches
							.indexOf(bestSoFar))
				bestSoFar = candidate;
		}
		return (bestSoFar);
	}

	public void flush(String entity, Set<String> types) throws IOException {
		String yagoBranch = yagoBranchForEntity(entity, types);
		// Announce.debug("Branch of", entity, "is", yagoBranch);
		if (yagoBranch == null) {
			return;
		}
		for (String type : types) {
			String branch = yagoBranchForClass(type);
			if (branch == null || !branch.equals(yagoBranch)) {
				Announce.debug("Wrong branch:", type, branch,
						". Expected branch:", yagoBranch);
			} else {
				Fact f = new Fact(entity, RDFS.type, type);
				f.makeId();
				Theme source = sources.get(f.getId());
				if (source == null)
					continue;
				numTypeFacts.increase(source);
				write(YAGOTYPES, f, YAGOTYPESSOURCES,
						FactComponent.wikipediaURL(entity),
						FactComponent.forString(source.name));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		MultilingualExtractor.wikipediaLanguages = Arrays.asList("en");
		new CoherentTypeExtractor().extract(new File("c:/fabian/data/yago3"),
				"test");
	}
}
