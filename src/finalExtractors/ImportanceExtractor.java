package finalExtractors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;


public class ImportanceExtractor extends extractors.Extractor {

  /** Holds the file with NAGA's entity pair co-occurrence counts*/
  protected final File cooccurrenceFile;
  
  @Override
  public Set<Theme> input() {    
    return new FinalSet<>(SimpleTypeExtractor.SIMPLETYPES);
  }

  /** The importance scores for the type facts*/
  public static final Theme IMPORTANCE=new Theme("yagoImportance","Scores that say how important every entity is in its WordNet leaf class. Use with "+SimpleTypeExtractor.SIMPLETYPES+".",Theme.ThemeGroup.SIMPLETAX);
  
  @Override
  public Set<Theme> output() {
    return new FinalSet<>(IMPORTANCE);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Map<String,String> line2id=new HashMap<>();
    Announce.doing("Loading type facts");
    for(Fact f : input.get(SimpleTypeExtractor.SIMPLETYPES)) {
      if(!f.getRelation().equals(RDFS.type) || f.getId()==null) continue;
      String clss=FactComponent.wordnetWord(f.getArg(2));
      if(clss==null) continue;
      String entity=lcase(f.getArg(1));
      if(clss.compareTo(entity)<0) line2id.put(clss+"\t"+entity,f.getId());
      else line2id.put(entity+"\t"+clss,f.getId());
    }
    Announce.done();
    String dec=FactComponent.forQname("xsd:", "decimal");
    for(String line : new FileLines(cooccurrenceFile,"Parsing cooccurrence file")) {
      int lp=line.lastIndexOf('\t');
      String id=line2id.get(line.substring(0,lp));
      if(id==null || line.substring(lp+1).matches("\\d+")) continue;      
      output.get(IMPORTANCE).write(new Fact(id,"<hasImportanceForClass>",FactComponent.forStringWithDatatype(line.substring(lp+1), dec)));
    }
  }

  protected static String lcase(String s) {
    s=FactComponent.stripBrackets(s);    
    s=s.replace('_', ' ');
    s=s.toLowerCase();
    return(s);
  }
  public ImportanceExtractor(File cooccurrenceFile) {
    this.cooccurrenceFile=cooccurrenceFile;
  }
}
