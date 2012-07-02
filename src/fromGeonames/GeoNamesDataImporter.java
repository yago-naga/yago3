package fromGeonames;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fromWikipedia.Extractor;
import fromWikipedia.InfoboxExtractor;


import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;

/**
 * The GeoNamesEntityMapper maps geonames entities to Wikipedia entities.
 * 
 * Needs a directory with these GeoNames files as input:
 *  - allCountries.txt
 *  - countryInfo.txt
 *  - hierarchy.txt
 * 
 * @author Johannes Hoffart
 *
 */
public abstract class GeoNamesDataImporter extends Extractor {

  protected File geonamesFolder;
    
  /** geonames entity links */
  public static final Theme GEONAMESDATA = new Theme("yagoGeonamesData", 
      "Data from GeoNames, e.g. coordinates, alternative names, locatedIn hierarchy, neighbor of");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(
        GeoNamesClassMapper.GEONAMESCLASSSIDS, 
        GeoNamesEntityMapper.GEONAMESENTITYIDS,
        InfoboxExtractor.INFOBOXFACTS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(GEONAMESDATA);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactWriter out = output.get(GEONAMESDATA);
    
    FactCollection mappedEntityIds = new FactCollection(input.get(GeoNamesEntityMapper.GEONAMESENTITYIDS));
    Map<String, String> geoEntityId2yago = mappedEntityIds.getReverseMap("<hasGeonamesEntityId>");
    FactCollection mappedClassIds = new FactCollection(input.get(GeoNamesClassMapper.GEONAMESCLASSSIDS));
    Map<String, String> geoClassId2yago = mappedClassIds.getReverseMap("<hasGeonamesClassId>");
    FactSource ibFacts = input.get(InfoboxExtractor.INFOBOXFACTS);
    
    extractAllCountries(new File(geonamesFolder, "allCountries.txt"), out, geoEntityId2yago, geoClassId2yago);
    extractHierarchy(new File(geonamesFolder, "hierarchy.txt"), out, geoEntityId2yago);
    extractCountryInfo(new File(geonamesFolder, "countryInfo.txt"), out, ibFacts);
  }
  
  private void extractAllCountries(File geodata, FactWriter out, Map<String, String> geoEntityId2yago, Map<String, String> geoClassId2yago) throws NumberFormatException, IOException {        
    for (String line : new FileLines(geodata, "UTF-8", "Importing GeoNames entity data")) {
      String[] data = line.split("\t");

      String geonamesId = FactComponent.forString(data[0]);
      
      if (shouldImportForGeonamesId(geonamesId, geoEntityId2yago)) {  
        String name = data[1];
      
        Float lati = Float.parseFloat(data[4]);
        Float longi = Float.parseFloat(data[5]);
        
        String fc = null;
        
        if (data[6].length() > 0 && data[7].length() > 0) {
          fc = data[6] + "." + data[7];
        } else {
          continue;
        }
        
        String alternateNames = data[3];
        List<String> namesList = null;

        if (alternateNames.length() > 0) {
          namesList = Arrays.asList(data[3].split(","));
        }
        
        // will remain untouched if not mapped
        name = FactComponent.forYagoEntity(getYagoNameForGeonamesId(name, FactComponent.forNumber(geonamesId), geoEntityId2yago));
        
        out.write(new Fact(name, "<hasLatitude>", FactComponent.forNumber(lati)));
        out.write(new Fact(name, "<hasLongitude>", FactComponent.forNumber(longi)));
        out.write(new Fact(name, RDFS.subclassOf, geoClassId2yago.get(FactComponent.forString(fc))));
        out.write(new Fact(name, "<hasGeonamesEntityId>", geonamesId));
        
        if (namesList != null) {
          for (String alternateName : namesList) {
            out.write(new Fact(name, RDFS.label, FactComponent.forString(alternateName)));
          }
        }
      }
    }
  }

  private void extractHierarchy(File geodata, FactWriter out, Map<String, String> geoEntityId2yago) throws IOException {
    for (String line : new FileLines(geodata, "UTF-8", "Importing GeoNames locatedIn data")) {
      String[] data = line.split("\t");
      
      String parent = FactComponent.forNumber(data[0]);
      String child = FactComponent.forNumber(data[1]);
      
      if (shouldImportForGeonamesId(parent, geoEntityId2yago)
          && shouldImportForGeonamesId(child, geoEntityId2yago)) {
        String childEntity = geoEntityId2yago.get(child);
        String parentEntity = geoEntityId2yago.get(parent);
        
        if (childEntity != null && parentEntity != null) {
          out.write(new Fact(childEntity, "<isLocatedIn>", parentEntity));
        }
      }
    }
  }

  private void extractCountryInfo(File geodata, FactWriter out, FactSource ibFacts) throws IOException {
    Map<String, String> tld2yago = new HashMap<>();
    for (Fact f : ibFacts) {
      if (f.getRelation().equals("<hasTLD>")) {
        tld2yago.put(f.getArg(2), f.getArg(1));
      }
    }
    
    for (String line : new FileLines(geodata, "UTF-8", "Importing neighboring countries")) {
      if (line.startsWith("#")) {
        continue;
      }

      String[] data = line.split("\t");

      if (data.length < 18) {
        continue; // no neighbors
      }

      String tld = data[9];
      
      if (!tld2yago.containsKey(tld)) {
        Announce.debug("TLD '" + tld + "' not available in YAGO, skipping neighbor data");
        continue;
      }
      
      String neighborString = data[17];

      if (neighborString.length() > 0) {
        List<String> neighborTLDs = new ArrayList<String>(Arrays.asList(neighborString.split(",")));
        
        for (String nbTLD : neighborTLDs) {
          String country = tld2yago.get(tld);
          String neighbor = tld2yago.get(nbTLD);
          
          if (neighbor != null) {
            out.write(new Fact(country, "<hasNeighbor>", neighbor));
          } else {
            Announce.debug("TLD '" + tld + "' not available in YAGO, not adding neighbor for '" + country + "'");
          }
        }
      } 
    }
  }

  private String getYagoNameForGeonamesId(String name, String geonamesId, Map<String, String> geoEntityId2yago) {
    if (geoEntityId2yago.containsKey(geonamesId)) {
      return geoEntityId2yago.get(geonamesId);
    } else {
      return name;
    }
  }
  
  public abstract boolean shouldImportForGeonamesId(String geonamesId, Map<String, String> geoEntityId2yago);
}
