package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TermExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * Class ImportantTypeExtractor - YAGO2S
 * 
 * Extracts only types that are mentioned in the first
 * sentence of Wikipedia.
 * 
 * @author Fabian M. Suchanek
 */

public class ImportantTypeExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS,TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS);
  }

  /** Important types*/
  public static final Theme IMPORTANTTYPES = new Theme("yagoImportantTypes",
      "rdf:type facts that are supported by the first sentence of Wikipedia - or default", Theme.ThemeGroup.TAXONOMY);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(IMPORTANTTYPES);
  }

  /** Points to Wikipedia*/
  protected File wikipedia;

  @Override
  public File inputDataFile() {   
    return wikipedia;
  }

  public ImportantTypeExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    TitleExtractor titleExtractor = new TitleExtractor(input);
    TermExtractor.ForClass classExtractor=new TermExtractor.ForClass(WordnetExtractor.preferredMeanings(input));
    Map<String, Set<String>> types=TransitiveTypeExtractor.yagoTaxonomy(input);
    String titleEntity = null;
    Announce.doing("Finding important types");
    loop: while (FileLines.findIgnoreCase(in, "<title>") != -1) {
      titleEntity = titleExtractor.getTitleEntity(in);
      Announce.debug("Entity:",titleEntity);
      if (titleEntity == null) continue;
      if(FileLines.find(in, "'''","</page>")!=0) continue;
      String firstPar=FileLines.readTo(in, "==").toString();
      Announce.debug("FirstPar:",firstPar.substring(0,Math.min(firstPar.length(),30)));
      firstPar=firstPar.replace("[[", ",[[").replace("]]","]],").replace(" ", ", ").replace("|", ", ");
      Set<String> myTypes=types.get(titleEntity);
      Announce.debug("Types:",myTypes);
      if(myTypes==null) continue;
      for(String parType : classExtractor.extractList(firstPar)) {
        if(myTypes.contains(parType)) {
          output.get(IMPORTANTTYPES).write(new Fact(titleEntity,RDFS.type,parType));
         Announce.debug("Chose",parType);
          continue loop;
        }
      }
      // By default just write any Wikipedia type
      for(String type : myTypes) {
        if(!type.startsWith("<wikicategory")) continue;
        output.get(IMPORTANTTYPES).write(new Fact(titleEntity,RDFS.type,type));
        Announce.debug("Chose by default",type);
        continue loop;
      }
      // ... or just any type
      for(String type : myTypes) {
        output.get(IMPORTANTTYPES).write(new Fact(titleEntity,RDFS.type,type));
        Announce.debug("Chose by default",type);
        continue loop;
      }      
    }
    Announce.done();
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    //new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "test"); 
    //new WordnetExtractor(new File("c:/fabian/data/wordnet")).extract(new File("c:/fabian/data/yago2s"),ParallelCaller.header);
    new ImportantTypeExtractor(new File("c:/fabian/data/wikipedia/testSet/wikitest.xml")).extract(new File("c:/fabian/data/yago2s"),"test");
  }

}
