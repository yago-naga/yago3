package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import basics.Fact;
import basics.YAGO;

import static org.junit.Assert.*;

//TODO: continue to test for author trademark otrs
/**
 * Test cases for WikidataImageLicenseExtractor
 * @author Ghazaleh Haratinezhad Torbati
 * 
 */

public class WikidataImageLicenseExtractorTest {

  private static final String RESOURCESPATH = "src/test/resources/" + WikidataImageLicenseExtractor.class.getName();
  static WikidataImageLicenseExtractor ex;

  // make small file for images
  // check for each license if it is extracted for the expected ones
  
  @BeforeClass
  public static void runExtractor() throws Exception {
    
    ex = new WikidataImageLicenseExtractor(new File(RESOURCESPATH + "/input/sample-commonswiki-20170201-pages-articles.xml"));
    
    // The first argument should be the folder containing the themes needed in the extractor.
    // WikidataImageLicenseExtractor needs "wikidataImages.tsv" as input.
    // I made small sample file for this matter.
    // Output of the extract function (wikidataImagesInformation.tsv) is written in the second second argument.
    ex.extract(new File(RESOURCESPATH + "/input"), new File(RESOURCESPATH + "/output"),"testing WikidataImageLicenseExtractor"); 
  }
  
  @Test
  public void testPublicDomainLicense() throws IOException {
    String licenseName = "<PublicDomainLicense>";
    String licenseUrl = "<https://en.wikipedia.org/wiki/Public_domain>";
    List<String> imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Francisco_Goya_-_Casa_de_locos.jpg>",
                                           "<http://commons.wikimedia.org/wiki/File:ETH_Zürich_wordmark.svg>");
    
    checkLicense(licenseUrl, licenseName, imageUrls);
  }
  
  @Test
  public void testGNULicenses() throws IOException {
    String licenseName = "<GNUFreeDocumentationLicense>";
    String licenseUrl = "<https://www.gnu.org/licenses/gfdl>";
    List<String> imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Kenwood_House.jpg>",
                                           "<http://commons.wikimedia.org/wiki/File:Lighthouse,_Hirtshals,_Denmark.jpeg>",
                                           "<http://commons.wikimedia.org/wiki/File:Xscreensaver_xmatrix.png>",
                                           "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<GNUGeneralPublicLicense>";
    licenseUrl = "<https://www.gnu.org/licenses/gpl>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Xscreensaver_xmatrix.png>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<GNULesserGeneralPublicLicense>";
    licenseUrl = "<https://www.gnu.org/licenses/lgpl>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Noia_64_apps_cervisia.png>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<GNUAfferoGeneralPublicLicense>";
    licenseUrl = "<https://www.gnu.org/licenses/agpl>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Phpfusionscreenshot.png>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
  }
  
  @Test
  public void testOpenGovernmentLicences() throws IOException {
    String licenseUrl =  "<https://www.nationalarchives.gov.uk/doc/open-government-licence/version/1>";
    String licenseId = "<license_2>";
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains(licenseId, YAGO.hasUrl, licenseUrl));
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains("<http://commons.wikimedia.org/wiki/File:Nemat_Shafik_Portrait.jpg>", YAGO.hasLicense, licenseId));

    licenseUrl =  "<https://www.nationalarchives.gov.uk/doc/open-government-licence/version/2>";
    licenseId = "<license_3>";
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains(licenseId, YAGO.hasUrl, licenseUrl));
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains("<http://commons.wikimedia.org/wiki/File:Suresh_Premachandran.jpg>", YAGO.hasLicense, licenseId));

  }
  
  @Test
  public void testOpenDataCommonsLicense() throws IOException {
    String licenseName = "<OpenDataCommonsLicense>";
    String licenseUrl = "<http://opendatacommons.org/licenses/odbl/>";
    List<String> imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Nürnberger_Ringbahn.png>");
    checkLicense(licenseUrl, licenseName, imageUrls);
  }
  
  @Test
  public void testFreeArtLicense() throws IOException {
    String licenseName = "<FreeArtLicense>";
    String licenseUrl = "<http://artlibre.org/licence/lal/en/>";
    List<String> imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
  }
  

  @Test
  public void testCreativeCommonsLicenses() throws IOException {
    String licenseName = "<CreativeCommonsLicense-BY-NC-SA-2.0>";
    String licenseUrl = "<https://creativecommons.org/licenses/by-nc-sa/2.0>";
    List<String> imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Arthur_Goldstuck.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-NC-ND-2.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-nc-nd/2.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Hans_Sünkel.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-NC-2.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-nc/2.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Napoli-capodimonte-royalpalace.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-SA-1.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-sa/1.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>",
                              "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-SA-2.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-sa/2.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>",
                              "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>",
                              "<http://commons.wikimedia.org/wiki/File:Kenwood_House.jpg>",
                              "<http://commons.wikimedia.org/wiki/File:Hans_Sünkel.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-SA-2.5>";
    licenseUrl = "<https://creativecommons.org/licenses/by-sa/2.5>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>",
                              "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-SA-3.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-sa/3.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>",
                              "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-SA-4.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by-sa/4.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Aztec_Empire_(orthographic_projection).svg>",
                              "<http://commons.wikimedia.org/wiki/File:Belarus-Mir-Castle-9.jpg>",
                              "<http://commons.wikimedia.org/wiki/File:Nürnberger_Ringbahn.png>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    
    licenseName = "<CreativeCommonsLicense-BY-1.0>";
    licenseUrl = "<https://creativecommons.org/licenses/by/1.0>";
    imageUrls = Arrays.asList("<http://commons.wikimedia.org/wiki/File:Groninger-museum.jpg>");
    checkLicense(licenseUrl, licenseName, imageUrls);
    

    licenseUrl =  "<https://creativecommons.org/licenses/by/2.5/pl>";
    String licenseId = "<license_1>";
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains(licenseId, YAGO.hasUrl, licenseUrl));
    assertTrue(WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().contains("<http://commons.wikimedia.org/wiki/File:Lighthouse,_Hirtshals,_Denmark.jpeg>", YAGO.hasLicense, licenseId));
    
  }
  
  
  
  private void checkLicense(String licenseUrl, String licenseName, List<String> imageUrls) throws IOException {
    assertEquals(licenseUrl, WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getObject(licenseName, YAGO.hasUrl));
    for(String url:imageUrls) {
      Set<Fact> facts = WikidataImageLicenseExtractor.WIKIDATAIMAGELICENSE.factCollection().getFactsWithSubjectAndRelation(url, YAGO.hasLicense);
      assertTrue(facts.contains(new Fact(url, YAGO.hasLicense, licenseName)));
    }
  }
}
