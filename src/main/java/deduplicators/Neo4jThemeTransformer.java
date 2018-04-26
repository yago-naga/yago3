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

package deduplicators;

import java.io.File;
/**
 * Transform facts to node and relation files for Neo4j import.
 * 
*/
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.MetadataExtractor;
import fromOtherSources.WikidataEntityDescriptionExtractor;
import fromOtherSources.WikidataEntityGeoCoordinateExtractor;
import fromOtherSources.WikidataImageExtractor;
import fromOtherSources.WikidataImageLicenseExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.PersonNameExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryGlossExtractor;
import fromWikipedia.ConteXtExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.StructureExtractor;
import fromWikipedia.WikiInfoExtractor;
import fromWikipedia.WikipediaEntityDescriptionExtractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.filehandlers.FileUtils;
import utils.Theme;

public class Neo4jThemeTransformer extends Extractor {
  
  public static final Theme NEO4JDONE = new Theme("neo4j_extractor_done", "This is a dump theme created by Neo4j Extractor to ensure that the extractor is done and allow reusing it.");
  
  public Neo4jThemeTransformer() {
    
  }
  
  public Neo4jThemeTransformer(String yagoOutputFolderPath) {
    OUTPUT_PATH = yagoOutputFolderPath;
    if (OUTPUT_PATH.charAt(OUTPUT_PATH.length()-1) != '/') {
      OUTPUT_PATH += "/";
    }
    Announce.message("Yago output path: " + OUTPUT_PATH);
  }

  private static Map<String, String> entity_wikidataId;

  private static Map<String, wikidataInstanceProperties> wikidataId_properies = new HashMap<>();

  private static class wikidataInstanceProperties {

    String gender;

    Set<String> labels;

    Set<String> givenNames;

    Set<String> familyNames;

    Set<String> longDescriptions;

    Set<String> shortDescriptions;

    Set<String> redirectedFrom;

    Set<String> wikipediaAnchorTexts;

    Set<String> citationTitles;


    public wikidataInstanceProperties() {
      gender = "";
      labels = new HashSet<>();
      givenNames = new HashSet<>();
      familyNames = new HashSet<>();
      longDescriptions = new HashSet<>();
      shortDescriptions = new HashSet<>();
      redirectedFrom = new HashSet<>();
      wikipediaAnchorTexts = new HashSet<>();
      citationTitles = new HashSet<>();
    }

    public String getGender() {
      return gender;
    }

    public String getLabels() {
      if (labels.isEmpty()) {
        return "";
      }
      return String.join(";", labels);
    }

    public String getGivenNames() {
      if (givenNames.isEmpty()) {
        return "";
      }
      return String.join(";", givenNames);
    }

    public String getFamilyNames() {
      if (familyNames.isEmpty()) {
        return "";
      }
      return String.join(";", familyNames);
    }

    public String getLongDescriptions() {
      if (longDescriptions.isEmpty()) {
        return "";
      }
      return String.join(";", longDescriptions);
    }

    public String getShortDescriptions() {
      if (shortDescriptions.isEmpty()) {
        return "";
      }
      return String.join(";", shortDescriptions);
    }

    public String getRedirectedFrom() {
      if (redirectedFrom.isEmpty()) {
        return "";
      }
      return String.join(";", redirectedFrom);
    }

    public String getWikipediaAnchorTexts() {
      if (wikipediaAnchorTexts.isEmpty()) {
        return "";
      }
      return String.join(";", wikipediaAnchorTexts);
    }

    public String getCitationTitles() {
      if (citationTitles.isEmpty()) {
        return "";
      }
      return String.join(";", citationTitles);
    }
  }

  private static String command = "";

  private static final String YAGO_OUTPUT_PATH_PLACEHOLDER = "YAGOOUTPUTPATH/";

  protected static String OUTPUT_PATH;
  
  private static final String commandFile = "import_script.txt";
  
  private static final String wikidataInstancesNodesFileName = "wikidataInstancesNodes.csv";

  private static final String sameAsRelationsFileName = "sameAsRelations.csv";

  private static final String entityNodesFileName = "entityNodes.csv";

  private static final String entityHasTranslationRelationsFileName = "entityHasTranslationRelations.csv";

  private static final String wikipeidiaUrlNodesFileName = "wikipediaUrlNodes.csv";

  private static final String hasWikipediaUrlRelationsFileName = "hasWikipediaUrlRelations.csv";

  private static final String wikidataImageNodesFileName = "wikidataImageNodes.csv";

  private static final String hasImageRelationsFileName = "hasImageRelations.csv";

  private static final String OTRSPermissionNodesFileName = "OTRSPermissionNodes.csv";

  private static final String hasOTRSPermissionRelationsFileName = "hasOTRSPermissionRelations.csv";

  private static final String authorNodesFileName = "authorNodes.csv";

  private static final String hasAuthorRelationsFileName = "hasAuthorRelations.csv";

  private static final String wikidataImageLicenseNodesFileName = "wikidataImageLicenseNodes.csv";

  private static final String hasLicenseRelationsFileName = "hasLicenseRelations.csv";

  private static final String hasInternalWikipediaLinkToRelationsFileName = "hasInternalWikipediaLinkToRelations.csv";

  private static final String typeNodesFileName = "typeNodes.csv";

  private static final String hasTypeRelationsFileName = "hasTypeRelations.csv";

  private static final String isSubclassOfRelationsFileName = "isSubclassOfRelations.csv";

  private static final String typeHasTranslationRelationsFileName = "typeHasTranslationRelations.csv";

  private static final String geoLocationNodesFileName = "geoLocationNodes.csv";
  
  private static final String hasGeoLocationRelationsFileName = "hasGeoLocationRelations.csv";
  
  private static final String metaInformationFileName = "metaInformation.tsv";
  
  private Writer writer;

  private List<String[]> tempNodes = new ArrayList<>();

  private List<String[]> tempRelations = new ArrayList<>();

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>();

    // WikiData links.
    input.add(WikidataLabelExtractor.WIKIDATAINSTANCES);
    
    // Metadata.
    input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));
    
    // Image.
    input.add(WikidataImageExtractor.WIKIDATAIMAGES);
  
    // Image Licenses.
    input.add(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE);
    
    input.addAll(StructureExtractor.STRUCTUREFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
  
    // For YAGO compliance.
    input.add(SchemaExtractor.YAGOSCHEMA);
    
    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    
    // Wikipedie category glosses.
    input.addAll(CategoryGlossExtractor.CATEGORYGLOSSES.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Types and Taxonomy.MAIN
    input.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    input.add(ClassExtractor.YAGOTAXONOMY);
    
    input.addAll(DictionaryExtractor.CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    
    // Dictionary.
    input.add(PersonNameExtractor.PERSONNAMES);
    input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
    input.addAll(GenderExtractor.GENDERBYPRONOUN.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
    input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
    input.add(HardExtractor.HARDWIREDFACTS);
        
    input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
   
    // Keyphrases.
    input.addAll(ConteXtExtractor.CONTEXTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Translation.
    input.addAll(DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    
    // Metadata.
    input.add(MetadataExtractor.METADATAFACTS);
        
    // Entity descriptions.
    input.add(WikidataEntityDescriptionExtractor.WIKIDATAENTITYDESCRIPTIONS);
    input.addAll(WikipediaEntityDescriptionExtractor.WIKIPEDIA_ENTITY_DESCRIPTIONS.inLanguages(MultilingualExtractor.wikipediaLanguages));

    // Entity Geocoordinates
    input.add(WikidataEntityGeoCoordinateExtractor.WIKIDATAENTITYGEOCOORDINATES);


    return input;
  }

  @Override
  public Set<Theme> output() {
    Set<Theme> output = new HashSet<Theme>();
    output.add(NEO4JDONE);
    return output;
  }

  @Override
  public void extract() throws Exception {
    long startTime, startTimeFileMaking;

    D.p("Starting " + WikidataLabelExtractor.WIKIDATAINSTANCES.name);
    startTime = System.currentTimeMillis();
    entity_wikidataId = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getMap(RDFS.sameas);
    WikidataLabelExtractor.WIKIDATAINSTANCES.killCache();
    D.p("Finishing " + WikidataLabelExtractor.WIKIDATAINSTANCES.name + (System.currentTimeMillis() - startTime));


    // Loading yagoWikipediaInfo Multilingual theme.
    // In these theme we want only "<hasWikipediaUrl>" relation. (In AIDAMerger).
    // We first make a temp map of the relation with WikidataInstances and then
    // make the temp list of String arrays as lines of the Csv file.
    D.p("Starting " + wikipeidiaUrlNodesFileName + " " + hasWikipediaUrlRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    for (Theme theme : WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation("<hasWikipediaUrl>")) {
        String entity = f.getSubject();
        String wikipediaUrl = f.getObject();
        entity_wikidataId.putIfAbsent(entity, entity);
        tempNodes.add(new String[] { wikipediaUrl });
        tempRelations.add(new String[] { entity_wikidataId.get(entity), wikipediaUrl });
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }

    writeToFile(wikipeidiaUrlNodesFileName, new String[] { "url:ID(WikipediaUrl)" }, tempNodes);
    tempNodes.clear();
    writeToFile(hasWikipediaUrlRelationsFileName,
        new String[] { ":START_ID(WikidataInstance)", ":END_ID(WikipediaUrl)" }, tempRelations);
    tempRelations.clear();

    addNodesToCommand("WikipediaUrl", wikipeidiaUrlNodesFileName);
    addRelationsToCommand("hasWikipediaUrl", hasWikipediaUrlRelationsFileName);

    D.p(
        "Finishing " + wikipeidiaUrlNodesFileName + " " + hasWikipediaUrlRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));


    // Loading wikidataImages theme.
    // Since each WikidataInstance has only one Image now, we do not need to keep them
    // in a map first. We can directly create the lines of Csv Node and Relation files.
    // Here we also need the relation <hasTrademark> in wikidataImageLicenses.
    D.p("Starting " + wikidataImageNodesFileName + " " + hasImageRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    for (Fact f : WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasImageID)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      String wikiPage = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getObject(f.getObject(), YAGO.hasWikiPage);
      String imageUrl = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getObject(f.getObject(), YAGO.hasImageUrl);
      String tradeMark = WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getObject(wikiPage, YAGO.hasTrademark);
      if (tradeMark == null) {
        tradeMark = "<false>";
      }
      tempNodes.add(new String[] { wikiPage, imageUrl, tradeMark });
      tempRelations.add(new String[] { entity_wikidataId.get(entity), wikiPage });
    }
    WikidataImageExtractor.WIKIDATAIMAGES.killCache();
    writeToFile(wikidataImageNodesFileName,
        new String[] { "wikiPage:ID(WikidataImage)", "imageUrl", "hasTrademark" }, tempNodes);
    tempNodes.clear();
    writeToFile(hasImageRelationsFileName, new String[] { ":START_ID(WikidataInstance)", ":END_ID(WikidataImage)" },
        tempRelations);
    tempRelations.clear();

    addNodesToCommand("WikidataImage", wikidataImageNodesFileName);
    addRelationsToCommand("hasImage", hasImageRelationsFileName);

    D.p
        ("Finishing " + wikidataImageNodesFileName + " " + hasImageRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));


    // Loading wikidataImageLicenses theme.
    // Interesting relations are: <hasLicense>, <hasAuthor> (<hasName> and <hasUrl>), <hasOTRSPermissionTicketID> 
    D.p("Starting " + wikidataImageLicenseNodesFileName + " " + hasLicenseRelationsFileName);
    Set<String> doneLicenseKeys = new HashSet<>();
    startTimeFileMaking = System.currentTimeMillis();
    // For Licenses, we make new License nodes and relationship between ImageWikipage and License.
    for (Fact f : WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getFactsWithRelation(YAGO.hasLicense)) {
      String imageWikipage = f.getSubject();
      String licenseName = f.getObject();
      String licenseUrl = WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getObject(licenseName, YAGO.hasUrl);

      if (!doneLicenseKeys.contains(licenseName)) {
        tempNodes.add(new String[] { licenseName, licenseUrl });
        doneLicenseKeys.add(licenseName);
      }
      tempRelations.add(new String[] { imageWikipage, licenseName });
    }

    writeToFile(wikidataImageLicenseNodesFileName, new String[] { "name:ID(WikidataImageLicense)", "url" },
        tempNodes);
    tempNodes.clear();
    writeToFile(hasLicenseRelationsFileName,
        new String[] { ":START_ID(WikidataImage)", ":END_ID(WikidataImageLicense)" }, tempRelations);
    tempRelations.clear();

    addNodesToCommand("WikidataImageLicense", wikidataImageLicenseNodesFileName);
    addRelationsToCommand("hasLicense", hasLicenseRelationsFileName);

    D.p(
        "Finishing " + wikidataImageLicenseNodesFileName + " " + hasLicenseRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));


    D.p("Starting " + authorNodesFileName + " " + hasAuthorRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    // For Author, we make new Author Nodes.
    // Here we need a mix constraint on both name and url but for now we just put an extra ID.
    // As a result, there might be duplicate authors with different ids.
    for (Fact f : WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getFactsWithRelation(YAGO.hasAuthor)) {
      String imageWikipage = f.getSubject();
      String authorId = f.getObject();
      String name = WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getObject(authorId, YAGO.hasName);
      String url = WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getObject(authorId, YAGO.hasUrl);
      if (name == null) {
        name = "";
      }
      if (url == null) {
        url = "";
      }
      tempNodes.add(new String[] { authorId, name, url });
      tempRelations.add(new String[] { imageWikipage, authorId });
    }

    writeToFile(authorNodesFileName, new String[] { "id:ID(Author)", "name", "url" }, tempNodes);
    tempNodes.clear();
    writeToFile(hasAuthorRelationsFileName, new String[] { ":START_ID(WikidataImage)", ":END_ID(Author)" },
        tempRelations);
    tempRelations.clear();

    addNodesToCommand("Author", authorNodesFileName);
    addRelationsToCommand("hasAuthor", hasAuthorRelationsFileName);

    D.p("Finishing " + authorNodesFileName + " " + hasAuthorRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));


    D.p("Starting " + OTRSPermissionNodesFileName + " " + hasOTRSPermissionRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    for (Fact f : WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getFactsWithRelation(YAGO.hasOTRSId)) {
      String imageWikipage = f.getSubject();
      String otrsId = f.getObject();

      tempNodes.add(new String[] { otrsId });
      tempRelations.add(new String[] { imageWikipage, otrsId });
    }
    WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.killCache();
    writeToFile(OTRSPermissionNodesFileName, new String[] { "id:ID(OTRSPermission)" }, tempNodes);
    tempNodes.clear();
    writeToFile(hasOTRSPermissionRelationsFileName,
        new String[] { ":START_ID(WikidataImage)", ":END_ID(OTRSPermission)" }, tempRelations);
    tempRelations.clear();

    addNodesToCommand("OTRSPermission", OTRSPermissionNodesFileName);
    addRelationsToCommand("hasOTRSPermission", hasOTRSPermissionRelationsFileName);

    D.p(
        "Finishing " + OTRSPermissionNodesFileName + " " + hasOTRSPermissionRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));

    D.p("Starting " + hasInternalWikipediaLinkToRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    for (Theme theme : StructureExtractor.STRUCTUREFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation("<hasInternalWikipediaLinkTo>")) {
        String entity1 = f.getSubject();
        entity_wikidataId.putIfAbsent(entity1, entity1);
        String entity2 = f.getObject();
        entity_wikidataId.putIfAbsent(entity2, entity2);

        String anchorText = theme.factCollection().getObject(f.getId(), "<hasAnchorText>");

        if (anchorText != null) {
          tempRelations.add(new String[] { entity_wikidataId.get(entity1), entity_wikidataId.get(entity2), anchorText });
        } else {
          tempRelations.add(new String[] { entity_wikidataId.get(entity1), entity_wikidataId.get(entity2), "" });
        }
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }
    writeToFile(hasInternalWikipediaLinkToRelationsFileName,
        new String[] { ":START_ID(WikidataInstance)", ":END_ID(WikidataInstance)", "anchorText" }, tempRelations);
    tempRelations.clear();

    addRelationsToCommand("hasInternalWikipediaLinkTo", hasInternalWikipediaLinkToRelationsFileName);
    D.p("Finishing " + hasInternalWikipediaLinkToRelationsFileName + " " + (System.currentTimeMillis() - startTimeFileMaking));
    
    // Geo Locations
    D.p("Starting " + geoLocationNodesFileName + " " + hasGeoLocationRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    for (Fact f : WikidataEntityGeoCoordinateExtractor.WIKIDATAENTITYGEOCOORDINATES.factCollection().getFactsWithRelation(YAGO.hasGeoLocation)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);
      
      String location_id = f.getObject();
      String latitude = WikidataEntityGeoCoordinateExtractor.WIKIDATAENTITYGEOCOORDINATES.factCollection().getObject(location_id, YAGO.hasLatitude);
      String longitude = WikidataEntityGeoCoordinateExtractor.WIKIDATAENTITYGEOCOORDINATES.factCollection().getObject(location_id, YAGO.hasLongitude);
      
      tempNodes.add(new String[] {location_id, latitude, longitude });
      tempRelations.add(new String[] { entity_wikidataId.get(entity), location_id });
    }
    WikidataEntityGeoCoordinateExtractor.WIKIDATAENTITYGEOCOORDINATES.killCache();
    writeToFile(geoLocationNodesFileName,
        new String[] { "location_id:ID(Location)", "latitude:float", "longitude:float" }, tempNodes);
    tempNodes.clear();
    writeToFile(hasGeoLocationRelationsFileName, 
        new String[] { ":START_ID(WikidataInstance)", ":END_ID(Location)" },
        tempRelations);
    tempRelations.clear();

    addNodesToCommand("Location", geoLocationNodesFileName);
    addRelationsToCommand("hasGeoLocation",hasGeoLocationRelationsFileName);

    D.p
        ("Finishing " + geoLocationNodesFileName + " " + hasGeoLocationRelationsFileName + (System.currentTimeMillis() - startTimeFileMaking));

    // Adding Types:
    D.p("Starting " + typeNodesFileName + " " + hasTypeRelationsFileName + " "
        + isSubclassOfRelationsFileName);
    startTimeFileMaking = System.currentTimeMillis();
    Map<String, Set<String>> wikidataId_types = new HashMap<>();
    Map<String, Set<String>> type_glosses = new HashMap<>();
    Set<String> types = new HashSet<>();

    D.p("Starting " + SchemaExtractor.YAGOSCHEMA.name);
    startTime = System.currentTimeMillis();
    for (Fact f : SchemaExtractor.YAGOSCHEMA.factCollection().getFactsWithRelation(RDFS.type)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);
      String type = f.getObject();
      types.add(type);

      wikidataId_types.computeIfAbsent(entity_wikidataId.get(entity), k -> new HashSet<String>()).add(type);
    }
    SchemaExtractor.YAGOSCHEMA.killCache();
    D.p("Finishing " + SchemaExtractor.YAGOSCHEMA.name + " " + (System.currentTimeMillis() - startTime));

    for (Theme theme : CategoryGlossExtractor.CATEGORYGLOSSES.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection()) {
        String type = f.getSubject();
        types.add(type);
        type_glosses.computeIfAbsent(type, k -> new HashSet<>()).add(f.getObject().replaceAll(";", "") + "@" + theme.language());
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }
    
    D.p("Starting " + TransitiveTypeExtractor.TRANSITIVETYPE.name);
    startTime = System.currentTimeMillis();
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE.factCollection().getFactsWithRelation("rdf:type")) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);
      String type = f.getObject();
      types.add(type);

      wikidataId_types.computeIfAbsent(entity_wikidataId.get(entity), k -> new HashSet<String>()).add(type);
    }
    TransitiveTypeExtractor.TRANSITIVETYPE.killCache();
    D.p("Finishing " + TransitiveTypeExtractor.TRANSITIVETYPE.name + (System.currentTimeMillis() - startTime));

    // types from hardWired TODO: only a couple are entities!!! But are added here since there are some entities.
    D.p("Starting " + HardExtractor.HARDWIREDFACTS.name + " " + RDFS.type + " relations");
    startTime = System.currentTimeMillis();
    for (Fact f:HardExtractor.HARDWIREDFACTS.factCollection().getFactsWithRelation(RDFS.type)) {
      if (entity_wikidataId.containsKey(f.getSubject())) {
        wikidataId_types.computeIfAbsent(entity_wikidataId.get(f.getSubject()), k -> new HashSet<String>()).add(f.getObject());
      }
    }
    D.p("Finishing " +HardExtractor.HARDWIREDFACTS.name + " " + RDFS.type + " relations " + (System.currentTimeMillis() - startTime));

    // hasGloss for types from hardWired TODO: These are glosses of relations not types. Like the previous one which the relations have types mostly not entities.
//    D.p("Starting " + HardExtractor.HARDWIREDFACTS.name + " " + YAGO.hasGloss + " relations");
//    startTime = System.currentTimeMillis();
//    for (Fact f:HardExtractor.HARDWIREDFACTS.factCollection().getFactsWithRelation(YAGO.hasGloss)) {
//      String type = f.getSubject();
//
//      if (types.contains(type)) {
//        type_glosses.computeIfAbsent(type, k -> new HashSet<>()).add(f.getObject().replaceAll(";", ""));
//      }
//    }
//    D.p("Finishing " +HardExtractor.HARDWIREDFACTS.name + " " + YAGO.hasGloss + " relations " + (System.currentTimeMillis() - startTime));

    
    // Writing to file:
    for (String wikidataId : wikidataId_types.keySet()) {
      for (String type : wikidataId_types.get(wikidataId)) {
        tempRelations.add(new String[] { wikidataId, type });
      }
    }
    writeToFile(hasTypeRelationsFileName, new String[] { ":START_ID(WikidataInstance)", ":END_ID(Type)" },
        tempRelations);
    tempRelations.clear();


    addRelationsToCommand("hasType", hasTypeRelationsFileName);
    wikidataId_types.clear();

    D.p("Starting " + ClassExtractor.YAGOTAXONOMY.name);
    startTime = System.currentTimeMillis();
    for (Fact f : ClassExtractor.YAGOTAXONOMY.factCollection().getFactsWithRelation(RDFS.subclassOf)) {
      String type1 = f.getSubject();
      types.add(type1);
      String type2 = f.getObject();
      types.add(type2);

      tempRelations.add(new String[] { type1, type2 });
    }
    ClassExtractor.YAGOTAXONOMY.killCache();
    D.p("Finishing " + ClassExtractor.YAGOTAXONOMY.name + (System.currentTimeMillis() - startTime));

    // subClassOf from hardWired
    D.p("Starting " + HardExtractor.HARDWIREDFACTS.name + " " + RDFS.subclassOf + " relations");
    startTime = System.currentTimeMillis();
    for (Fact f:HardExtractor.HARDWIREDFACTS.factCollection().getFactsWithRelation(RDFS.subclassOf)) {
      String type1 = f.getSubject();
      types.add(type1);
      String type2 = f.getObject();
      types.add(type2);

      tempRelations.add(new String[] { type1, type2 });
    }
    D.p("Finishing " +HardExtractor.HARDWIREDFACTS.name + " " + RDFS.subclassOf + " relations " + (System.currentTimeMillis() - startTime));
    
    writeToFile(isSubclassOfRelationsFileName, new String[] { ":START_ID(Type)", ":END_ID(Type)" },
        tempRelations);
    tempRelations.clear();

    for (Theme theme : DictionaryExtractor.CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish())) {
      D.p("Starting " + theme.name);
      for (Fact f : theme.factCollection().getFactsWithRelation(YAGO.hasTranslation)) {
        String type1 = f.getSubject();
        types.add(type1);
        String type2 = f.getObject();
        types.add(type2);

        tempRelations.add(new String[] { type1, type2 });
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }
    writeToFile(typeHasTranslationRelationsFileName, new String[] { ":START_ID(Type)", ":END_ID(Type)" },
        tempRelations);
    tempRelations.clear();

    for (String type : types) {
      if (type_glosses.containsKey(type)) {
        Set<String> glosses = type_glosses.get(type);
        if (glosses.isEmpty()) {
          tempNodes.add(new String[] { type, "" });
        } else {
          tempNodes.add(new String[] { type, String.join(";", glosses) });
        }
      } else {
        tempNodes.add(new String[] { type, "" });
      }
    }

    writeToFile(typeNodesFileName, new String[] { "type:ID(Type)", "gloss:string[]" }, tempNodes);
    tempNodes.clear();

    addNodesToCommand("Type", typeNodesFileName);
    addRelationsToCommand("isSubclassOf", isSubclassOfRelationsFileName);
    addRelationsToCommand("hasTranslation", typeHasTranslationRelationsFileName);
    type_glosses.clear();
    types.clear();

    D.p("Finishing " + typeNodesFileName + " " + hasTypeRelationsFileName + " " 
        + isSubclassOfRelationsFileName + " " + (System.currentTimeMillis() - startTimeFileMaking));
    // Properties of Wikidata Instance:
    D.p("Starting " + wikidataInstancesNodesFileName + " " + sameAsRelationsFileName + " " + entityNodesFileName);
    startTimeFileMaking = System.currentTimeMillis();
    D.p("Starting " + PersonNameExtractor.PERSONNAMES.name);
    startTime = System.currentTimeMillis();
    for (Fact f : PersonNameExtractor.PERSONNAMES.factCollection().getFactsWithRelation("<hasGivenName>")) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).givenNames.add(f.getObject());
    }
    for (Fact f : PersonNameExtractor.PERSONNAMES.factCollection().getFactsWithRelation("<hasFamilyName>")) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).familyNames.add(f.getObject());
    }
    PersonNameExtractor.PERSONNAMES.killCache();
    D.p("Finishing " + PersonNameExtractor.PERSONNAMES.name + (System.currentTimeMillis() - startTime));

    for (Theme theme : GenderExtractor.GENDERBYPRONOUN.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation("<hasGender>")) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).gender = f.getObject();
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }

    for (Theme theme : RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation("<redirectedFrom>")) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).redirectedFrom.add(f.getObject());
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }

    D.p("Starting " + WikidataEntityDescriptionExtractor.WIKIDATAENTITYDESCRIPTIONS.name);
    startTime = System.currentTimeMillis();
    for (Fact f : WikidataEntityDescriptionExtractor.WIKIDATAENTITYDESCRIPTIONS.factCollection().getFactsWithRelation(YAGO.hasShortDescription)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).shortDescriptions
          .add(f.getObject().replaceAll(";", ""));
    }
    WikidataEntityDescriptionExtractor.WIKIDATAENTITYDESCRIPTIONS.killCache();
    D.p("Finishing " + WikidataEntityDescriptionExtractor.WIKIDATAENTITYDESCRIPTIONS.name + (System.currentTimeMillis() - startTime));

    for (Theme theme : WikipediaEntityDescriptionExtractor.WIKIPEDIA_ENTITY_DESCRIPTIONS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation(YAGO.hasLongDescription)) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).longDescriptions
            .add(f.getObject().replaceAll(";", "") + "@" + theme.language());
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }

    D.p("Starting " + PersonNameExtractor.PERSONNAMEHEURISTICS.name);
    startTime = System.currentTimeMillis();
    for (Fact f : PersonNameExtractor.PERSONNAMEHEURISTICS.factCollection().getFactsWithRelation(RDFS.label)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).labels.add(f.getObject());
    }
    PersonNameExtractor.PERSONNAMEHEURISTICS.killCache();
    D.p("Finishing " + PersonNameExtractor.PERSONNAMEHEURISTICS.name + (System.currentTimeMillis() - startTime));

    D.p("Starting " + WikidataLabelExtractor.WIKIPEDIALABELS.name);
    startTime = System.currentTimeMillis();
    for (Fact f : WikidataLabelExtractor.WIKIPEDIALABELS.factCollection().getFactsWithRelation(RDFS.label)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).labels.add(f.getObject());
    }
    WikidataLabelExtractor.WIKIPEDIALABELS.killCache();
    D.p("Finishing " + WikidataLabelExtractor.WIKIPEDIALABELS.name + (System.currentTimeMillis() - startTime));

    D.p("Starting " + WikidataLabelExtractor.WIKIDATAMULTILABELS.name);
    startTime = System.currentTimeMillis();
    for (Fact f : WikidataLabelExtractor.WIKIDATAMULTILABELS.factCollection().getFactsWithRelation(RDFS.label)) {
      String entity = f.getSubject();
      entity_wikidataId.putIfAbsent(entity, entity);

      wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).labels.add(f.getObject());
    }
    WikidataLabelExtractor.WIKIDATAMULTILABELS.killCache();
    D.p("Finishing " + WikidataLabelExtractor.WIKIDATAMULTILABELS.name + (System.currentTimeMillis() - startTime));

    for (Theme theme : DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation(RDFS.label)) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).labels.add(f.getObject());
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }

    for (Theme theme : ConteXtExtractor.CONTEXTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation("<hasWikipediaAnchorText>")) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).wikipediaAnchorTexts
            .add(f.getObject());
      }
      for (Fact f : theme.factCollection().getFactsWithRelation("<hasCitationTitle>")) {
        String entity = f.getSubject();
        entity_wikidataId.putIfAbsent(entity, entity);

        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(entity), k -> new wikidataInstanceProperties()).citationTitles.add(f.getObject());
      }
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }
    
    // label from hardWired TODO again just some are entities some or types/...
    D.p("Starting " + HardExtractor.HARDWIREDFACTS.name + " " + RDFS.label + " relations");
    startTime = System.currentTimeMillis();
    for (Fact f:HardExtractor.HARDWIREDFACTS.factCollection().getFactsWithRelation(RDFS.label)) {
      if (entity_wikidataId.containsKey(f.getSubject())) {
        wikidataId_properies.computeIfAbsent(entity_wikidataId.get(f.getSubject()), k -> new wikidataInstanceProperties()).labels.add(f.getObject());
      }
    }
    D.p("Finishing " +HardExtractor.HARDWIREDFACTS.name + " " + RDFS.label + " relations " + (System.currentTimeMillis() - startTime));
    HardExtractor.HARDWIREDFACTS.killCache();

    // Writing to file:
    for (String wikidataId : new HashSet<String>(entity_wikidataId.values())) {
      wikidataInstanceProperties properties = new wikidataInstanceProperties();
      if (wikidataId_properies.containsKey(wikidataId)) {
        properties = wikidataId_properies.get(wikidataId);
      }

      tempNodes.add(new String[] {
          wikidataId,
          properties.getGender(),
          properties.getLabels(),
          properties.getGivenNames(),
          properties.getFamilyNames(),
          properties.getLongDescriptions(),
          properties.getShortDescriptions(),
          properties.getRedirectedFrom(),
          properties.getWikipediaAnchorTexts(),
          properties.getCitationTitles() });
    }

    writeToFile(wikidataInstancesNodesFileName,
        new String[] {
            "url:ID(WikidataInstance)",
            "gender",
            "labels:string[]",
            "givenNames:string[]",
            "familyNames:string[]",
            "longDescriptions:string[]",
            "shortDescriptions:string[]",
            "redirectsFrom:string[]",
            "wikipediaAnchorTexts:string[]",
            "citationTitles:string[]" },
        tempNodes);
    tempNodes.clear();

    for (Theme theme : DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish())) {
      D.p("Starting " + theme.name);
      startTime = System.currentTimeMillis();
      for (Fact f : theme.factCollection().getFactsWithRelation(YAGO.hasTranslation)) {
        String entity1 = f.getSubject();
        String entity2 = f.getObject();

        tempRelations.add(new String[] { entity1, entity2 });
        // Add them to entities if did not exist
        if (!entity_wikidataId.containsKey(entity1)) {
          tempNodes.add(new String[] { entity1 });
        }
        if (!entity_wikidataId.containsKey(entity2)) {
          tempNodes.add(new String[] { entity2 });
        }
      }
      theme.killCache();
      D.p("Finishing " + theme.name + (System.currentTimeMillis() - startTime));
    }
    writeToFile(entityHasTranslationRelationsFileName,
        new String[] { ":START_ID(Entity)", ":END_ID(Entity)" }, tempRelations);
    tempRelations.clear();

    for (String entity : entity_wikidataId.keySet()) {
      tempNodes.add(new String[] { entity });
      tempRelations.add(new String[] { entity_wikidataId.get(entity), entity });
    }
    writeToFile(entityNodesFileName, new String[] { "entity:ID(Entity)" }, tempNodes);
    tempNodes.clear();
    writeToFile(sameAsRelationsFileName, new String[] { ":START_ID(WikidataInstance)", ":END_ID(Entity)" },
        tempRelations);
    tempRelations.clear();


    addNodesToCommand("WikidataInstance", wikidataInstancesNodesFileName);
    addNodesToCommand("Entity", entityNodesFileName);
    addRelationsToCommand("sameAs", sameAsRelationsFileName);
    addRelationsToCommand("hasTranslation", entityHasTranslationRelationsFileName);
    entity_wikidataId.clear();
    wikidataId_properies.clear();

    D.p("Finishing " + " " + wikidataInstancesNodesFileName + " " + sameAsRelationsFileName + " " + entityNodesFileName + " "
        + (System.currentTimeMillis() - startTimeFileMaking));

    // Meta: 
    D.p("Starting " + metaInformationFileName);
    List<String> languages = new ArrayList<>();
    List<String> wikiSources = new ArrayList<>();
    List<String> values = new ArrayList<>();
    
    values.add("YAGO3");
    
    
    for(Fact f : MetadataExtractor.METADATAFACTS.factCollection().getFactsWithRelation("<_yagoMetadata>")) {
      if (f.getSubject().contains("CreationDate")) {
        values.add(f.getObject());
        continue;
      }
      
      wikiSources.add(f.getObject());
      languages.add(f.getObject().substring(1, 3));
    }
    MetadataExtractor.METADATAFACTS.killCache();
    values.add(String.join(";", languages));
    
    values.add(String.join(";", wikiSources));
    
    Integer wikidatasize = new HashSet<>(entity_wikidataId.values()).size();
    values.add(wikidatasize.toString());
    
    values.add(OUTPUT_PATH);
    
    values.add(new Date().toString());
    
    tempNodes.add(values.toArray(new String[values.size()]));
    
    writeToFile(metaInformationFileName, new String[] {"confName:ID(Meta)", "KB_creationDate", "languages:string[]", 
        "KB_WikipediaSources:string[]", "collection_size", "datasource_YAGO3", "creationDate"}, tempNodes);
    tempNodes.clear();
    command += " --nodes:Meta \"" + YAGO_OUTPUT_PATH_PLACEHOLDER + metaInformationFileName + "\" ";
    D.p("Finishing " + metaInformationFileName);
    
    
    FileWriter writer = new FileWriter(OUTPUT_PATH + commandFile);
    writer.write(command);
    writer.close();
    D.p("Import Script written in file: " + commandFile);
    
    NEO4JDONE.write(new Fact("Extractor", "isDone", "true"));
  }

  private void addRelationsToCommand(String type, String dataFile) {

    command += " --relationships:" + type + " \"" +  YAGO_OUTPUT_PATH_PLACEHOLDER + dataFile + "\"";
  }

  private void addNodesToCommand(String label, String dataFile) {
    command += " --nodes:" + label + " \"" + YAGO_OUTPUT_PATH_PLACEHOLDER + dataFile + "\"";
  }

  private void writeToFile(String fileName, String[] headers, List<String[]> lines) throws IOException {
    writer = FileUtils.getBufferedUTF8Writer(new File(OUTPUT_PATH + fileName));
    
    writer.write(String.join("\t", headers) + "\n");
    
    
    for (String[] line : lines) {
      for (int i = 0; i < line.length; i++) {
        line[i] = '"' + line[i].replaceAll("\"", "") + '"';
      }
      writer.write(String.join("\t", line) + "\n");
    }
    writer.close();
  }
}
