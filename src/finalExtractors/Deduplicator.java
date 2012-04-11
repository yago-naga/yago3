package finalExtractors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractors.Extractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public abstract class Deduplicator extends Extractor {

  /** Size of the memory*/
  public static final long HEAPSIZE= Runtime.getRuntime().maxMemory();

  /** Size of my memory*/
  public static final long MYHEAPSIZE= HEAPSIZE/5;

  /** Number of facts that can be cached*/
  public static final long NUMFACTS = MYHEAPSIZE/100/10;

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
    Announce.doing("Deduplicating",this.getClass().getSimpleName());
    
    // We don't need any more taxonomy beyond this point
    // BUT: due to parallelization, some other extractors might be using it!
    //TransitiveTypeExtractor.freeMemory();
    //WordnetExtractor.freeMemory();
   
    // Collect themes where we find the relations
    Map<String, Set<Theme>> relation2themes = new HashMap<>();
    // Collect the number of facts per relation
    Map<String,Integer> relation2number=new HashMap<>();
   
    // Collect all relations
    Announce.doing("Collecting relations");
    for(Theme theme : input.keySet()) {
      Announce.doing("Reading",theme);
      for(Fact fact : input.get(theme)) {
        if(!isMyRelation(fact)) continue;
        D.addKeyValue(relation2themes, fact.getRelation(), theme, HashSet.class);
        D.addKeyValue(relation2number, fact.getRelation(), 1);
      }
      Announce.done();
    }
    for(String relation : relation2number.keySet()) {
      Announce.message(relation,relation2number.get(relation));
    }
    Announce.done();
    
    // Write all relations
    Announce.doing("Writing relations");
    FactWriter w=output.get(myOutput());
    while(!relation2themes.isEmpty()) {
      Set<String> relations=new HashSet<>();
      Set<Theme> themes=new HashSet<>();
      int size=0;      
      for(String relation : relation2themes.keySet()) {
        if(size+relation2number.get(relation)<NUMFACTS || size==0) {
          size+=relation2number.get(relation);
          relations.add(relation);
          themes.addAll(relation2themes.get(relation));
        }
      }
      for(String relation : relations) {
        relation2themes.remove(relation);
        relation2number.remove(relation);
      }
      Announce.doing("Loading batch",relations);
      FactCollection batch=new FactCollection();
      for(Theme theme : themes) {
        Announce.doing("Loading from",theme);
        for(Fact fact : input.get(theme)) {
          if(relations.contains(fact.getRelation())) batch.add(fact);
        }
        Announce.done();
      }
      Announce.done();
      Announce.doing("Writing batch");
      for(Fact f : batch) w.write(f);
      Announce.done();
    }
    Announce.done();
    w.close();
    Announce.done();
  }
  
}
