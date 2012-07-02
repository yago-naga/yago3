package fromThemes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fromWikipedia.Extractor;


import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * YAGO2s - SimpleDeduplicator
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public abstract class SimpleDeduplicator extends Extractor {

  /** Theme that I want to output*/
  public abstract Theme myOutput();

  @Override
  public final Set<Theme> output() {
    return new FinalSet<>(myOutput());
  }

  /** TRUE if I want to write this relation*/
  public abstract boolean isMyRelation(Fact fact);

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Announce.doing("Deduplicating", this.getClass().getSimpleName());

    // We don't need any more taxonomy beyond this point
    // BUT: due to parallelization, some other extractors might be using it!
    //TransitiveTypeExtractor.freeMemory();
    //WordnetExtractor.freeMemory();

    FactWriter w = output.get(myOutput());
    Announce.doing("Loading");
    FactCollection batch = new FactCollection();
    for (Theme theme : input.keySet()) {
      Announce.doing("Loading from", theme);
      for (Fact fact : input.get(theme)) {
        if (isMyRelation(fact)) batch.add(fact);
      }
      Announce.done();
    }
    Announce.done();
    
    Announce.doing("Writing");
    for (Fact f : batch)
      w.write(f);
    Announce.done();
    w.close();
    
    Announce.done();
  }

}
