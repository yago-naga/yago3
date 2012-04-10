package finalExtractors;

import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;

/**
 * YAGO2s - TypeExtractor
 * 
 * Deduplicates all type and subclass facts and puts them into the right themes.
 * 
 * This is different from the FactExtractor, because its output is useful for
 * many extractors that deliver input for the FactExtractor.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TypeExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYTYPES, HardExtractor.HARDWIREDFACTS,
    InfoboxExtractor.INFOBOXTYPES);
  }

  /** Final types */
  public static final Theme YAGOTYPES = new Theme("yagoTypes", "Types of YAGO");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOTYPES);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    String relation = RDFS.type;
    Announce.doing("Reading", relation);
    FactCollection facts = new FactCollection();
    for (Theme theme : input.keySet()) {
      Announce.doing("Reading", theme);
      for (Fact fact : input.get(theme)) {
        if (!relation.equals(fact.getRelation())) continue;
        facts.add(fact);
      }
      Announce.done();
    }
    Announce.done();
    Announce.doing("Writing", relation);
    FactWriter w = output.get(YAGOTYPES);
    for (Fact fact : facts)
      w.write(fact);
    Announce.done();
  }
}
