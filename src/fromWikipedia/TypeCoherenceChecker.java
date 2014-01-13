package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.SimpleTypeExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;

  
public class TypeCoherenceChecker extends Extractor {
  

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>();
    result.add(WordnetExtractor.WORDNETCLASSES);
    result.add(HardExtractor.HARDWIREDFACTS);
    result.add(EntityTranslator.TRANSLATEDFACTS);
    for (String s : Extractor.languages) {
//      result.add(WikipediaTypeExtractor.YAGOTYPES_MAP.get(s));
      result.add(WikipediaTypeExtractor.WIKIPEDIACLASSES_MAP.get(s));
    }
    return result;
  }

 

  /** All types of YAGO */
  public static final Theme COHERENTTYPES = new Theme("cohrenetTypes",
      "The coherent types extracted from different wikipedias");
  public static final Theme COHERENTTYPESSOURCES = new Theme("cohrenetTypesSources",
      "Sources for the coherent types extracted from different wikipedias");

  /** Caches the YAGO branches*/
  protected Map<String, String> yagoBranches;
  
  /** Holds all the classes from Wordnet*/
protected FactCollection wordnetClasses;

/** Holds the facts about categories that we accumulate*/
protected ExtendedFactCollection categoryClassFacts;
/** Holds the facts about categories that we accumulate*/
//protected FactCollection categoryClassFacts;
  
  protected  ExtendedFactCollection loadFacts(FactSource factSource, ExtendedFactCollection result) {
    for(Fact f: factSource){
      result.add(f);
    }
    return(result);
  }
  
  
  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    
    categoryClassFacts = new ExtendedFactCollection();
    yagoBranches = new HashMap<String, String>();
    wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES),true);
    wordnetClasses.load(input.get(HardExtractor.HARDWIREDFACTS));
    ExtendedFactCollection batch = new ExtendedFactCollection();
    
    for (String s : Extractor.languages) {
//      loadFacts(input.get(WikipediaTypeExtractor.YAGOTYPES_MAP.get(s)), batch);
      loadFacts(input.get(EntityTranslator.TRANSLATEDFACTS), batch);
      loadFacts(input.get(WikipediaTypeExtractor.WIKIPEDIACLASSES_MAP.get(s)), categoryClassFacts);
    }
    
    FactWriter w = output.get(COHERENTTYPES);
    Set<String> processed = new TreeSet<String>();
    for (Fact f : batch){
      
      String currentEntity= f.getArg(1);
      if(processed.contains(currentEntity)) continue;
      Set<String> typesOfCurrentEntity= batch.getArg2s(currentEntity, "rdf:type");
      System.out.println(currentEntity + " OOOOOOOOOOOOOOOOOOOOOO "+ typesOfCurrentEntity );
      flush(currentEntity,typesOfCurrentEntity,  output);
      processed.add(currentEntity);
    }
    Announce.doing("Writing hard wired types");
    for (Fact f : input.get(HardExtractor.HARDWIREDFACTS)) {
      if (f.getRelation().equals(RDFS.type)) 
//        output.get(COHERENTTYPES).write(f);
      write(output,COHERENTTYPES, f, COHERENTTYPESSOURCES, FactComponent.wikipediaURL(f.getArg(1)),
          "WikipediaTypeExtractor from category");
    }
    Announce.done();

    w.close();
    
    Announce.done();
  }
  
  /** Returns the YAGO branch for a category class */
  public String yagoBranchForClass(String arg) {
    if (yagoBranches.containsKey(arg)) return (yagoBranches.get(arg));
    String yagoBranch = SimpleTypeExtractor.yagoBranch(arg, wordnetClasses);
    if (yagoBranch != null) {
      yagoBranches.put(arg, yagoBranch);
      return (yagoBranch);
    }
    String sup = categoryClassFacts.getArg2(arg, RDFS.subclassOf);
    if (sup != null) {
      yagoBranch = SimpleTypeExtractor.yagoBranch(sup, wordnetClasses);
      if (yagoBranch != null) {
        yagoBranches.put(arg, yagoBranch);
        return (yagoBranch);
      }
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
        if(type.startsWith("<wordnet")) branches.increase(yagoBranch);
      }
    }
    String bestSoFar = null;
    for (String candidate : branches.keys()) {
      if (bestSoFar == null || branches.get(candidate) > branches.get(bestSoFar) || branches.get(candidate) == branches.get(bestSoFar)
          && SimpleTypeExtractor.yagoBranches.indexOf(candidate) < SimpleTypeExtractor.yagoBranches.indexOf(bestSoFar)) bestSoFar = candidate;
    }
    return (bestSoFar);
  }

public void flush(String entity, Set<String> types, Map<Theme, FactWriter> writers) throws IOException {
  String yagoBranch = yagoBranchForEntity(entity, types);
//  Announce.debug("Branch of", entity, "is", yagoBranch);
  if (yagoBranch == null) {
    types.clear();
    return;
  }
  for (String type : types) {
    String branch = yagoBranchForClass(type);
    if (branch == null || !branch.equals(yagoBranch)) {
      Announce.debug("Wrong branch:", type, branch);
    } else {
//      writers.get(COHERENTTYPES).write( new Fact(entity, RDFS.type, type));
     
    
      write(writers, COHERENTTYPES, new Fact(entity, RDFS.type, type), COHERENTTYPESSOURCES, FactComponent.wikipediaURL(entity),
          "WikipediaTypeExtractor from category");
    }
  }
  types.clear();
}


  
  public static void main(String[] args) throws Exception {
    TypeCoherenceChecker extractor = new TypeCoherenceChecker();
    extractor.extract(new File("D:/data2/yago2s/"),"");
  }


  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(COHERENTTYPES, COHERENTTYPESSOURCES);
 
  }

}

  
  



