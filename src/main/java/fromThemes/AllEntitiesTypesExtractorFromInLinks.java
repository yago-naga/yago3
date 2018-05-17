package fromThemes;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.Extractor;
import fromWikipedia.StructureExtractor;
import utils.EntityType;
import utils.FactCollection;
import utils.Theme;

public class AllEntitiesTypesExtractorFromInLinks extends Extractor {

  public static final Theme ALLENTITIES_INLINKS = new Theme("allEntities_from_inlinks",
      "List of all entities specifying if they are named entities or concepts or unknown, extracted from inlinks.");
  
  @Override
  public Set<Theme> input() {
    Set<Theme> input =  new HashSet<Theme>();
//    input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
//    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    //this extractor actually depends on WikidataInstances and CatMembers to get all entity names, but since I do this in other extractor AllEntitiesFromYago, to write less code I assume that is already called and load the entity names from that theme's subjects
    input.add(AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALLENTITIES_YAGO);
    input.add(StructureExtractor.STRUCTUREFACTS.inLanguage("en"));
    return input;
  }

  @Override
  public Set<Theme> output() {
    Set<Theme> results =  new HashSet<Theme>();
    results.add(ALLENTITIES_INLINKS);
    return results;
  }

  @Override
  public void extract() throws Exception {
    FactCollection reverseStructure = StructureExtractor.STRUCTUREFACTS.inLanguage("en").factCollection().getReverse();
    Map<String, String> anchors = StructureExtractor.STRUCTUREFACTS.inLanguage("en").factCollection().getMap("<hasAnchorText>");
    Set<String> entities = AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings.ALLENTITIES_YAGO.factCollection().getSubjects();
    
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
            ALLENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.NAMED_ENTITY.getYagoName()));
          }
          else if(upperFirstLetter < lowerFirstLetter) {
            ALLENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.CONCEPT.getYagoName()));
          }
          else {
            ALLENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.BOTH.getYagoName()));
          }
        }
        else {
          ALLENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
        }

      }
      else {
        ALLENTITIES_INLINKS.write(new Fact(entity, YAGO.isNamedEntity, EntityType.UNKNOWN.getYagoName()));
      }
    }
  }

}
