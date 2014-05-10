package fromThemes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
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
		result.addAll(CategoryTypeExtractor.CATEGORYTYPES
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		result.addAll(InfoboxTypeExtractor.INFOBOXTYPES
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

	@Override
	public void extract() throws Exception {

		yagoBranches = new HashMap<String, String>();
		subclassFacts = new FactCollection();
		typeFacts = new FactCollection();
		for (Theme theme : input()) {
			for (Fact f : theme.factSource()) {
				if (f.getRelation().equals(RDFS.type))
					typeFacts.justAdd(f);
				if (f.getRelation().equals(RDFS.subclassOf))
					subclassFacts.justAdd(f);
			}
		}
		for (String currentEntity : typeFacts.getSubjects()) {
			flush(currentEntity,
					typeFacts.collectObjects(currentEntity, RDFS.type));
		}
		yagoBranches = null;
		typeFacts = null;
		subclassFacts = null;
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
				// Give higher priority to the stuff extracted from infoboxes
				branches.increase(yagoBranch);
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
				Announce.debug("Wrong branch:", type, branch);
			} else {
				// writers.get(COHERENTTYPES).write( new Fact(entity, RDFS.type,
				// type));

				write(YAGOTYPES, new Fact(entity, RDFS.type, type),
						YAGOTYPESSOURCES, FactComponent.wikipediaURL(entity),
						"WikipediaTypeExtractor from category");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CoherentTypeExtractor extractor = new CoherentTypeExtractor();
		extractor.extract(new File("D:/data3/yago2s/"), "");
	}
}
