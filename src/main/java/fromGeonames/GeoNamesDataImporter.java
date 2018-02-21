/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Johannes Hoffart.

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

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import fromThemes.InfoboxMapper;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import utils.FactCollection;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * The GeoNamesEntityMapper maps geonames entities to Wikipedia entities.
 * 
 * Needs a directory with these GeoNames files as input: - allCountries.txt -
 * countryInfo.txt - hierarchy.txt
 * 
*/
public class GeoNamesDataImporter extends DataExtractor {

  public static final String GEO_ENTITY_PREFIX = "geoentity_";

  /** geonames data for mapped entities */
  public static final Theme GEONAMES_MAPPED_DATA = new Theme("mappedGeonamesData",
      "Data from GeoNames for mapped Wikipedia entities, e.g. coordinates, alternative names, locatedIn hierarchy, neighbor of");

  /** geonames mapped data sources */
  public static final Theme GEONAMES_MAPPED_DATA_SOURCES = new Theme("mappedGeonamesSources", "Data from GeoNames for mapped Wikipedia entities.");

  /** geonames data for entities that could not be mapped */
  public static final Theme GEONAMES_ONLY_DATA = new Theme("yagoGeonamesOnlyData",
      "Data from GeoNames for all non-mapped entities, e.g. coordinates, alternative names, locatedIn hierarchy, neighbor of", ThemeGroup.GEONAMES);

  /** geonames types */
  public static final Theme GEONAMES_TYPES = new Theme("yagoGeonamesTypes", "All GeoNames types for both mapped and non-mapped entities",
      ThemeGroup.GEONAMES);

  /** geonames types sources */
  public static final Theme GEONAMES_TYPES_SOURCE = new Theme("yagoGeonamesTypesSources",
      "Source for all GeoNames types for both mapped and non-mapped entities", ThemeGroup.META);

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(
        Arrays.asList(GeoNamesClassMapper.GEONAMESCLASSSIDS, GeoNamesEntityMapper.GEONAMESENTITYIDS, InfoboxMapper.INFOBOXFACTS.inEnglish()));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(GEONAMES_MAPPED_DATA, GEONAMES_ONLY_DATA, GEONAMES_TYPES, GEONAMES_TYPES_SOURCE, GEONAMES_MAPPED_DATA_SOURCES);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<Theme>(GeoNamesEntityMapper.GEONAMESENTITYIDS, GeoNamesClassMapper.GEONAMESCLASSSIDS);
  }

  @Override
  public void extract() throws Exception {
    // FactWriter mappedOut = output.get(GEONAMESMAPPEDDATA);
    // FactWriter onlyOut = output.get(GEONAMESONLYDATA);
    // FactWriter typeOut = output.get(GEONAMESTYPES);

    FactCollection mappedEntityIds = GeoNamesEntityMapper.GEONAMESENTITYIDS.factCollection();
    Map<String, String> geoEntityId2yago = mappedEntityIds.getReverseMap("<hasGeonamesEntityId>");
    FactCollection mappedClassIds = GeoNamesClassMapper.GEONAMESCLASSSIDS.factCollection();
    Map<String, String> geoClassId2yago = mappedClassIds.getReverseMap("<hasGeonamesClassId>");
    FactSource ibFacts = InfoboxMapper.INFOBOXFACTS.inEnglish();

    Map<Integer, String> geoId2name = extractAllCountries(new File(inputData, "allCountries.txt"), GEONAMES_MAPPED_DATA, GEONAMES_ONLY_DATA,
        GEONAMES_TYPES, geoEntityId2yago, geoClassId2yago);
    extractHierarchy(new File(inputData, "hierarchy.txt"), GEONAMES_MAPPED_DATA, GEONAMES_ONLY_DATA, geoEntityId2yago, geoId2name);
    extractCountryInfo(new File(inputData, "countryInfo.txt"), GEONAMES_MAPPED_DATA, ibFacts);
  }

  private Map<Integer, String> extractAllCountries(File geodata, Theme mappedOut, Theme onlyOut, Theme typeOut, Map<String, String> geoEntityId2yago,
      Map<String, String> geoClassId2yago) throws NumberFormatException, IOException {
    Map<Integer, String> geoId2name = new HashMap<>();
    for (String line : new FileLines(geodata, "UTF-8", "Importing GeoNames entity data")) {
      String[] data = line.split("\t");

      String geonamesId = data[0];
      String geonamesIdYagoFormat = FactComponent.forString(geonamesId);
      String name = data[1];
      String canonicalName = FactComponent.forStringWithLanguage(name, "eng");
      geoId2name.put(Integer.parseInt(geonamesId), name);

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

      // Only <> will be added if not mapped
      name = FactComponent.forYagoEntity(getYagoNameForGeonamesId(name, geonamesIdYagoFormat, geoEntityId2yago));

      // Decide where to write to - if mapped, write to core facts, if
      // not, separately.
      Theme out = onlyOut;
      Theme outSource = null;
      Theme typeOutSource = null;
      if (geoEntityId2yago.containsKey(geonamesIdYagoFormat)) {
        out = mappedOut;
        outSource = GEONAMES_MAPPED_DATA_SOURCES;
        typeOutSource = GEONAMES_TYPES_SOURCE;
      }

      writeFact(out, new Fact(name, RDFS.label, canonicalName), outSource);
      writeFact(out, new Fact(name, "<hasLatitude>", FactComponent.forStringWithDatatype(lati.toString(), "<degrees>")), outSource);
      writeFact(out, new Fact(name, "<hasLongitude>", FactComponent.forStringWithDatatype(longi.toString(), "<degrees>")), outSource);
      writeFact(out, new Fact(name, "<hasGeonamesEntityId>", geonamesIdYagoFormat), outSource);
      writeFact(typeOut, new Fact(name, RDFS.type, geoClassId2yago.get(FactComponent.forString(fc))), typeOutSource);

      if (namesList != null) {
        for (String alternateName : namesList) {
          writeFact(out, new Fact(name, RDFS.label, FactComponent.forString(alternateName)), outSource);
        }
      }
    }
    return geoId2name;
  }

  private void extractHierarchy(File geodata, Theme mappedOut, Theme onlyOut, Map<String, String> geoEntityId2yago, Map<Integer, String> geoId2name)
      throws IOException {
    for (String line : new FileLines(geodata, "UTF-8", "Importing GeoNames locatedIn data")) {
      String[] data = line.split("\t");

      String parentName = geoId2name.get(Integer.parseInt(data[0]));
      String childName = geoId2name.get(Integer.parseInt(data[1]));

      if (parentName == null || childName == null) {
        Announce.debug("Skipping GeoNames locatedIn fact because of missing " + "child or parent id. GeoNames data is not clean.");
        continue;
      }

      String parent = FactComponent.forString(data[0]);
      String child = FactComponent.forString(data[1]);
      Theme out = onlyOut;
      Theme outSource = null;
      // When both parent and child are part of Wikipedia, write to
      // mapped.
      if (geoEntityId2yago.containsKey(parent) && geoEntityId2yago.containsKey(child)) {
        out = mappedOut;
        outSource = GEONAMES_MAPPED_DATA_SOURCES;
      }

      String childEntity = getYagoNameForGeonamesId(childName, child, geoEntityId2yago);
      String parentEntity = getYagoNameForGeonamesId(parentName, parent, geoEntityId2yago);

      if (childEntity != null && parentEntity != null) {
        writeFact(out, new Fact(childEntity, "<isLocatedIn>", parentEntity), outSource);
      }
    }
  }

  private void extractCountryInfo(File geodata, Theme mappedOut, FactSource ibFacts) throws IOException {
    Map<String, String> tld2yago = new HashMap<>();
    for (Fact f : ibFacts) {
      if (f.getRelation().equals("<hasTLD>")) {
        tld2yago.put(FactComponent.asJavaString(f.getArg(2)), f.getArg(1));
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
          String neighbor = tld2yago.get("." + nbTLD.toLowerCase() + "");

          if (neighbor != null) {
            writeFact(mappedOut, new Fact(country, "<hasNeighbor>", neighbor), GEONAMES_MAPPED_DATA_SOURCES);
          } else {
            Announce.debug("TLD '" + tld + "' not available in YAGO, not adding neighbor for '" + country + "'");
          }
        }
      }
    }
  }

  private void writeFact(Theme factTheme, Fact f, Theme metaTheme) throws IOException {
    factTheme.write(f);
    if (metaTheme != null) {
      Fact sourceFact = f.metaFact(YAGO.extractionSource, "http://geonames.org");
      metaTheme.write(sourceFact);
    }
  }

  private String getYagoNameForGeonamesId(String name, String geonamesIdYagoFormat, Map<String, String> geoEntityId2yago) {
    if (geoEntityId2yago.containsKey(geonamesIdYagoFormat)) {
      return geoEntityId2yago.get(geonamesIdYagoFormat);
    } else {
      // To avoid clashes with canoncial Wikipedia names, add the GeoNames
      // id
      // to unmatched GeoNames entities.
      String geonamesIdOnly = FactComponent.stripQuotes(FactComponent.getString(geonamesIdYagoFormat));
      return FactComponent.forYagoEntity(GEO_ENTITY_PREFIX + name + "_" + geonamesIdOnly);
    }
  }

  public GeoNamesDataImporter(File geonamesFolder) {
    super(geonamesFolder);
  }

  public GeoNamesDataImporter() {
    this(new File(Parameters.get("geonames")));
  }
}
