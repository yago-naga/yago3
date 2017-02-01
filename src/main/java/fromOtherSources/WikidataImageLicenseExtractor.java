package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.DataExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.Theme;

public class WikidataImageLicenseExtractor extends DataExtractor{
  
  public static class authorUser {
    String name;
    String url;
    
    public authorUser() {
      this.name = null;
      this.url = null;
    }
  }
  
  private static class licenseReturn {
    Set<String> imageLicenses;
    Map<String, String> addedLicenses;
    
    public licenseReturn() {
      imageLicenses = new HashSet<String>();
      addedLicenses = new HashMap<String, String>();
    }
  }
  
  public static final Theme WIKIDATAIMAGELICENSE = new Theme("wikidataImageInformation", 
      "Licences extracted for wikidata Images");
  
  private static final String COMMONS_WIKI = "commons_wiki";
  
  private static final String wikipediaUrl = "wikipedia.org/wiki/";
  private static final String wikimediaCommonUrl = "https://commons.wikimedia.org/wiki/";
  private static final String flickerUsersUrl = "https://www.flickr.com/people/";
  
  private static final List<String> CC_TYPES = Arrays.asList("by","by-sa","by-nd","by-nc","by-nc-sa","by-nc-nd");
  private static final List<String> CC_VERSIONS = Arrays.asList("1.0","2.0","2.5","3.0","4.0"); 
  
  private static int authorCnt = 0;
  private static int licenseCnt = 0;
  

  
  //TODO: check unicode flag
  private static Pattern authorFieldPattern = Pattern.compile("\\|\\s*(?:author|artist|creator)\\s*=\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
  private static Pattern trademark = Pattern.compile("\\{\\{trademarked(?:\\}\\}|\\|)", Pattern.CASE_INSENSITIVE);
  private static Pattern attributionPattern = Pattern.compile("\\{\\{(attribution(.*?))\\}\\}", Pattern.CASE_INSENSITIVE);
  private static Pattern OTRSPermissionPattern = Pattern.compile("(?:\\{\\{|\\|)(?:Permission)?OTRS\\|(?:id=)?(\\d+)(?:\\}\\}|\\|)");
  
  private static Map<String, Pattern> authorPatterns;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    tempMap.put("Unknown", Pattern.compile("\\{\\{unknown\\|author\\}\\}", Pattern.CASE_INSENSITIVE));
    
    tempMap.put("wikiLanguageUser", Pattern.compile("\\[\\[(?::?(.{2,3}):)?((?:user:)?.+?)(?:\\|(.+?))?\\]\\]", Pattern.CASE_INSENSITIVE));
    tempMap.put("wikiCommonUser", Pattern.compile("\\{\\{((?:user|u|creator):(.+?))\\}\\}", Pattern.CASE_INSENSITIVE));// chk u:stuff
    tempMap.put("wikiGermanUser", Pattern.compile("\\{\\{ud:(.+?)\\}\\}", Pattern.CASE_INSENSITIVE));
    tempMap.put("wikiUserAtProject", Pattern.compile("\\{\\{(?:(?:user at project)|(?:original uploader))\\|(.*?)\\|(?:wikipedia\\|)?(.{2,3})\\}\\}"));
    
    tempMap.put("flickrUser1", Pattern.compile("\\[\\[flickruser:(.+?)(?:\\|(.+))?\\]\\]"));
    tempMap.put("externalLink", Pattern.compile("^\\[[^\\[](.*?)(?:(?:\\s+(.*)\\])|\\])"));
    
    authorPatterns = Collections.unmodifiableMap(tempMap);
  }
  
  private static final Map<String, Pattern> imageLicensePatterns;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    
    tempMap.put("CreativeCommonsLicense", Pattern.compile("(?:\\{\\{|\\|)cc-(by(?:-sa|-nd|-nc|-nc-sa|-nc-nd|))-(\\d\\.\\d|all)(?:-(.{2,3}))?(?:\\}\\}|\\|)", Pattern.CASE_INSENSITIVE));
    
    tempMap.put("GNUFreeDocumentationLicense", Pattern.compile("(?:\\{\\{|\\|)(GFDL)(?:\\}\\}|\\|)"));
    tempMap.put("GNUGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(GPL)(?:\\}\\}|\\|)"));
    tempMap.put("GNULesserGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(LGPL)(?:\\}\\}|\\|)"));
    tempMap.put("GNUAfferoGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(AGPL)(?:\\}\\}|\\|)"));
    
    tempMap.put("FreeArtLicense", Pattern.compile("artlibre.org/licence/"));
    
    tempMap.put("OpenGovernmentLicence", Pattern.compile("(?:\\{\\{|\\|)OGL(\\d)?(?:\\}\\}|\\|)"));

    tempMap.put("OpenDataCommonsLicense", Pattern.compile("(?:\\{\\{|\\|)(ODbL)(?:\\}\\}|\\|)"));
    
    tempMap.put("publicDomainLicense", Pattern.compile("(?:\\{\\{|\\|)(PD[^\\|\\}]*)(?:\\}\\}|\\|)"));
    
    
    
    //tempMap.put("flickrreview", Pattern.compile("(?:\\{\\{|\\|)(flickrreview\\|(.*))(?:\\}\\}|\\|)")); // http://commons.wikimedia.org/wiki/File:CIAS_2013_-_2014_Ford_Transit_Connect_Titanium_(8485216955).jpg
    
    imageLicensePatterns = Collections.unmodifiableMap(tempMap);
  }

  private static final Map<String, String> hardCodedLicenses;
  static {
    Map<String, String> tempMap = new HashMap<>();
    tempMap.put("publicDomainLicense",            "https://en.wikipedia.org/wiki/Public_domain");
    tempMap.put("GNUFreeDocumentationLicense",    "https://www.gnu.org/licenses/gfdl");
    tempMap.put("GNUGeneralPublicLicense",        "https://www.gnu.org/licenses/gpl");
    tempMap.put("GNULesserGeneralPublicLicense",  "https://www.gnu.org/licenses/lgpl");
    tempMap.put("GNUAfferoGeneralPublicLicense",  "https://www.gnu.org/licenses/agpl");
    tempMap.put("FreeArtLicense",                 "http://artlibre.org/licence/lal/en/");
    tempMap.put("OpenDataCommonsLicense",         "http://opendatacommons.org/licenses/odbl/");
    
    for(String type:CC_TYPES)
      for(String version:CC_VERSIONS)
        tempMap.put("CreativeCommonsLicense-" + type + "-" + version , "https://creativecommons.org/licenses/" + type + "/" + version);
  
    hardCodedLicenses = Collections.unmodifiableMap(tempMap);
  }
  
  
  public WikidataImageLicenseExtractor(File input) {
    super(input);
  }
  
  public WikidataImageLicenseExtractor() {
    this(Parameters.getFile(COMMONS_WIKI));
  }
  
  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(WikidataImageExtractor.WIKIDATAIMAGES));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(WIKIDATAIMAGELICENSE));
  }

  @Override
  public void extract() throws Exception {
    
    writeHardcodedLicenses();
    
    Reader in = FileUtils.getBufferedUTF8Reader(inputData);
    
    // TODO: maybe add imageFileName to images.tsv?
    // Mapping of image file name to its url.
    Map<String, String> fileNameToUrl = getFileNames();
    
    // Go through commonswiki dump and stop at titles which are File
    while(FileLines.findIgnoreCase(in, "<title>File:" ) != -1) {
      String imageFileName = FileLines.readToBoundary(in, "</title>");
      imageFileName = Char17.decodeAmpersand(imageFileName);
      // If the title was not null and was one of the image files that we extracted before:
      if (imageFileName != null && fileNameToUrl.containsKey(imageFileName)){
        String text = FileLines.readBetween(in, "<text", "</text>");
        
        authorUser author = new authorUser();
        licenseReturn licenses = new licenseReturn();
        String permissionOTRS = null; 
        Boolean trademark = false;
        String attribution = null;
        
        author = findAuthor(text);
        licenses = findLicense(text);
        permissionOTRS = findOTRSPermission(text);
        trademark = findTrademark(text);
        attribution = findAttribution(text.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " "));
        String imageUrl = FactComponent.forYagoEntity(fileNameToUrl.get(imageFileName));
        
        // Write available information:
        for(String licenseID:licenses.addedLicenses.keySet()) {
          String url = FactComponent.forYagoEntity(licenses.addedLicenses.get(licenseID));
          WIKIDATAIMAGELICENSE.write(new Fact(licenseID, YAGO.hasUrl, url));
        }

        for(String license:licenses.imageLicenses)
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasLicense, FactComponent.forYagoEntity(license)));
          
        if(author.name != null || author.url != null) {
          String authorID = FactComponent.forYagoEntity("author_" + (++authorCnt));
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasAuthor, authorID));
          if(author.name != null)
            WIKIDATAIMAGELICENSE.write(new Fact(authorID, YAGO.hasName, FactComponent.forYagoEntity(author.name)));
          if(author.url != null)
            WIKIDATAIMAGELICENSE.write(new Fact(authorID, YAGO.hasUrl, FactComponent.forYagoEntity(author.url)));
        }
        
        if(permissionOTRS != null)
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasOTRSId, FactComponent.forYagoEntity(permissionOTRS)));
          
        if(trademark)
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasTrademark, FactComponent.forYagoEntity(trademark.toString())));
        
        //if(attribution != null)
          //WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasAttributionTag, attribution));
        // something else: attcc : http://commons.wikimedia.org/wiki/File:Ph_locator_camiguin_mambajao.png
      }
    }
    in.close();
    
  }
  
  /**
   * Return a mapping from image file names to their image wiki page.
   * @return
   * @throws IOException
   */
  private Map<String, String> getFileNames() throws IOException {
    Map<String, String> fileNameToUrl = getFileNames();

    // Load extracted images. Facts here will be: <yagoEntity> <hasImageID> <image_ID>
    Set<Fact> entityImages = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasImageID);
    
    for(Fact f:entityImages) {
      String imageID = f.getObject();
      String imageWikiPageUrl = FactComponent.stripBrackets(WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getObject(imageID, YAGO.hasWikiPage));
      String imageFileName = imageWikiPageUrl.substring(imageWikiPageUrl.indexOf("/wiki/File:") + "/wiki/File:".length()).replaceAll("_", " ");

      fileNameToUrl.put(imageFileName, imageWikiPageUrl);
    }
    return fileNameToUrl;
  }

  /**
   * Find if there exist an OTRS permission tag and return its ticket id.
   * @param text
   * @return
   */
  private String findOTRSPermission(String text) {
    Matcher matcher = OTRSPermissionPattern.matcher(text);
    if(matcher.find())
      return matcher.group(1);
    return null;
  }

  
  /**
   * This function create facts for hard coded licenses such as:
   * <publicDomainLicense> <hasUrl> <https://en.wikipedia.org/wiki/Public_domain>
   * @throws IOException 
   */
  private static void writeHardcodedLicenses() throws IOException {
    for (String licenseKey:hardCodedLicenses.keySet()) { 
      Fact f = new Fact(FactComponent.forYagoEntity(licenseKey), YAGO.hasUrl, FactComponent.forYagoEntity(hardCodedLicenses.get(licenseKey)));
      WIKIDATAIMAGELICENSE.write(f);
    }
  }
  
  /**
   * Find if there exist an Attribution tag.
   * TODO: what to to with the info after it like user, ... There is also a Attribution-CC....
   * @param text
   * @return
   */
  private String findAttribution(String text) {
    Matcher matcher = attributionPattern.matcher(text);
    if(matcher.find())
      return matcher.group(1);
    return null;
  }
  
  /**
   * Find existing licenses from the defined licenses.
   * @param text
   * @return
   */
  private static licenseReturn findLicense(String text) {
    licenseReturn licenses = new licenseReturn();
    
    for(String key:imageLicensePatterns.keySet()) {
      Matcher licenseMatcher = imageLicensePatterns.get(key).matcher(text);
      while(licenseMatcher.find()) {
        if (hardCodedLicenses.containsKey(key)) {
          licenses.imageLicenses.add(key);
        } 
        else if (key.equals("CreativeCommonsLicense") && licenseMatcher.group(3) == null) {
          if(licenseMatcher.group(2).equals("all"))
            for(String version:CC_VERSIONS)
              licenses.imageLicenses.add(key + "-" + licenseMatcher.group(1) + "-" + version);
          else
            licenses.imageLicenses.add(key + "-" + licenseMatcher.group(1) + "-" + licenseMatcher.group(2));
        }
        else {
          String licenseID = FactComponent.forYagoEntity("license_" + (++licenseCnt));
          licenses.imageLicenses.add(licenseID);
          
          String url = null;
          switch(key) {
            case "CreativeCommonsLicense":
              if(licenseMatcher.group(2).equals("all"))
                for(String version:CC_VERSIONS) {
                  url = hardCodedLicenses.get(key + "-" + licenseMatcher.group(1) + "-" + version) + "/" + licenseMatcher.group(3);
                  licenses.addedLicenses.put(licenseID, url);
                }
              else {
                url = hardCodedLicenses.get(key + "-" + licenseMatcher.group(1) + "-" + licenseMatcher.group(2)) + "/" + licenseMatcher.group(3);
                licenses.addedLicenses.put(licenseID, url);
              }
              break;
            
            case "OpenGovernmentLicence":
              url = "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/" + ( (licenseMatcher.group(1) == null) ? "1" : licenseMatcher.group(1));
              licenses.addedLicenses.put(licenseID, url);
              break;

          }
        }
      }
        
    }
    
    return licenses;
  }
  
  /**
   * Find the author of the image, return its name and url if available.
   * @param text
   * @return
   */
  private static authorUser findAuthor(String text) {
    
    authorUser author = new authorUser();
    Matcher matcher = authorFieldPattern.matcher(text);
    
    if(matcher.find()) {
      String authorFieldText = matcher.group(1);
      for (String key:authorPatterns.keySet()) {
        Matcher authorTypeMatcher = authorPatterns.get(key).matcher(authorFieldText);
        if (authorTypeMatcher.find()) {
          switch(key){
            
            case "Unknown":
              author.name = "Unknown";
              break;
              
            case "wikiLanguageUser":
              if (authorTypeMatcher.group(1) != null) 
                author.url = "http://" + authorTypeMatcher.group(1) + "." + wikipediaUrl + authorTypeMatcher.group(2).replaceAll(" ", "_");
              else 
                author.url = wikimediaCommonUrl + authorTypeMatcher.group(2).replaceAll(" ", "_");
              if (authorTypeMatcher.group(3) != null)
                author.name = authorTypeMatcher.group(3);
              break;
              
            case "wikiCommonUser":
              author.url = wikimediaCommonUrl + authorTypeMatcher.group(1);
              author.name = authorTypeMatcher.group(2);
              break;
              
            case "wikiGermanUser":
              author.url = "https://de." + wikipediaUrl + authorTypeMatcher.group(1);
              author.name = authorTypeMatcher.group(1);
              break;
              
            case "flickrUser1":
              author.url = flickerUsersUrl + authorTypeMatcher.group(1);
              break;
              
            case "externalLink":
              author.url = authorTypeMatcher.group(1);
              if(authorTypeMatcher.group(2) != null)
                author.name = authorTypeMatcher.group(2);
              break;
              
            case "wikiUserAtProject":
              author.url = "http://" + authorTypeMatcher.group(2) + "." + wikipediaUrl + "User:" + authorTypeMatcher.group(1).replaceAll(" ", "_");
              break;
              
          }
        }
      }
      if (author.equals(new authorUser())) {
        author.name = authorFieldText;
      }
    }
    
    return author;
  }
  
  /**
   * Find if there exist a trademak tag.
   * @param text
   * @return
   */
  private static Boolean findTrademark(String text) {
    Matcher matcher = trademark.matcher(text);
    if (matcher.find())
      return true;
    return false;
  }

}
