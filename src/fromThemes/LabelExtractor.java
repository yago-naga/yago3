package fromThemes;

import java.util.Set;

import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryMapper;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.InfoboxExtractor;
import fromWikipedia.InfoboxMapper;
import fromWikipedia.PersonNameExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.WikipediaLabelExtractor;



import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;

/**
 * YAGO2s - LabelExtractor
 * 
 * Deduplicates all label facts (except for the multilingual ones). This extractor is different from FactExtractor so that it can run in parallel. 
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LabelExtractor extends SimpleDeduplicator {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryMapper.CATEGORYFACTS_TOREDIRECT_MAP.get("en"), DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS,         
        HardExtractor.HARDWIREDFACTS, WikipediaLabelExtractor.WIKIPEDIALABELS, 
        InfoboxMapper.INFOBOXFACTS_TOREDIRECT_MAP.get("en"),
        PersonNameExtractor.PERSONNAMES,
        WordnetExtractor.WORDNETWORDS, SchemaExtractor.YAGOSCHEMA,
        WordnetExtractor.WORDNETGLOSSES, FlightIATAcodeExtractor.AIRPORT_CODE, RedirectExtractor.REDIRECTLABELS_MAP.get("en"),
        GeoNamesDataImporter.GEONAMESMAPPEDDATA);
  }

  /** Relations that we care for*/
  public static Set<String> relations = new FinalSet<>(RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>",
      "<hasFamilyName>", "<hasGloss>", "<redirectedFrom>");

  /** All facts of YAGO */
  public static final Theme YAGOLABELS = new Theme("yagoLabels", "All facts of YAGO that contain labels (rdfs:label, skos:prefLabel, isPreferredMeaningOf, hasGivenName, hasFamilyName, hasGloss, redirectedFrom)", ThemeGroup.CORE);

  @Override
  public Theme myOutput() {   
    return YAGOLABELS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {    
    return relations.contains(fact.getRelation());
  }
   
 }
