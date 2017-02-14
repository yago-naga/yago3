package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import fromOtherSources.WikidataImageLicenseExtractor;

import static org.junit.Assert.*;

/**
 * Test case for CategoryExtractor.
 * We provided small wikipedia files for English and German languages and
 * test the extractor on these languages.
 * 1- To check if correct and sufficient categories are extracted.
 * 2- To check that redirect pages are not extracted.
 * 3- To check for English language, the words in wordnet are not extracted (not name entities).
 * 
 * @author Ghazaleh Haratinezhad Torbati
 *
 */
public class CategoryExtractorTest {
  
  private static final String RESOURCESPATH = "src/test/resources/" + CategoryExtractor.class.getName() + "2";
  private static CategoryExtractor ex;
  private static String enWikiFileName = "sample-enwiki-20170201-pages-articles.xml";
  private static String deWikiFileName = "sample-dewiki-20170201-pages-articles.xml";
  
    
  @Test
  public void testExtractorEn() throws Exception {
    testExtractor("en", enWikiFileName);
  }
  
  @Test
  public void testExtractorDe() throws Exception {
    testExtractor("de", deWikiFileName);
  }
  
  public void testExtractor(String language, String wikiFileName) throws Exception {
    ex = new CategoryExtractor(language, new File(RESOURCESPATH + "/input/" + wikiFileName));
    
    // The first argument should be the folder containing the themes needed in the extractor.
    // I created small theme files as well as wikipedia files, in order to run the test quickly.
    // Output of the extract function (CategoryMembers_language.tsv) is written in the second second argument.
    ex.extract(new File(RESOURCESPATH + "/input"), new File(RESOURCESPATH + "/output"),"testing CategoryExtraction in language: " + language);
    
    compareOutputWithExpected(language);
  }
  
  
  public void compareOutputWithExpected(String language) throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/categoryMembers_" + language + ".tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_categoryMembers_" + language + ".tsv")));
    
    assertEquals(actual, expected);
  }

}

