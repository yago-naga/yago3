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
import extractors.DisambiguationPageExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.PersonNameExtractor;
import extractors.RuleExtractor;
import extractors.TemporalInfoboxExtractor;
import extractors.WordnetExtractor;
import extractors.geonames.GeoNamesDataImporter;

/**
 * YAGO2s - LabelExtractor
 * 
 * Deduplicates all label facts (except for the multilingual ones). This extractor is different from FactExtractor so that it can run in parallel. 
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LabelExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYFACTS, DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS,         
        HardExtractor.HARDWIREDFACTS, 
        InfoboxExtractor.INFOBOXFACTS,
        PersonNameExtractor.PERSONNAMES,
          WordnetExtractor.WORDNETWORDS,
        WordnetExtractor.WORDNETGLOSSES);
  }

  /** Relations that we care for*/
  public static Set<String> relations = new FinalSet<>(RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>",
      "<hasFamilyName>", "<hasGloss>");

  /** All facts of YAGO */
  public static final Theme YAGOLABELS = new Theme("yagoLabels", "All labels of YAGO");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOLABELS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Announce.doing("Extracting sources");
    FactWriter w = output.get(YAGOLABELS);
    for (String relation : relations) {
      Announce.doing("Extracting", relation);
      FactCollection facts = new FactCollection();
      for (Theme theme : input.keySet()) {
        Announce.doing("Extracting from", theme);
        for (Fact fact : input.get(theme)) {
          if (fact.getRelation().equals(relation)) facts.add(fact);
        }
        Announce.done();
      }
      Announce.done();
      Announce.doing("Writing",relation);
      for(Fact f : facts) w.write(f);
      Announce.done();
    }
    Announce.done();
  }
}
