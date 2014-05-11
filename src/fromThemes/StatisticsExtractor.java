package fromThemes;

import java.io.File;
import java.util.Set;

import deduplicators.ClassExtractor;
import deduplicators.FactExtractor;
import deduplicators.LabelExtractor;
import deduplicators.LiteralFactExtractor;
import deduplicators.MetaFactExtractor;
import deduplicators.SchemaExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.ByteString;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.parsers.NumberFormatter;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.WikiInfoExtractor;

/**
 * YAGO2s - StatisticsExtractor
 * 
 * Extracts statistics about YAGO themes etc.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class StatisticsExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(ClassExtractor.YAGOTAXONOMY,
				CoherentTypeExtractor.YAGOTYPES, FactExtractor.YAGOFACTS,
				LabelExtractor.YAGOLABELS, MetaFactExtractor.YAGOMETAFACTS,
				SchemaExtractor.YAGOSCHEMA,
				LiteralFactExtractor.YAGOLITERALFACTS,
				WordnetExtractor.WORDNETIDS, WikiInfoExtractor.WIKIINFO);
	}

	/** YAGO statistics theme */
	public static final Theme STATISTICS = new Theme("yagoStatistics",
			"Statistics about YAGO and YAGO themes", ThemeGroup.META);

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(STATISTICS);
	}

	@Override
	public void extract() throws Exception {
		// TransitiveTypeExtractor.freeMemory();
		// WordnetExtractor.freeMemory();
		Set<String> definedRelations = new IntHashMap<>();
		IntHashMap<String> relations = new IntHashMap<>();
		Set<ByteString> instances = new IntHashMap<>();
		Announce.doing("Making YAGO statistics");
		for (Theme t : input()) {
			Announce.doing("Analyzing", t);
			int counter = 0;
			for (Fact f : t) {
				counter++;
				ByteString arg1 = ByteString.of(f.getArg(1));
				if ((f.getRelation().equals(RDFS.domain) || f.getRelation()
						.equals(RDFS.range))) {
					definedRelations.add(f.getArg(1));
				}
				relations.increase(f.getRelation());
				if (f.getRelation().equals(RDFS.type)) {
					instances.add(arg1);
				}
			}
			STATISTICS.write(new Fact(FactComponent.forTheme(t),
					YAGO.hasNumber, FactComponent.forNumber(counter)));
			Announce.done();
		}
		Announce.doing("Writing results");
		for (String rel : relations.keys()) {
			STATISTICS.write(new Fact(rel.toString(), YAGO.hasNumber,
					FactComponent.forNumber(relations.get(rel))));
			if (!definedRelations.contains(rel))
				Announce.warning("Undefined relation:", rel);
		}
		for (String rel : definedRelations) {
			if (!relations.containsKey(rel))
				Announce.warning("Unused relation:", rel);
		}
		Announce.done();
		Announce.message(instances.size(), "things");
		STATISTICS.write(new Fact(YAGO.yago, FactComponent
				.forYagoEntity("hasNumberOfThings"), FactComponent
				.forNumber(instances.size())));
		STATISTICS.write(new Fact(YAGO.yago, FactComponent
				.forYagoEntity("wasCreatedOnDate"), FactComponent
				.forDate(NumberFormatter.ISOdate())));
		Announce.done();
	}

	public static void main(String[] args) throws Exception {
		new StatisticsExtractor().extract(new File("c:/fabian/data/yago2s"),
				"test");
	}
}
