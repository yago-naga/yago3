package finalExtractors;

import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor;
import extractors.HardExtractor;

/**
 * YAGO2s - SchemaExtractor
 * 
 * Deduplicates all schema facts (except for the multilingual ones). This extractor is different from FactExtractor so that it can run in parallel. 
 * 
 * @author Fabian M. Suchanek
 * 
 */

public class SchemaExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
  }

  /** All facts of YAGO */
  public static final Theme YAGOSCHEMA = new Theme("yagoSchema", "The schema of YAGO relations");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOSCHEMA);
  }

  /** Relations that we care for*/
  public static Set<String> relations = new FinalSet<>(RDFS.domain, RDFS.range, RDFS.subpropertyOf);

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactWriter w=output.get(YAGOSCHEMA);
    Announce.doing("Extracting schema");
     for(Theme theme : input.keySet()) {
       for(Fact fact : input.get(theme)) {
         if(relations.contains(fact.getRelation())) w.write(fact);
       }
     }
     Announce.done();
  }

}
