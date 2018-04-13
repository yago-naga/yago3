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
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.Theme;

/**
 * Extract licenses for images from commons wiki dump.
 * 
*/

public class WikidataImageLicenseExtractor extends DataExtractor {
 
  public static final Theme WIKIDATAIMAGELICENSE = new Theme("wikidataImageLicenses", 
      "Licences extracted for wikidata Images");
  
  private static final String COMMONS_WIKI = "commons_wiki";
  
  private static final String wikipediaUrl = "wikipedia.org/wiki/";
  private static final String wikimediaCommonUrl = "https://commons.wikimedia.org/wiki/";
  private static final String flickerUsersUrl = "https://www.flickr.com/people/";
  
  private static final List<String> CC_TYPES = Arrays.asList("BY","BY-SA","BY-ND","BY-NC","BY-NC-SA","BY-NC-ND");
  private static final List<String> CC_VERSIONS = Arrays.asList("1.0","2.0","2.5","3.0","4.0"); 
  
  private static int authorCnt = 0;
  private static int licenseCnt = 0;
  
  public WikidataImageLicenseExtractor(File input) {
    super(input);
  }
  
  public WikidataImageLicenseExtractor() {
    this(Parameters.getFile(COMMONS_WIKI));
  }
  
  
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
  
  private static Pattern authorFieldPattern = Pattern.compile("\\|\\s*(?:author|artist|creator)\\s*=\\s*(.+?)(?!\\{\\{.*?\\|.*?\\}\\})(?:[^a-zA-Z]\\|)", Pattern.CASE_INSENSITIVE);
  private static Pattern trademark = Pattern.compile("\\{\\{trademarked(?:\\}\\}|\\|)", Pattern.CASE_INSENSITIVE);
  //private static Pattern attributionPattern = Pattern.compile("\\{\\{(attribution(.*?))\\}\\}", Pattern.CASE_INSENSITIVE);
  private static Pattern OTRSPermissionPattern = Pattern.compile("(?:\\{\\{|\\|)(?:Permission)?OTRS\\|(?:id=)?(\\d+)(?:\\}\\}|\\|)");
  
  private static Map<String, Pattern> authorPatterns;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    tempMap.put("Unknown", Pattern.compile("\\{\\{unknown\\|author\\}\\}", Pattern.CASE_INSENSITIVE));
    
    tempMap.put("wikiLanguageUser", Pattern.compile("\\[\\[(?::?(.{2,3}):)?((?!:File:)(?:user:)?.+?)(?:\\|(.+?))?\\]\\]", Pattern.CASE_INSENSITIVE));
    tempMap.put("wikiCommonUser", Pattern.compile("\\{\\{((user|u|creator)(:|\\|)(.+?))\\}\\}", Pattern.CASE_INSENSITIVE));// chk u:stuff
    tempMap.put("wikiGermanUser", Pattern.compile("\\{\\{ud:(.+?)\\}\\}", Pattern.CASE_INSENSITIVE));
    tempMap.put("wikiUserAtProject", Pattern.compile("\\{\\{(?:(?:user at project)|(?:original uploader))\\|(.*?)\\|(?:wikipedia\\|)?(.{2,3})\\}\\}"));
    
    tempMap.put("flickrUser", Pattern.compile("\\[\\[flickruser:(.+?)(?:\\|(.+))?\\]\\]"));
    tempMap.put("externalLink", Pattern.compile("^\\[([^\\[].*?)(?:(?:\\s+(.*)\\])|\\])"));
    
    authorPatterns = Collections.unmodifiableMap(tempMap);
  }
  
  private static final Map<String, Pattern> imageLicensePatterns;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    
    tempMap.put("CreativeCommonsLicense", Pattern.compile("(?:\\{\\{|\\|)cc-(by(?:-sa|-nd|-nc|-nc-sa|-nc-nd|))-(\\d\\.\\d|all)(?:-(.{2,3}))?(?:\\}\\}|\\|)", Pattern.CASE_INSENSITIVE));
    
    tempMap.put("GNUFreeDocumentationLicense", Pattern.compile("(?:\\{\\{|\\|)(GFDL)(-\\d\\.\\d)?(-\\w+)?(?:\\}\\}|\\|)"));
    tempMap.put("GNUGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(GPL)(v\\d\\+)?(v\\d)?( only)?(?:\\}\\}|\\|)"));
    tempMap.put("GNULesserGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(LGPL)(v\\d(?:\\.\\d)?\\+?)?( only)?(?:\\}\\}|\\|)"));
    tempMap.put("GNUAfferoGeneralPublicLicense", Pattern.compile("(?:\\{\\{|\\|)(AGPL)(?:\\}\\}|\\|)"));
    
    tempMap.put("FreeArtLicense", Pattern.compile("artlibre.org/licence/"));
    
    tempMap.put("OpenGovernmentLicence", Pattern.compile("(?:\\{\\{|\\|)OGL(\\d)?(?:\\}\\}|\\|)"));

    tempMap.put("OpenDataCommonsLicense", Pattern.compile("(?:\\{\\{|\\|)(ODbL)(?:\\}\\}|\\|)"));
    
    tempMap.put("PublicDomainLicense", Pattern.compile("(?:\\{\\{|\\|)(PD[^\\|\\}]*)(?:\\}\\}|\\|)"));
    
    //tempMap.put("flickrreview", Pattern.compile("(?:\\{\\{|\\|)(flickrreview\\|(.*))(?:\\}\\}|\\|)")); // http://commons.wikimedia.org/wiki/File:CIAS_2013_-_2014_Ford_Transit_Connect_Titanium_(8485216955).jpg
    
    imageLicensePatterns = Collections.unmodifiableMap(tempMap);
  }

  private static final Map<String, String> hardCodedLicenses;
  static {
    Map<String, String> tempMap = new HashMap<>();
    tempMap.put("PublicDomainLicense",            "https://en.wikipedia.org/wiki/Public_domain");
    tempMap.put("GNUFreeDocumentationLicense-1.1",    "https://www.gnu.org/licenses/fdl-1.1");
    tempMap.put("GNUFreeDocumentationLicense-1.2",    "https://www.gnu.org/licenses/fdl-1.2");
    tempMap.put("GNUFreeDocumentationLicense-1.3",    "https://www.gnu.org/licenses/fdl-1.3");
    tempMap.put("GNUGeneralPublicLicense-v2",        "https://www.gnu.org/licenses/gpl-2.0");
    tempMap.put("GNUGeneralPublicLicense-v3",        "https://www.gnu.org/licenses/gpl-3.0");
    tempMap.put("GNULesserGeneralPublicLicense",  "https://www.gnu.org/licenses/lgpl");
    tempMap.put("GNUAfferoGeneralPublicLicense",  "https://www.gnu.org/licenses/agpl");
    tempMap.put("FreeArtLicense",                 "http://artlibre.org/licence/lal/en/");
    tempMap.put("OpenDataCommonsLicense",         "http://opendatacommons.org/licenses/odbl/");
    
    for(String type:CC_TYPES)
      for(String version:CC_VERSIONS)
        tempMap.put("CreativeCommonsLicense-" + type + "-" + version , "https://creativecommons.org/licenses/" + type.toLowerCase() + "/" + version);
  
    hardCodedLicenses = Collections.unmodifiableMap(tempMap);
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
   
    int cntImagesNOLicense = 0;
    int cntImages = 0;
    
    writeHardcodedLicenses();
    
    Reader in = FileUtils.getBufferedUTF8Reader(inputData);
    
    // Mapping of image file name to its url.
    Map<String, String> imageUrlByName = getFileNames();
    
    // Go through commonswiki dump and stop at titles which are File
    while (FileLines.findIgnoreCase(in, "<title>File:" ) != -1) {
      String imageFileName = FileLines.readToBoundary(in, "</title>");
      imageFileName = Char17.decodeAmpersand(imageFileName);
      // If the title was not null and was one of the image files that we extracted before:
      if (imageFileName != null && imageUrlByName.containsKey(imageFileName)) {
        cntImages++;
        
        String text = FileLines.readBetween(in, "<text", "</text>");
        
        authorUser author = new authorUser();
        licenseReturn licenses = new licenseReturn();
        String permissionOTRS = null; 
        Boolean trademark = false;
        //String attribution = null;
        
        author = findAuthor(text.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " "));
        licenses = findLicense(text, imageFileName);
        permissionOTRS = findOTRSPermission(text);
        trademark = findTrademark(text);
        //attribution = findAttribution(text.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " "));
        
        String imageUrl = FactComponent.forYagoEntity(imageUrlByName.get(imageFileName));
        
        // Write available information:
        for (String licenseID:licenses.addedLicenses.keySet()) {
          String url = FactComponent.forYagoEntity(licenses.addedLicenses.get(licenseID));
          WIKIDATAIMAGELICENSE.write(new Fact(licenseID, YAGO.hasUrl, url));
        }

        if (licenses.imageLicenses.isEmpty()) {
          cntImagesNOLicense++;
        }
        
        for (String license:licenses.imageLicenses) {
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasLicense, FactComponent.forYagoEntity(license)));
        }
          
        if (author.name != null || author.url != null) {
          String authorID = FactComponent.forYagoEntity("author_" + (++authorCnt));
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasAuthor, authorID));
          if (author.name != null) {
            WIKIDATAIMAGELICENSE.write(new Fact(authorID, YAGO.hasName, FactComponent.forYagoEntity(author.name)));
          }
          if (author.url != null) {
            WIKIDATAIMAGELICENSE.write(new Fact(authorID, YAGO.hasUrl, FactComponent.forYagoEntity(author.url)));
          }
        }
        
        if (permissionOTRS != null) {
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasOTRSId, FactComponent.forYagoEntity(permissionOTRS)));
        }
          
        if (trademark) {
          WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasTrademark, FactComponent.forYagoEntity(trademark.toString())));
        }
        
        //if (attribution != null) {
          //WIKIDATAIMAGELICENSE.write(new Fact(imageUrl, YAGO.hasAttributionTag, attribution));
        //}
        // something else: attcc : http://commons.wikimedia.org/wiki/File:Ph_locator_camiguin_mambajao.png
      }
    }
    //System.out.println("#total: " + fileNameToUrl.size());
    //System.out.println("#images: " + cntImages);
    //System.out.println("#imagesWithNolicence: " + cntImagesNOLicense);
    in.close();
    
  }
  
  /**
   * Create a mapping from image file names to their image Wikipedia page for easy access.
   * 
   * @return A map of image file names to image Wikipedia Urls.
   * @throws IOException
   */
  private Map<String, String> getFileNames() throws IOException {
    Map<String, String> fileNameToUrl = new HashMap<>();

    // Load extracted images. Facts here will be: <yagoEntity> <hasImageID> <image_ID>
    Set<Fact> entityImages = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasImageID);
    
    for (Fact f:entityImages) {
      String imageID = f.getObject();
      String imageWikiPageUrl = FactComponent.stripBrackets(WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getObject(imageID, YAGO.hasWikiPage));
      String imageFileName = imageWikiPageUrl.substring(imageWikiPageUrl.indexOf("/wiki/File:") + "/wiki/File:".length()).replaceAll("_", " ");

      fileNameToUrl.put(imageFileName, imageWikiPageUrl);
    }
    return fileNameToUrl;
  }

  /**
   * Find if there exist an OTRS permission tag and return its ticket id.
   * 
   * @param text Image's Wikipedia page content.
   * @return Return OTRS id if it exists and null otherwise.
   */
  private String findOTRSPermission(String text) {
    Matcher matcher = OTRSPermissionPattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  
  /**
   * This function create facts for hard coded licenses such as:
   * <publicDomainLicense> <hasUrl> <https://en.wikipedia.org/wiki/Public_domain>
   * 
   * @throws IOException 
   */
  private static void writeHardcodedLicenses() throws IOException {
    for (String licenseKey:hardCodedLicenses.keySet()) { 
      Fact f = new Fact(FactComponent.forYagoEntity(licenseKey), YAGO.hasUrl, FactComponent.forYagoEntity(hardCodedLicenses.get(licenseKey)));
      WIKIDATAIMAGELICENSE.write(f);
    }
  }
  
//  /**
//   * Find if there exist an Attribution tag.
//   * TODO: what to to with the info after it like user, ... There is also a Attribution-CC....
//   * @param text Image's Wikipedia page content.
//   * @return
//   */
//  private String findAttribution(String text) {
//    Matcher matcher = attributionPattern.matcher(text);
//    if(matcher.find())
//      return matcher.group(1);
//    return null;
//  }
  
  /**
   * Find existing licenses from the defined licenses.
   * 
   * @param text Image's Wikipedia page content.
   * @return Return the found licenses for the image.
   */
  private static licenseReturn findLicense(String text, String imageFileName) {
    licenseReturn licenses = new licenseReturn();
    
    //TODO: In order to support more licenses, one has to add the pattern to "imageLicensePatterns" and then support it in the loop below.
    for (String key:imageLicensePatterns.keySet()) {
      Matcher licenseMatcher = imageLicensePatterns.get(key).matcher(text);
      while (licenseMatcher.find()) {
        // If the license is found was one of the hard coded licenses:
        if (hardCodedLicenses.containsKey(key)) {
          licenses.imageLicenses.add(key);
        } 
        // If it is GFDL, check if it has a specific version.
        else if (key.equals("GNUFreeDocumentationLicense")) {
          if(licenseMatcher.group(2) != null) {//version
            licenses.imageLicenses.add(key + licenseMatcher.group(2));
          }
          else {
            licenses.imageLicenses.add(key + "-1.3");
          }
        }
        // If it is GPL, check for versions.
        else if (key.equals("GNUGeneralPublicLicense")) {
          if (licenseMatcher.group(3) != null) {//specific version
            licenses.imageLicenses.add(key + "-" + licenseMatcher.group(3));
          }
          else {
            licenses.imageLicenses.add(key + "-v3");
          }
        }
        // If it is Creative Common and does not have a specific language (It is still in the hard coded licenses with different key):
        else if (key.equals("CreativeCommonsLicense") && licenseMatcher.group(3) == null) {
          if (licenseMatcher.group(2).equals("all")) {
            // Add all of the CC versions to the image licenses.
            for (String version:CC_VERSIONS) {
              licenses.imageLicenses.add(key + "-" + licenseMatcher.group(1).toUpperCase() + "-" + version);
            }
          }
          else {
            licenses.imageLicenses.add(key + "-" + licenseMatcher.group(1).toUpperCase() + "-" + licenseMatcher.group(2));
          }
        }
        // It is not in the hard coded licenses:
        else {
          String licenseID = FactComponent.forYagoEntity("license_" + (++licenseCnt));
          
          licenses.imageLicenses.add(licenseID);
          
          String url = null;
          switch(key) {
            case "CreativeCommonsLicense":
              if (licenseMatcher.group(2).equals("all")) {
                for (String version:CC_VERSIONS) {
                  url = hardCodedLicenses.get(key + "-" + licenseMatcher.group(1).toUpperCase() + "-" + version) + "/" + licenseMatcher.group(3);
                  licenses.addedLicenses.put(licenseID, url);
                }
              }
              else {
                url = hardCodedLicenses.get(key + "-" + licenseMatcher.group(1).toUpperCase() + "-" + licenseMatcher.group(2)) + "/" + licenseMatcher.group(3);
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
   * 
   * @param text Image's Wikipedia page content.
   * @return Return the author/creator of the image.
   */
  //TODO: multiple authors https://commons.wikimedia.org/wiki/File:N%C3%BCrnberger_Ringbahn.png
  private static authorUser findAuthor(String text) {
    
    authorUser author = new authorUser();
    Matcher matcher = authorFieldPattern.matcher(text);
    if (matcher.find()) {
      String authorFieldText = matcher.group(1);
      //TODO: In order to support, one has to add the pattern to "authorPatterns" and then support it in the loop below.
      for (String key:authorPatterns.keySet()) {
        Matcher authorTypeMatcher = authorPatterns.get(key).matcher(authorFieldText);
        if (authorTypeMatcher.find()) {
          switch(key) {
            
            case "Unknown":
              author.name = "Unknown";
              break;
              
            case "wikiLanguageUser":
              if (authorTypeMatcher.group(1) != null) { 
                author.url = "http://" + authorTypeMatcher.group(1) + "." + wikipediaUrl + authorTypeMatcher.group(2).replaceAll(" ", "_");
              }
              else { 
                author.url = wikimediaCommonUrl + authorTypeMatcher.group(2).replaceAll(" ", "_");
              }
              if (authorTypeMatcher.group(3) != null) {
                author.name = authorTypeMatcher.group(3);
              }
              break;
              
            case "wikiCommonUser":
              if(authorTypeMatcher.group(2).equals("creator") || authorTypeMatcher.group(2).equals("Creator")) {
                author.url = wikimediaCommonUrl + "Creator:" + authorTypeMatcher.group(4);
              }
              else {
                author.url = wikimediaCommonUrl + "User:" + authorTypeMatcher.group(4);
              }
              author.name = authorTypeMatcher.group(4);
              break;
              
            case "wikiGermanUser":
              author.url = "https://de." + wikipediaUrl + authorTypeMatcher.group(1);
              author.name = authorTypeMatcher.group(1);
              break;
              
            case "flickrUser":
              author.url = flickerUsersUrl + authorTypeMatcher.group(1);
              break;
              
            case "externalLink":
              author.url = authorTypeMatcher.group(1);
              if(authorTypeMatcher.group(2) != null) {
                author.name = authorTypeMatcher.group(2);
              }
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
   * Find if there exist a trademark tag.
   * 
   * @param text Image's Wikipedia page content.
   * @return Return whether there is a trademark tag. 
   */
  private static Boolean findTrademark(String text) {
    Matcher matcher = trademark.matcher(text);
    if (matcher.find()) {
      return true;
    }
    return false;
  }

}
