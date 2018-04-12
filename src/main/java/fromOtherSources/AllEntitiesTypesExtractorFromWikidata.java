package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import fromThemes.AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.EntityType;
import utils.FactCollection;
import utils.Theme;

public class AllEntitiesTypesExtractorFromWikidata extends DataExtractor {

  public AllEntitiesTypesExtractorFromWikidata(File wikidata) {
    super(wikidata);
  }
  
  public AllEntitiesTypesExtractorFromWikidata() {
    this(Parameters.getFile(WIKIDATA));
  }

  public static final Theme ALL_ENTITIES_WIKIDATA = new Theme("allEntities_from_wikidata",
      "List of all entities specifying if they are named entities or concepts or unknown, extracted from wikidata.");
  
  private static final String WIKIDATA = "wikidata";
  
  private static final Map<String, String> mostEnglishyagoEntityWikidataId = new HashMap<String, String>();
  
  @Override
  public Set<Theme> input() {
    Set<Theme> input =  new HashSet<Theme>();
  input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
//  input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    //this extractor actually depends on WikidataInstances and CatMembers to get all entity names, but since I do this in other extractor AllEntitiesFromYago, to write less code I assume that is already called and load the entity names from that theme's subjects
    input.add(AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALL_ENTITIES_YAGO);
    return input;
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(AllEntitiesTypesExtractorFromWikidata.ALL_ENTITIES_WIKIDATA));
  }

  @Override
  public void extract() throws Exception {
    N4Reader nr = new N4Reader(inputData);
    loadMostEnglishEntities();
    
    // Based on rules stated here: https://www.wikidata.org/wiki/Help:Basic_membership_properties:
    // But since there are some exceptions, we will have BOTH and wrong results sometimes.
    // Example of wrong relation: https://www.wikidata.org/wiki/Q41576
    Set<String> instances = new HashSet<>();
    Set<String> classes = new HashSet<>();
    
    while(nr.hasNext()) {
      Fact f = nr.next(); 
      if (mostEnglishyagoEntityWikidataId.containsKey(f.getSubject()) || mostEnglishyagoEntityWikidataId.containsKey(f.getObject())) {
        if (f.getRelation().equals("<http://www.wikidata.org/prop/direct/P31>")) {//P31 -> instanceOf
          instances.add(f.getSubject());
          classes.add(f.getObject());
        }
        else if (f.getRelation().equals("<http://www.wikidata.org/prop/direct/P279>")) {//P279 -> subClassOf
          classes.add(f.getSubject());
          classes.add(f.getObject());
        }
      }
    }
    nr.close();
    System.out.println("intsances: " + instances.size());
    System.out.println("classes: " + classes.size());
    
    for (String entity : AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALL_ENTITIES_YAGO.factCollection().getSubjects()) {
      if (entity == null) {
        continue;
      }
      String wikidataId = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getObject(entity, RDFS.sameas);
      if (wikidataId != null ) {
        if (mostEnglishyagoEntityWikidataId.containsKey(wikidataId)) {
          if (mostEnglishyagoEntityWikidataId.get(wikidataId).equals(entity)) {
            if (instances.contains(wikidataId) && classes.contains(wikidataId)) {
              ALL_ENTITIES_WIKIDATA.write(new Fact(mostEnglishyagoEntityWikidataId.get(wikidataId), YAGO.isNamedEntity, EntityType.BOTH.getYagoName()));
            }
            else if (classes.contains(wikidataId)) {
              ALL_ENTITIES_WIKIDATA.write(new Fact(mostEnglishyagoEntityWikidataId.get(wikidataId), YAGO.isNamedEntity, EntityType.CONCEPT.getYagoName()));
            }
            else if (instances.contains(wikidataId)) {
              ALL_ENTITIES_WIKIDATA.write(new Fact(mostEnglishyagoEntityWikidataId.get(wikidataId), YAGO.isNamedEntity, EntityType.NAMED_ENTITY.getYagoName()));
            }
            else {
              ALL_ENTITIES_WIKIDATA.write(new Fact(mostEnglishyagoEntityWikidataId.get(wikidataId), YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
            }
          }
          else {
            System.out.println("Warn: Most English was not equal to entity: " + entity + " " + mostEnglishyagoEntityWikidataId.get(wikidataId) + " " + wikidataId);
          }
        }
        else {
          System.out.println("Warn: Entity not present in most English entities " + entity + " " + wikidataId);
        }
    
      }
      else {
        ALL_ENTITIES_WIKIDATA.write(new Fact(entity, YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
      }
        
    }
  }
  
  /**
   * Fill the map mostEnglishyagoEntityWikidataId which maps from WikidataId to the most English yago entity.
   * @throws IOException
   */
  private void loadMostEnglishEntities() throws IOException {
    FactCollection reverseWikidataInstances = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getReverse();
    
    for(String subject:reverseWikidataInstances.getSubjects()) {
      String mostEn = getMostEnglishEntityName(reverseWikidataInstances.getFactsWithSubjectAndRelation(subject, RDFS.sameas));
      if (mostEn == null) {
        System.out.println(subject);
        continue;
      }
      mostEnglishyagoEntityWikidataId.put(subject, mostEn); 
    }
  }

  /**
   * Return the most English entity name given all entity names available
   * @param entityFacts yago entity in different languages
   * @return most English entity name
   */
  private static String getMostEnglishEntityName(Set<Fact> entityFacts){
    // Map of entity names for each language 
    Map<String, String> languageEntityName = new HashMap<>();
    // each entityFact is like: <http://www.wikidata.org/entity/Q23>  owl:sameAs <George_Washington>
    for(Fact f:entityFacts){
      String language = FactComponent.getLanguageOfEntity(f.getObject());
      if (language != null)
        languageEntityName.put(language, f.getObject());
      else
        languageEntityName.put("en", f.getObject());
    }
    
    String mostEnglishLanguage = DictionaryExtractor.mostEnglishLanguage(languageEntityName.keySet());
    return languageEntityName.get(mostEnglishLanguage);
  }
  
  /** Cache for category members */
  protected static SoftReference<Map<String, EntityType>> cache = new SoftReference<>(null);

  public static synchronized Map<String, EntityType> getAllEntitiesToSplitType() {
    Announce.doing("Loading All Entities Types");
    Map<String, EntityType> map = cache.get();
    if (map == null) {
      cache = new SoftReference<>(map = new HashMap<>());
      for (Fact f : ALL_ENTITIES_WIKIDATA) {
        if (YAGO.isNamedEntity.equals(f.getRelation())) {
          map.put(f.getSubject(), EntityType.find(f.getObject()));
        }
      }
    }
    return map;
  }


}
