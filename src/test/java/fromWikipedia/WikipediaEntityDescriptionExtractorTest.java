package fromWikipedia;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Test;

import followUp.FollowUpExtractor;

/**
 * Test case for WikipediaEntityDescriptionExtractorTest.
 * We provided small wikipedia files for English and German languages and
 * test the extractor on these languages. As well as the followUp extractor for German language.
 * 
 * @author Ghazaleh Haratinezhad Torbati
 *
 */
public class WikipediaEntityDescriptionExtractorTest {
  
  private static final String RESOURCESPATH = "src/test/resources/" + WikipediaEntityDescriptionExtractor.class.getName();
  
  private WikipediaEntityDescriptionExtractor ex;
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
    ex = new WikipediaEntityDescriptionExtractor(language, new File(RESOURCESPATH + "/input/" + wikiFileName));

    // The first argument should be the folder containing the themes needed in the extractor.
    // I created small theme files as well as wikipedia files, in order to run the test quickly.
    // WikipediaEntityDescriptionExtractorTest needs following files: "_titlePatterns.tsv", "yagoPreferredMeanings.tsv" and "redirectLabelsNeedsTranslationTypeChecking.tsv"
    // I made small files for this test and save them in resources/output. The reason I put them in the output is that I need the created theme along with others for the
    // followUp extractor.
    // Output of the extract function (wikipediaEntityDescriptions_language.tsv) is written in the second second argument.
    ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing WikipediaEntityDescriptionExtractor in language: " + language);
    Set<FollowUpExtractor> followUps = ex.followUp();
    for(FollowUpExtractor fex:followUps)
      fex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing followUp WikipediaEntityDescriptionExtractor in language: " + language);
    compareOutputWithExpected(language);
  }
  
  
  public void compareOutputWithExpected(String language) throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikipediaEntityDescriptions_" + language + ".tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikipediaEntityDescriptions_" + language + ".tsv")));
    
    assertEquals(actual, expected);
    
    if(language == "en")  return;
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikipediaEntityDescriptionsNeedsTranslation_" + language + ".tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikipediaEntityDescriptionsNeedsTranslation_" + language + ".tsv")));
    
    assertEquals(actual, expected);
  }

}
