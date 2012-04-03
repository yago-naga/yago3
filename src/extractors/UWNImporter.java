package extractors;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;

/**
 * Imports the multi-lingual class labels from Gerard de Melo's Universal WordNet (WUN=
 * 
 * Needs the uwn-nouns.tsv as input, with the format
 * three_letter_language_code\tnew_label\trel:means\twordnet_synset_id\tconfidence
 * 
 * @author Johannes Hoffart
 *
 */
public class UWNImporter extends Extractor {

  protected File uwnNouns;
    
  /** multi-lingual class names*/
  public static final Theme UWNDATA = new Theme("yagoMultilingualClassLabels", 
      "Multi-lingual class labels from Universal WordNet");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(
        HardExtractor.HARDWIREDFACTS,
        WordnetExtractor.WORDNETIDS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(UWNDATA);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    // get wordnet synset id mapping
    Map<String, String> wnssm = new FactCollection(input.get(WordnetExtractor.WORDNETIDS)).getReverseMap("<hasSynsetId>"); 
    Map<String, String> tlc2language = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS)).getReverseMap("<hasThreeLetterLanguageCode>");

    FactWriter writer = output.get(UWNDATA);
    
    for (String line : new FileLines(uwnNouns, "Importing UWN mappings")) {
      String data[] = line.split("\t");
            
      String lang = tlc2language.get(FactComponent.forString(data[0]));
      String name = FactComponent.forStringWithLanguage(data[1],data[0]);
      String wordnetSynset = wnssm.get(FactComponent.forString(data[3]));
      
      if (wordnetSynset == null) {
        Announce.debug("No WordNet Synset for id " + data[3]);
        continue;
      }
      
      if (lang == null) {
        Announce.debug("No Wikipedia language for " + data[3]);
        continue;
      }
      
      writer.write(new Fact(wordnetSynset, RDFS.label, name));
    }
  }
  
  public UWNImporter(File uwnNouns) {
    this.uwnNouns = uwnNouns;
  }
}
