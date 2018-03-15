package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromWikipedia.StructureExtractor;
import javatools.administrative.Parameters;
import utils.EntityType;
import utils.FactCollection;
import utils.Theme;

public class AllEntitiesTypesExtractorFromInLinks extends Extractor {

  public static final Theme ALL_ENTITIES_INLINKS = new Theme("allEntities_from_inlinks",
      "List of all entities specifying if they are named entities or concepts or unknown, extracted from inlinks.");
  
  @Override
  public Set<Theme> input() {
    Set<Theme> input =  new HashSet<Theme>();
//    input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
//    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    //this extractor actually depends on WikidataInstances and CatMembers to get all entity names, but since I do this in other extractor AllEntitiesFromYago, to write less code I assume that is already called and load the entity names from that theme's subjects
    input.add(AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALL_ENTITIES_YAGO);
    input.add(StructureExtractor.STRUCTUREFACTS.inLanguage("en"));
    return input;
  }

  @Override
  public Set<Theme> output() {
    Set<Theme> results =  new HashSet<Theme>();
    results.add(ALL_ENTITIES_INLINKS);
    return results;
  }

  @Override
  public void extract() throws Exception {
    FactCollection reverseStructure = StructureExtractor.STRUCTUREFACTS.inLanguage("en").factCollection().getReverse();
    Map<String, String> anchors = StructureExtractor.STRUCTUREFACTS.inLanguage("en").factCollection().getMap("<hasAnchorText>");
    Set<String> entities = AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALL_ENTITIES_YAGO.factCollection().getSubjects();
    
    for(String entity : entities) {
      String lan = FactComponent.getLanguageOfEntity(entity);
      if (lan == null || lan.equals("en")) {
        Set<Fact> facts = reverseStructure.getFactsWithSubjectAndRelation(entity, "<hasInternalWikipediaLinkTo>");
        int upperFirstLetter = 0;
        int lowerFirstLetter = 0;
        for(Fact f:facts) {
          String anchor = FactComponent.stripQuotesAndLanguage(anchors.get(f.getId()));
          if (anchor != null) {
            if (Character.isUpperCase(anchor.charAt(0))) {
              upperFirstLetter++;
            }
            else {
              lowerFirstLetter++;
            }
          }
        }
        
        if (upperFirstLetter != 0 ||  lowerFirstLetter != 0) {
          if(upperFirstLetter > lowerFirstLetter) {
            ALL_ENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.NAMED_ENTITY.getYagoName()));
          }
          else if(upperFirstLetter < lowerFirstLetter) {
            ALL_ENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.CONCEPT.getYagoName()));
          }
          else {
            ALL_ENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.BOTH.getYagoName()));
          }
        }
        else {
          ALL_ENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
        }

      }
      else {
        ALL_ENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
      }
    }
  }
  
  
  public static void main(String[] args) throws Exception {
    new AllEntitiesTypesExtractorFromInLinks().extract(new File("/local_san2/tmp/yago_output_allEntities_en20170620_de20170620"), "List of all entities specifying if they are named entities or concepts or unknown, extracted from inlinks.");
  }
}
