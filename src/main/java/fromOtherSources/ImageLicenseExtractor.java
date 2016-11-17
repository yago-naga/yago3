package fromOtherSources;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.Extractor;
import javatools.datatypes.FinalSet;
import utils.Theme;

public class ImageLicenseExtractor extends Extractor{
  
  //TODO: Can only keep 1 license in the theme ?
  public static final Theme WIKIDATAIMAGELICENSE = new Theme("wikidataImageLicenses", 
      "Licences extracted for wikidata Images");
  
  private static Pattern whiteSpacePattern = Pattern.compile("^[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+$");
  
  private static final Pattern userPattern = Pattern.compile("href=\"((?:http://|https://)?(?:(?:.{2,3}\\.)?wikipedia\\.org/wiki/(?:User:)?|commons\\.wikimedia\\.org/wiki/(?:User:)?|/wiki/User:)[^\"]+)\"");
  private static final String usersCommonUrl = "https://commons.wikimedia.org";
  private static final String trademarkUrl = "https://en.wikipedia.org/wiki/Trademark";
  
  
  
//TODO: add more licenses such as: (OTRS in http://commons.wikimedia.org/wiki/File:Marguerite-Marie_Dubois_en_1965.jpg)
  private static final Map<String, Pattern> imageLicences;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    // Different licenses:
    tempMap.put("creativeCommons", Pattern.compile("creativecommons\\.org/licenses/(.*?)/(\\d+\\.\\d+)(/(?!deed\\.)[^ \"<]{2,3}/?)?"));
    
    tempMap.put("GNUFreeDocumentationLicense", Pattern.compile("GNU Free Documentation License.*[Vv]ersion\\s+(\\d+)(\\.\\d+)?"));
    tempMap.put("GNUGeneralPublicLicense", Pattern.compile("GNU General Public License.*[Vv]ersion\\s+(\\d+)(\\.\\d+)?"));
    tempMap.put("GNULesserGeneralPublicLicense", Pattern.compile("GNU Lesser General Public License.*[Vv]ersion\\s+(\\d+)(\\.\\d+)?"));
    tempMap.put("GNUAfferoGeneralPublicLicense", Pattern.compile("GNU Affero General Public License.*[Vv]ersion\\s+(\\d+)(\\.\\d+)?"));
    
    tempMap.put("FreeArtLicense", Pattern.compile("artlibre.org/licence/"));
    
    tempMap.put("OpenGovernmentLicence", Pattern.compile("nationalarchives.gov.uk/doc/open-government-licence/version/(\\d+)"));
    
    tempMap.put("OpenDataCommons", Pattern.compile("(Open Data Commons|opendatacommons\\.org)"));
    tempMap.put("OpenStreetMap", Pattern.compile("(OpenStreetMap|openstreetmap\\.org)"));

    // Permissions:
    tempMap.put("publicDomain", Pattern.compile("([Pp]ublic [Dd]omain)"));    
    
    imageLicences = Collections.unmodifiableMap(tempMap);
  }
  
  private static final Map<String, String> licenseURLs;
  static {
    Map<String, String> tempMap = new HashMap<String, String>();
    tempMap.put("creativeCommons", "https://creativecommons.org/licenses/");
    tempMap.put("GNUFreeDocumentationLicense", "https://www.gnu.org/licenses/fdl-");
    tempMap.put("GNUFreeDocumentationLicense", "https://www.gnu.org/licenses/gpl-");
    tempMap.put("GNUFreeDocumentationLicense", "https://www.gnu.org/licenses/lgpl-");
    tempMap.put("GNUAfferoGeneralPublicLicense", "https://www.gnu.org/licenses/agpl-");
    tempMap.put("OpenGovernmentLicence", "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/");
    tempMap.put("FreeArtLicense", "http://artlibre.org/licence/lal/en/");
    tempMap.put("publicDomain", "https://en.wikipedia.org/wiki/Public_domain");
    tempMap.put("OpenDataCommons", "http://opendatacommons.org/licenses/odbl/");
    tempMap.put("OpenStreetMap", "http://www.openstreetmap.org/copyright");
    licenseURLs = Collections.unmodifiableMap(tempMap);
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
    String gh = "https://commons.wikimedia.org/w/index.php?title=File:%22Ode_to_the_Sea%22_-_A_complete_outfit.jpg&action=edit";
    Connection connect = Jsoup.connect(gh);
    Document doc = connect.get();
    System.out.println(doc.getElementById("wpTextbox1").toString());
    System.exit(1);
    
    
    Set<Fact> wikidataEntityImage = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasWikiDataImageUrl);
    
    for(Fact fact : wikidataEntityImage){
      String url = FactComponent.stripBrackets(fact.getObject());
      //System.out.println(url);
      try {
        String uri = URI.create(url).toASCIIString();
        extractImageLicense(uri, FactComponent.forUri(url));
      }
      catch (Exception e) {
        extractImageLicense(url, FactComponent.forUri(url));
      }
    }
  }

  // This function extract licenses in image webpage.
  // By checking permission row in summary of the image and
  // the rest of the text in "mw-imagepage-content"
  public static Boolean extractImageLicense(String imageUrl, String printUrl) {
    try {
      Set<String> licenses = new HashSet<String>();
      Boolean trademark = false;
      String author = null;
      
      Connection connect = Jsoup.connect(imageUrl);
      Document doc = connect.get();
      //Document doc = connect.get();
      Element imageContent = doc.getElementById("mw-imagepage-content");
      if (imageContent != null) {
        Elements children = imageContent.children();
        for(Element child:children){
          //Summary table is in this div:
          if (child.tagName().equals("div") && child.className().equals("hproduct commons-file-information-table")){
            // Find the Author:
            author = findAuthor(child.getElementById("fileinfotpl_aut"));

            // Find the Permission:
            Element permission = child.getElementById("fileinfotpl_perm");
            if (permission != null) {
              Elements siblings = permission.siblingElements();
              for(Element sibling:siblings) {
                String text = sibling.toString().replaceAll("[\\s\\x00-\\x1F]+", " ");
                trademark = findTrademark(text);
                licenses.addAll(matchLicense(text, printUrl));
              }
            }
            
          }
          //Other text in content:
          else {
            String text = child.toString().replaceAll("[\\s\\x00-\\x1F]+", " ");
            trademark = findTrademark(text);
            licenses.addAll(matchLicense(text, printUrl));
          }
        }        
      }
      else {
        System.out.println("No 'mw-imagepage-content' for " + imageUrl);
      }
      
      if (author != null)
        WIKIDATAIMAGELICENSE.write(new Fact(printUrl, YAGO.hasAuthor, author));
      else {
        System.out.println("No author found for: " + imageUrl);
      }
      
      if (trademark)
        WIKIDATAIMAGELICENSE.write(new Fact(printUrl, YAGO.hasTrademark, trademarkUrl));
      
      if(licenses.size() == 0) {
        System.out.println("No license found in defined licenses for: " + imageUrl);
      }
      // write theme here:
      else {
        for(String license:licenses) {
          WIKIDATAIMAGELICENSE.write(new Fact(printUrl, YAGO.hasLicense, license));
        }
      }
      
    }
    catch (IOException e) {
      System.out.println("Error: Connection problem for: " + imageUrl);
    }
    return true;
  }
  
  // This function find text matches with license patterns.
  // And return uri to the found license.
  public static Set<String> matchLicense(String text, String url) {
    Set<String> licenses = new HashSet<String>();
    for(String licenceName : imageLicences.keySet()) {
      Pattern licensePattern = imageLicences.get(licenceName);
      Matcher matcher = licensePattern.matcher(text);
      while(matcher.find()) {
        if(licenceName.equals("creativeCommons"))
           licenses.add(licenseURLs.get(licenceName) + matcher.group(1) + "/" + matcher.group(2) + ((matcher.group(3) != null) ?  matcher.group(3) : "") );
        else if (licenceName.equals("GNUFreeDocumentationLicense")   ||  licenceName.equals("GNUGeneralPublicLicense") ||
                 licenceName.equals("GNULesserGeneralPublicLicense") ||  licenceName.equals("GNUAfferoGeneralPublicLicense"))
          licenses.add(licenseURLs.get(licenceName) + matcher.group(1) + ((matcher.group(2) != null) ? matcher.group(2) : ".0"));
        else if (licenceName.equals("OpenGovernmentLicence"))
          licenses.add(licenseURLs.get(licenceName) + matcher.group(1));
        else
          licenses.add(licenseURLs.get(licenceName));
      }
    }
    return licenses;
  }
  
  public static String findAuthor(Element authorField) {
    String author = null;
    if (authorField != null) {
      Element sibling = authorField.nextElementSibling();
      String text = sibling.toString().replaceAll("[\\s\\x00-\\x1F]+", " ");
      Matcher matcher = userPattern.matcher(text);
      if(matcher.find()) {
        if(matcher.group(1).contains("wikipedia.org") || matcher.group(1).contains("wikimedia.org"))
          author = matcher.group(1);
        else
          author = usersCommonUrl + matcher.group(1);
      }
      else {
        author = text.replaceAll("<.*?>", "");
        if (whiteSpacePattern.matcher(author).find())
          author = null;
      }
    }
    return author;
  }
  
  // TODO: pattern is enough?
  public static Boolean findTrademark(String text) {
    if (text.contains("en.wikipedia.org/wiki/en:trademark"))
      return true;
    return false;
  }
  
  public String extractAttributionTag(String text) {
    if (text.contains("<span class=\"licensetpl_short\" style=\"display:none;\">Attribution</span>") || text.contains("alt=\"Trademarked\"")){
      return "yes";
    }
    return null;
  }

}

