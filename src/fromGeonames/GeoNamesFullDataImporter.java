package fromGeonames;

import java.io.File;
import java.util.Map;

/**
 * Will import ALL data from GeoNames. This means ~8 million entities!
 * 
 * @author Johannes Hoffart
 *
 */
public class GeoNamesFullDataImporter extends GeoNamesDataImporter {

  public GeoNamesFullDataImporter(File geonamesFolder) {
    this.geonamesFolder = geonamesFolder;
  }

  @Override
  public boolean shouldImportForGeonamesId(String geonamesId, Map<String, String> geoEntityId2yago) {
    return true;
  }
}
