package fromThemes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.datatypes.FinalSet;
import utils.EntityType;
import utils.FactCollection;
import utils.Theme;

public class AllEntitiesTypesExtractorFromYagoWordnetPrefMeanings extends Extractor {

  public static final Theme ALL_ENTITIES_YAGO = new Theme("allEntities_from_yago",
      "List of all entities specifying if they are named entities or concepts or unknown, extracted from yago-preffered meanings.");
  
  /** Holds the words of wordnet -- only for English title extractors*/
  protected Set<String> wordnetWords;
  
  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<>();
    input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(WordnetExtractor.PREFMEANINGS);
    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(ALL_ENTITIES_YAGO);
  }

  @Override
  public void extract() throws Exception {
    this.wordnetWords = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings().keySet();
    
    FactCollection reverseWikidataInstances = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getReverse();
    Set<String> done = new HashSet<>();
    for (String wikidataId : reverseWikidataInstances.getSubjects()) {
      Set<Fact> facts = reverseWikidataInstances.getFactsWithSubjectAndRelation(wikidataId, RDFS.sameas);
      Map<String, String> language2entity = new HashMap<>();
      for(Fact f: facts) {
        String language = FactComponent.getLanguageOfEntity(f.getObject());
        if (language == null) {
          language = "en";
        }
        language2entity.put(language, f.getObject());
        done.add(f.getObject());
      }
      String mostEnglishLan = DictionaryExtractor.mostEnglishLanguage(language2entity.keySet());
      if (mostEnglishLan == null) {
        continue;
      }
      // Checking if it is named entity or not
      // If it does not have a English version we cannot say anything about it, and we mark it as unknown.
      String isNamedEntity = EntityType.UNKNOWN.getYagoName();
      if (FactComponent.isEnglish(mostEnglishLan)) {
        if (isConcept(language2entity.get(mostEnglishLan))) {
          isNamedEntity = EntityType.CONCEPT.getYagoName();
        }
        else {
          isNamedEntity = EntityType.NAMED_ENTITY.getYagoName();
        }
      }
      ALL_ENTITIES_YAGO.write(new Fact(language2entity.get(mostEnglishLan), YAGO.isNamedEntity, isNamedEntity));
    }
    int temp_wd_ents = done.size();
    
    // Add other entities that does not have a wikidata ID
    for (Theme theme:CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      for (String entity : theme.factCollection().getSubjects()) {
        if (!done.contains(entity)) {
          System.out.println("NOT IN WD: " + entity);
          done.add(entity);
          String isNamedEntity = EntityType.UNKNOWN.getYagoName();
          if (theme.isEnglishOrDefault()) {
            if (isConcept(entity)) {
              isNamedEntity = EntityType.CONCEPT.getYagoName();
            }
            else {
              isNamedEntity = EntityType.NAMED_ENTITY.getYagoName();
            }
          }
          ALL_ENTITIES_YAGO.write(new Fact(entity, YAGO.isNamedEntity, isNamedEntity));
        }
      }
    }
    System.out.println("#Wikidata entities in all langs: " + temp_wd_ents);
    System.out.println("#Extra entities in all langs added by CatMems: " + (done.size() - temp_wd_ents));
  }

  private boolean isConcept(String title) {
    title = FactComponent.stripBrackets(title).replace('_', ' ').toLowerCase();
    return wordnetWords.contains(title);
  }

}
