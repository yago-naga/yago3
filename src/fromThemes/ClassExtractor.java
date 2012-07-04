package fromThemes;

import java.io.File;
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
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.WikipediaTypeExtractor;

/**
 * YAGO2s - ClassExtractor
 * 
 * Deduplicates all type subclass facts and puts them into the right themes.
 * 
 * This is different from the FactExtractor, because its output is useful for
 * many extractors that deliver input for the FactExtractor.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class ClassExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(WikipediaTypeExtractor.WIKIPEDIACLASSES,
        HardExtractor.HARDWIREDFACTS,         
        WordnetExtractor.WORDNETCLASSES);
  }

  /** The YAGO taxonomy */
  public static final Theme YAGOTAXONOMY = new Theme("yagoTaxonomy", "The entire YAGO taxonomy. These are all rdfs:subClassOf facts derived from Wikipedia and from WordNet.", ThemeGroup.TAXONOMY);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOTAXONOMY);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    String relation =RDFS.subclassOf;
      Announce.doing("Reading", relation);
      FactCollection facts = new FactCollection();
      for (Theme theme : input.keySet()) {
        Announce.doing("Reading", theme);
        for (Fact fact : input.get(theme)) {
          if (!relation.equals(fact.getRelation()))
            continue;
          facts.add(fact);
        }
        Announce.done();
      }
      Announce.done();
      Announce.doing("Writing", relation);
      FactWriter w = output.get(YAGOTAXONOMY);
      for (Fact fact : facts)
        w.write(fact);
      Announce.done();
    }
  
  public static void main(String[] args) throws Exception {
    new ClassExtractor().extract(new File("c:/fabian/data/yago2s"),"test");
  }
}
