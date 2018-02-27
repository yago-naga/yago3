/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2017 Ghazaleh Haratinezhad Torbati.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import extractors.MultilingualExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * Extract description from wikidata.
 * 
*/
public class WikidataEntityDescriptionExtractor extends DataExtractor {
 
  public static final Theme WIKIDATAENTITYDESCRIPTIONS = new Theme("wikidataEntityDescriptions", 
      "Description extracted from wikidata for entities.");
  
  private static final String WIKIDATA = "wikidata";
 
  private static final Map<String, String> yagoEntityMostEnglish = new HashMap<String, String>();
  
  public WikidataEntityDescriptionExtractor(File wikidata) {
    super(wikidata);
  }
  
  public WikidataEntityDescriptionExtractor() {
    this(Parameters.getFile(WIKIDATA));
  }

  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(WikidataLabelExtractor.WIKIDATAINSTANCES));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(WIKIDATAENTITYDESCRIPTIONS));
  }

  @Override
  public void extract() throws Exception {
    
    // Loading the map from wikidataIds to the most English yago entity.
    loadMostEnglishEntities();
    
    N4Reader nr = new N4Reader(inputData);
    
    while(nr.hasNext()) {
      Fact f = nr.next();
      if (f.getRelation().equals("<http://schema.org/description>")) {
        // Select the most English entity.
        String yagoEntity = yagoEntityMostEnglish.get(f.getSubject());
        if(yagoEntity != null) {
          String description = f.getObject();
          // Write the description into the theme if its language is in our available languages.
          if(MultilingualExtractor.wikipediaLanguages.contains(FactComponent.getLanguageOfString(description)))
              WIKIDATAENTITYDESCRIPTIONS.write(new Fact(yagoEntity, YAGO.hasShortDescription, description));
        }
      }
      
    }
    
    nr.close();
  }
  
  /**
   * Fill the map yagoEntityMostEnglish which maps from WikidataId to the most English yago entity.
   * @throws IOException
   */
  private void loadMostEnglishEntities() throws IOException {
    FactCollection reverseWikidataInstances = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getReverse();
    
    for(String subject:reverseWikidataInstances.getSubjects()) {
      yagoEntityMostEnglish.put(subject, getMostEnglishEntityName(reverseWikidataInstances.getFactsWithSubjectAndRelation(subject, RDFS.sameas))); 
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
  

}
