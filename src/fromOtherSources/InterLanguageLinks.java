package fromOtherSources;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fromWikipedia.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.N4Reader;
import basics.Theme;

public class InterLanguageLinks extends Extractor {

  private File inputFile; 
  public static final Theme INTERLANGUAGELINKS=new Theme("yagoInterLanguageLinks","The manually created inter language synonyms");
  
  public InterLanguageLinks(File inputFolder) {
    this.inputFile = inputFolder.isFile()?inputFolder:new File(inputFolder,"wikidata.rdf"); 
  }
  
  public Set<Theme> output() {
    return (new FinalSet<Theme>(INTERLANGUAGELINKS));
  }
  
  public void extract(File input, FactWriter writer) throws Exception {
    Announce.doing("", input.getName());
//    "C://Users//Administrator//Downloads//turtle-20130808-links.ttl"
    N4Reader nr = new N4Reader(FileUtils.getBufferedUTF8Reader(input));
    Map<String,String> correspondence =  new HashMap<String, String>();
    while(nr.hasNext()){
      Fact f=nr.next();
      if(f.getArg(2).contains("#Item")){
        String mostEnglishName=null;
        String mostEnglishLang=null; 
//        = correspondence.get("en");
        for(int i = 0; i<Extractor.languages.length; i++){
          mostEnglishName = correspondence.get(languages[i]);
          if(mostEnglishName!=null){
            //correspondence.remove(languages[i]);
            mostEnglishLang= languages[i];
            break;
          }
        }
        
        if(mostEnglishName !=null){
          mostEnglishName = Char.cutLast(mostEnglishName);
          for (Map.Entry<String, String> entry : correspondence.entrySet())
          {
            writer.write(new Fact(FactComponent.forYagoEntity(Char.decodePercentage(mostEnglishName)),  "rdfs:label"
                , FactComponent.forStringWithLanguage(Char.decodePercentage(Char.cutLast(entry.getValue())), entry.getKey())));
          }
        }
        correspondence.clear();
        
        f= nr.next();
      }else if (f.getRelation().contains("Language")){
        String[] parts = f.getArg(1).split("/");
        String name = parts[parts.length - 1 ];
        correspondence.put(FactComponent.stripQuotes(f.getArg(2)), name);
      }
    
    }
    nr.close();
  }
  
  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> factCollections) throws Exception {
    Announce.doing("Copying patterns");
    Announce.message("Input folder is", inputFile);
      extract(inputFile, writers.get(INTERLANGUAGELINKS));
    Announce.done();
  }


  public static void main(String[] args) {
    try {
      new InterLanguageLinks(new File("D:/wikidata.rdf"))
      .extract(new File("D:/data2/yago2s/"), "test");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public Set<Theme> input() {
   return Collections.emptySet();
  }


}