package finalExtractors;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalMap;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.DisambiguationPageExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.PersonNameExtractor;
import extractors.RuleExtractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYFACTS,
				CategoryExtractor.CATEGORYTYPES,
				CategoryExtractor.CATEGORYCLASSES, 
				HardExtractor.HARDWIREDFACTS, 
				RuleExtractor.RULERESULTS,
				InfoboxExtractor.INFOBOXFACTS, 
				InfoboxExtractor.INFOBOXTYPES, 				 
				//DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS, 
				HardExtractor.HARDWIREDFACTS,
				RuleExtractor.RULERESULTS, 
				PersonNameExtractor.PERSONNAMES, 
				WordnetExtractor.WORDNETCLASSES,
				WordnetExtractor.WORDNETWORDS, 
				WordnetExtractor.WORDNETGLOSSES,
				WordnetExtractor.WORDNETIDS,
				HardExtractor.HARDWIREDFACTS	);
	}

	/** All facts of YAGO */
	public static final Theme YAGOFACTS = new Theme("yagoFacts", "All instance facts of YAGO");
	/** All facts of YAGO */
	public static final Theme YAGOSCHEMA = new Theme("yagoSchema", "The schema of YAGO relations");
	/** All facts of YAGO */
	public static final Theme YAGOLABELS = new Theme("yagoLabels", "All labels of YAGO instances");
	/** Final types */
	public static final Theme YAGOTYPES = new Theme("yagoTypes", "Types of YAGO");
	/** The YGAO taxonomy */
	public static final Theme YAGOTAXONOMY = new Theme("yagoTaxonomy", "The entire YAGO taxonomy");

	/** which relations go to which theme */
	public static final Map<Theme, Set<String>> theme2relations = new FinalMap<>(YAGOSCHEMA, new FinalSet<>(
			RDFS.domain, RDFS.range, RDFS.subpropertyOf), YAGOLABELS, new FinalSet<>(RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>","<hasGloss>"), YAGOTYPES, new FinalSet<>(RDFS.type), YAGOTAXONOMY, new FinalSet<>(RDFS.subclassOf));

	@Override
	public Set<Theme> output() {
		Set<Theme> themes=new HashSet<>(theme2relations.keySet());
		themes.add(YAGOFACTS);
		return themes;
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Map<String, FactWriter> relation2Writer = new TreeMap<>();
		for (Theme t : theme2relations.keySet()) {
			for (String r : theme2relations.get(t)) {
				relation2Writer.put(r, output.get(t));
			}
		}
		Set<String> relationsToDo = new TreeSet<>();
		relationsToDo.add(RDFS.label);
		Set<String> relationsDone = new TreeSet<>();
		while (!relationsToDo.isEmpty()) {
			String relation = D.pick(relationsToDo);
			relationsToDo.remove(relation);
			relationsDone.add(relation);
			Announce.doing("Reading", relation);
			FactCollection facts = new FactCollection();
			for (Theme theme : input.keySet()) {
				Announce.doing("Reading", theme);
				for (Fact fact : input.get(theme)) {
					if (!relationsDone.contains(fact.getRelation())
							&& !fact.getRelation().startsWith("<_"))
						relationsToDo.add(fact.getRelation());
					if (!relation.equals(fact.getRelation()))
						continue;
					facts.add(fact);
				}
				Announce.done();
			}
			Announce.done();
			Announce.doing("Writing", relation);
			FactWriter w=relation2Writer.get(relation);
			if(w==null) w=output.get(YAGOFACTS);
			for (Fact fact : facts)
				w.write(fact);
			Announce.done();
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new FactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
	}
}
