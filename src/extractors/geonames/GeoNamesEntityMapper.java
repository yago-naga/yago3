package extractors.geonames;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor;
import extractors.InfoboxExtractor;

/**
 * The GeoNamesEntityMapper maps geonames entities to Wikipedia entities.
 * 
 * Needs the GeoNames allCountries.txt as input.
 * 
 * @author Johannes Hoffart
 *
 */
public class GeoNamesEntityMapper extends Extractor {

  private File allCountries;
  
  private Map<String, List<Integer>> name2ids = new HashMap<>();
  private Map<Integer, Float> id2latitude = new HashMap<>();
  private Map<Integer, Float> id2longitude = new HashMap<>();
  
  /** geonames entity links */
  public static final Theme GEONAMESENTITYIDS = new Theme("yagoGeonamesEntityIds", "IDs from GeoNames entities");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(
        InfoboxExtractor.INFOBOXFACTS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(GEONAMESENTITYIDS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {    
    for (String line : new FileLines(allCountries, "UTF-8", "Reading GeoNames entities")) {
      String[] data = line.split("\t");

      Integer geonamesId = Integer.parseInt(data[0]);
      String name = data[1];

      List<Integer> ids = name2ids.get(name);
      if (ids == null) {
        ids = new LinkedList<Integer>();
        name2ids.put(name, ids);
      }
      ids.add(geonamesId);
      
      Float lati = Float.parseFloat(data[4]);
      Float longi = Float.parseFloat(data[5]);      
      
      id2latitude.put(geonamesId, lati);
      id2longitude.put(geonamesId, longi);
    }

    FactSource ibFacts = input.get(InfoboxExtractor.INFOBOXFACTS);       
   
    // Try to match all entities of type yagoGeoEntity
    Set<String> yagoGeoEntities = new HashSet<>();
    Map<String, Pair<Float, Float>> coordinates = new HashMap<>();
    
    for (Fact f : ibFacts) {
      if (f.getRelation().equals(RDFS.type) && f.getArg(2).equals(GeoNamesClassMapper.GEO_CLASS)) {
        yagoGeoEntities.add(f.getArg(1));
      } else if (f.getRelation().equals("<hasLatitude>")) {
        Pair<Float, Float> coos = coordinates.get(f.getArg(1));
        
        if (coos == null) {
          coos = new Pair<Float, Float>(null, null);
          coordinates.put(f.getArg(1), coos);          
        }
        
        coos.first = Float.parseFloat(FactComponent.stripQuotes(FactComponent.literalAndDataType(f.getArg(2))[0]));
      } else if (f.getRelation().equals("<hasLongitude>")) {
        Pair<Float, Float> coos = coordinates.get(f.getArg(1));
        
        if (coos == null) {
          coos = new Pair<Float, Float>(null, null);
          coordinates.put(f.getArg(1), coos);          
        }
        
        coos.second = Float.parseFloat(FactComponent.stripQuotes(FactComponent.literalAndDataType(f.getArg(2))[0]));
      }
    }
    
    for (String geoEntity : yagoGeoEntities) {
      Pair<Float, Float> coos = coordinates.get(geoEntity);
      
      Float lats = null;
      Float longs = null;
      
      if (coos != null) {
        lats = coos.first;
        longs = coos.second;
      }
      
      Integer geoId = matchToGeonames(geoEntity, lats, longs);

      if (geoId != -1) {
        output.get(GEONAMESENTITYIDS).write(new Fact(geoEntity, "<hasGeonamesEntityId>", FactComponent.forNumber(geoId)));
      }
    }

  }
  
  public Integer matchToGeonames(String name, Float lats, Float longs) {
    if (isInGeonames(name)) {
      List<Integer> possibleLocations = getGeonamesIds(name);

      Integer correctLocation = -1;

      if (possibleLocations.size() == 1) {
        correctLocation = possibleLocations.get(0);
      } else if (possibleLocations.size() > 1) {
        // try to disambiguate by choosing the closest location
        if (lats != null && longs != null) {
          correctLocation = getIdForLocation(name, lats, longs);

          if (correctLocation != -1) {
            Announce.debug("Found geonames target using disambiguation");
          }
        }
      }

      if (correctLocation != -1) {
        // update geonames db to store the mapping - 
        // this allows to retrieve YAGO entities by geonames id later
        // in the GeonamesImporter - this is also where all the facts
        // will be added to the entity
        return correctLocation;
      }
    }
    
    return -1;
  }
  
  private boolean isInGeonames(String locationName) {
    return name2ids.containsKey(FactComponent.stripBrackets(locationName));
  }

  private List<Integer> getGeonamesIds(String locationName) {
    return name2ids.get(FactComponent.stripBrackets(locationName));
  }

  private int getIdForLocation(String locationName, float latitude, float longitude) {
    List<Integer> possibleLocations = getGeonamesIds(locationName);

    // assume there is only one correct place for any given name/coord pair
    for (int possibleLocation : possibleLocations) {
      if (isNearby(possibleLocation, latitude, longitude)) {
        return possibleLocation;
      }
    }

    // nothing was found with fitting name/coordinates
    return -1;
  }
  
  private boolean isNearby(int possibleLocation, float latitude, float longitude) {
    float cLat = id2latitude.get(possibleLocation);
    float cLong = id2longitude.get(possibleLocation);

    if (distance(cLat, cLong, latitude, longitude) < 0.05) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Returns the angle distance between coordinate A and B
   * in Degrees. 0.01 degrees is roughly equivalent to 1.11 km (depending 
   * on the location on Earth. This should be enough to discriminate.
   * 
   * @param latA
   * @param longA
   * @param latB
   * @param longB
   * @return Distance between A and B in degrees
   */
  private double distance(float latA, float longA, float latB, float longB) {
    double lat1 = Math.toRadians(new Double(latA));
    double long1 = Math.toRadians(new Double(longA));
    double lat2 = Math.toRadians(new Double(latB));
    double long2 = Math.toRadians(new Double(longB));

    double angleDistance = Math.toDegrees(Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(long1 - long2)));

    return angleDistance;
  }

  public GeoNamesEntityMapper(File allCountries) {
    this.allCountries = allCountries;
  }
}
