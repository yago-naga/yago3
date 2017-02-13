package fromOtherSources;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import extractors.MultilingualExtractor;

import static org.junit.Assert.*;

/**
 * Test cases for WikidataImageExtractor
 * @author Ghazaleh Haratinezhad Torbati
 *
 */

public class WikidataImageExtractorTest {

  public static final String RESOURCESPATH = "src/test/resources/" + WikidataImageExtractor.class.getName();
  
  /**
   * Test getOriginalImageUrl which should change the image's wiki page url to it's original url.
   * @throws NoSuchAlgorithmException
   */
  @Test
  public void testGetOriginalImageUrl() throws NoSuchAlgorithmException {
    
    List <String> inputImageWikiUrls = Arrays.asList(
        "http://commons.wikimedia.org/wiki/File:Breviarium_Grimani_-_November.jpg",
        "http://commons.wikimedia.org/wiki/File:Flag_of_Moldova.svg",
        "http://commons.wikimedia.org/wiki/File:POL_Kurów_COA.svg",
        "http://commons.wikimedia.org/wiki/File:Champs-Élysées_from_the_Arc_de_Triomphe.jpg",
        "http://commons.wikimedia.org/wiki/File:Corallushortulanus.png",
        "http://commons.wikimedia.org/wiki/File:Aurélien_Clerc.jpg",
        "http://commons.wikimedia.org/wiki/File:Nyiragongo_1994.jpg",
        "http://commons.wikimedia.org/wiki/File:Torre_Belém_April_2009-4a.jpg",
        "http://commons.wikimedia.org/wiki/File:De_fem_søstre_i_Århus_final_version.jpg",
        "http://commons.wikimedia.org/wiki/File:Network_of_the_Presidents_of_the_Supreme_Judicial_Courts_of_the_European_Union_Logo.png");
    
    List<String> outputOriginalUrls = Arrays.asList(
        "https://upload.wikimedia.org/wikipedia/commons/3/35/Breviarium_Grimani_-_November.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/2/27/Flag_of_Moldova.svg",
        "https://upload.wikimedia.org/wikipedia/commons/d/d4/POL_Kurów_COA.svg",
        "https://upload.wikimedia.org/wikipedia/commons/9/95/Champs-Élysées_from_the_Arc_de_Triomphe.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/2/2b/Corallushortulanus.png",
        "https://upload.wikimedia.org/wikipedia/commons/f/f7/Aurélien_Clerc.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/6/68/Nyiragongo_1994.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/6/65/Torre_Belém_April_2009-4a.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/2/23/De_fem_søstre_i_Århus_final_version.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/2/2e/Network_of_the_Presidents_of_the_Supreme_Judicial_Courts_of_the_European_Union_Logo.png");
    
    List<String> testOutputOriginalUrls = new LinkedList<>();    
    
    for(String url:inputImageWikiUrls) {
      testOutputOriginalUrls.add(WikidataImageExtractor.getOriginalImageUrl(url));
    }
    
    assertEquals(outputOriginalUrls.size(), testOutputOriginalUrls.size());
    assertArrayEquals(outputOriginalUrls.toArray(), testOutputOriginalUrls.toArray());
    
  }
  

  @Before
  public void setup() {
    // Set the languages to de and en:
    MultilingualExtractor.wikipediaLanguages = Arrays.asList("en", "de");
  }
  
  /**
   * Testing the image extractor. It should extract one (for now) image from the sample file for each entity.
   * @throws Exception
   */
  @Test
  public void testImageExtractor() throws Exception {
    // Test Extraction on small input sample-wikidata-statements.nt
    WikidataImageExtractor ex = new WikidataImageExtractor(new File(RESOURCESPATH + "/input/sample-wikidata-statements.nt"));
    

    // The first argument should be the folder containing the themes needed in the extractor.
    // WikidataImageExtractor needs following files: "wikidataInstances.tsv" and "yagoTransitiveType.tsv"
    // I made small files for this test and save them in resources. But original files are also usable.
    // Output of the extract function (wikidataImages.tsv) is written in the second second argument.
    ex.extract(new File(RESOURCESPATH + "/input"), new File(RESOURCESPATH + "/output"),"testing WikidataImageExtractor");
    
    // Compare output file with the expected file
    compareOutputWithExpected();
  }
  
  public void compareOutputWithExpected() throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataImages.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataImages.tsv")));
    
    assertEquals(actual, expected);
  }
  

}
