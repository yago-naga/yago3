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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * Extract Geo coordinates from wikidata.
 * 
*/
public class WikidataEntityGeoCoordinateExtractor extends DataExtractor {
 
  public static final Theme WIKIDATAENTITYGEOCOORDINATES = new Theme("wikidataEntityGeoCoordinates", 
      "Geo Coordinates extracted from wikidata for entities.");
  
  private static final String WIKIDATA = "wikidata";
 
  private static final Map<String, String> yagoEntityMostEnglish = new HashMap<String, String>();
  
  private static final String wikidataEntityIdPrefix = "<http://www.wikidata.org/entity/";
  private static final Pattern WikidataStatementPattern = Pattern.compile("<http://www.wikidata.org/entity/statement/(Q\\d+)-.+>");
  
  public WikidataEntityGeoCoordinateExtractor(File wikidata) {
    super(wikidata);
  }
  
  public WikidataEntityGeoCoordinateExtractor() {
    this(Parameters.getFile(WIKIDATA));
  }

  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(WikidataLabelExtractor.WIKIDATAINSTANCES));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(WIKIDATAENTITYGEOCOORDINATES));
  }
  
  private class Location {
    Double latitude;
    Double longitude;
    
    public Location(Double latitude, Double longitude) {
      super();
      this.latitude = latitude;
      this.longitude = longitude;
    }

    public boolean equals(Location obj) {
      return ((latitude == obj.latitude) && (longitude == obj.longitude));
    }

    public int hashCode() {
      return latitude.hashCode() * longitude.hashCode();
      
    }
  }
  
  static final Pattern POINT = Pattern.compile("Point\\(([-\\.\\d]+) ([-\\.\\d]+)\\)");
  static final int LONGITUDE = 1, LATITUDE = 2;
  
  @Override
  public void extract() throws Exception {
    
    // Loading the map from wikidataIds to the most English yago entity.
    loadMostEnglishEntities();
    
    Map<Location, String> locations = new HashMap<>();
    Map<String, Set<String>> entityLocations = new HashMap<>();
    N4Reader nr = new N4Reader(inputData);
    int location_cnt = 0;
    while(nr.hasNext()) {
      Fact f = nr.next();
//    Fact:<http://www.wikidata.org/entity/Q22> <http://www.wikidata.org/prop/direct/P625> "Point(-5 57)"^^<http://www.opengis.net/ont/geosparql#wktLiteral>
      if (f.getRelation().endsWith("/P625>")) {
//         Select the most English entity.
        String wikidataId = f.getSubject();
        Matcher statementMatcher = WikidataStatementPattern.matcher(wikidataId);
        if (statementMatcher.find()) {
          wikidataId = wikidataEntityIdPrefix + statementMatcher.group(1) + ">";
        }
        String yagoEntity = yagoEntityMostEnglish.get(wikidataId);
        if(yagoEntity != null) {
          Matcher matcher = POINT.matcher(f.getObject());
          if (matcher.find()) {
            Location location = new Location(new Double(matcher.group(LATITUDE)), new Double(matcher.group(LONGITUDE)));
            String location_id;
            if (locations.containsKey(location)) {
              location_id = locations.get(location);
            }
            else {
              location_id = "LOCATION_" + location_cnt;
              locations.put(location, location_id);
              WIKIDATAENTITYGEOCOORDINATES.write(new Fact(location_id, YAGO.hasLatitude, location.latitude.toString()));
              WIKIDATAENTITYGEOCOORDINATES.write(new Fact(location_id, YAGO.hasLongitude, location.longitude.toString()));
              location_cnt++;
            }
            if (!entityLocations.containsKey(yagoEntity) || (entityLocations.get(yagoEntity).contains(location_id))) {
              WIKIDATAENTITYGEOCOORDINATES.write(new Fact(yagoEntity, YAGO.hasGeoLocation, location_id));
              entityLocations.computeIfAbsent(yagoEntity, k -> new HashSet<>()).add(location_id);
            }
          }
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
  
  public static void main(String[] args) throws Exception {
    WikidataEntityGeoCoordinateExtractor ex = new WikidataEntityGeoCoordinateExtractor(new File("/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/wikidatawiki/20170626/wikidata-20170626-all-BETA.ttl"));
    ex.extract(new File("/local_san2/tmp/yago_aida_en20170620_de20170620_zh20170620_es20170620_ar20170620_fr20170620/"), "Test");
  }

}


