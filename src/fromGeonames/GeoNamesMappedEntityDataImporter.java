package fromGeonames;

import java.io.File;
import java.util.Map;

/**
 * Will import data from GeoNames only for entities which have been matched to YAGO.
 * 
 * @author Johannes Hoffart
 *
 */
public class GeoNamesMappedEntityDataImporter extends GeoNamesDataImporter {

  public GeoNamesMappedEntityDataImporter(File geonamesFolder) {
    this.geonamesFolder = geonamesFolder;
  }

  @Override
  public boolean shouldImportForGeonamesId(String geonamesId, Map<String, String> geoEntityId2yago) {
    return geoEntityId2yago.containsKey(geonamesId);
  }
}
