package test.fromWikipedia;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Test case for {@link RedirectExtractor}.
 * Small Wikipedia dumps were compiled for English and German and are tested with this extractor.
 */
public class RedirectExtractorTest {
  
  private static final String RESOURCESPATH = "src/test/resources/" + RedirectExtractor.class.getName() + "2";
  private static RedirectExtractor ex;
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
    ex = new RedirectExtractor(language, new File(RESOURCESPATH + "/input/" + wikiFileName));
    
    ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing RedirectExtractor in language: " + language);
    
    compareOutputWithExpected(language);
  }
  
  
  public void compareOutputWithExpected(String language) throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/redirectLabelsNeedsTranslationTypeChecking_" + language + ".tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_redirectLabelsNeedsTranslationTypeChecking_" + language + ".tsv")));
    
    assertEquals(expected, actual);
  }
  
  

}
