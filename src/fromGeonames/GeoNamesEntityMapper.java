package fromGeonames;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromThemes.TypeChecker;
import fromWikipedia.Extractor;

/**
 * The GeoNamesEntityMapper maps geonames entities to Wikipedia entities.
 * 
 * Needs the GeoNames alternateNames.txt as input.
 * 
 * @author Johannes Hoffart
 *
 */
public class GeoNamesEntityMapper extends Extractor {

  private File alternateNames;
  
  public static final String ENWIKI_PREFIX = "http://en.wikipedia.org/wiki/"; 

  /** geonames entity links (need type-checking to make sure all entities are present). */
  public static final Theme DIRTYGEONAMESENTITYIDS = new Theme("geonamesEntityIdsDirty", "IDs from GeoNames entities (might contain links to non-YAGO entities)", ThemeGroup.GEONAMES);
  
  /** geonames entity links */
  public static final Theme GEONAMESENTITYIDS = new Theme("yagoGeonamesEntityIds", "IDs from GeoNames entities", ThemeGroup.GEONAMES);

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>();
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(DIRTYGEONAMESENTITYIDS);
  }
  
  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
        new TypeChecker(DIRTYGEONAMESENTITYIDS, GEONAMESENTITYIDS, this)));
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {    
    for (String line : new FileLines(alternateNames, "UTF-8", "Reading GeoNames Wikipedia mappings")) {
      String[] data = line.split("\t");
      
      String language = data[2];
      if (language.equals("link")) {
        // Skip non-Wikipedia link alternate names.
        String alternateName = data[3];
        if (alternateName.startsWith(ENWIKI_PREFIX)) {         
          String geoEntity = FactComponent.forWikipediaTitle(
              alternateName.substring(
                  ENWIKI_PREFIX.length(), alternateName.length()));
          String geoId = data[1];
          // Links missing in YAGO will be dropped by the type-checker.
          output.get(DIRTYGEONAMESENTITYIDS).write(
              new Fact(
                  geoEntity, "<hasGeonamesEntityId>", 
                  FactComponent.forString(geoId)));
        }
      }
    }
  }

  public GeoNamesEntityMapper(File alternateNames) {
    this.alternateNames = alternateNames;
  }
}
