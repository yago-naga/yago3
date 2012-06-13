package finalExtractors;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.parsers.NumberFormatter;
import javatools.parsers.NumberParser;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;
import extractors.Extractor;
import extractors.WordnetExtractor;

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
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY, TypeExtractor.YAGOTYPES, FactExtractor.YAGOFACTS, LabelExtractor.YAGOLABELS,
        MetaFactExtractor.YAGOMETAFACTS, SchemaExtractor.YAGOSCHEMA, LiteralFactExtractor.YAGOLITERALFACTS, SourceExtractor.YAGOSOURCES,
        WordnetExtractor.WORDNETIDS);
  }

  /** YAGO statistics theme */
  public static final Theme STATISTICS = new Theme("yagoStatistics", "Statistics about YAGO and YAGO themes", ThemeGroup.META);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(STATISTICS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    TransitiveTypeExtractor.freeMemory();
    WordnetExtractor.freeMemory();
    Map<String, Integer> relations = new HashMap<>();
    Set<String> instances = new HashSet<>(15_000_000);
    FactWriter out = output.get(STATISTICS);
    Announce.doing("Making YAGO statistics");
    for (Theme t : input.keySet()) {
      Announce.doing("Analyzing", t);
      int counter = 0;
      for (Fact f : input.get(t)) {
        counter++;
        if ((f.getRelation().equals(RDFS.domain) || f.getRelation().equals(RDFS.range)) && !relations.containsKey(f.getArg(1))) {
          relations.put(f.getArg(1), 0);
        }
        D.addKeyValue(relations, f.getRelation(), 1);
        if (f.getRelation().equals(RDFS.type)) {
          instances.add(f.getArg(1));
        }
      }
      out.write(new Fact(FactComponent.forTheme(t), YAGO.hasNumber, FactComponent.forNumber(counter)));
      Announce.done();
    }
    Announce.doing("Writing results");
    for (String rel : relations.keySet()) {
      if (relations.get(rel) == 0) Announce.warning("Relation without facts:", rel);
      else out.write(new Fact(rel, YAGO.hasNumber, FactComponent.forNumber(relations.get(rel))));
    }
    Announce.done();
    Announce.message(instances.size(), "things");
    out.write(new Fact(YAGO.yago, FactComponent.forYagoEntity("hasNumberOfThings"), FactComponent.forNumber(instances.size())));
    out.write(new Fact(YAGO.yago, FactComponent.forYagoEntity("wasCreatedOnDate"), FactComponent.forDate(NumberFormatter.ISOtime())));
    Announce.done();
  }

  public static void main(String[] args) throws Exception {
    new StatisticsExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
