package fromWikipedia;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Test;

import followUp.FollowUpExtractor;

public class CategoryGlossExtractorTest {
  private static final String RESOURCESPATH = "src/test/resources/" + CategoryGlossExtractor.class.getName();
  
  private static CategoryGlossExtractor ex;
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
    ex = new CategoryGlossExtractor(language, new File(RESOURCESPATH + "/input/" + wikiFileName));
    
    // The first argument should be the folder containing the themes needed in the extractor.
    // I created small theme files as well as wikipedia files, in order to run the test quickly.
    // Output of the extract function (wikipediaCategoryGlosses and wikipediaCategoryGlossesNeedsTranslation) is written in the folder of the second argument.
    ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing CategoryExtraction in language: " + language);
    
    //Set<FollowUpExtractor> followUps = ex.followUp();
    //for(FollowUpExtractor fex:followUps)
      //fex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing followUp CategoryExtraction in language: " + language);
    compareOutputWithExpected(language);
  }

  public void compareOutputWithExpected(String language) throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikipediaCategoryGlosses_" + language + ".tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikipediaCategoryGlosses_" + language + ".tsv")));
    
    assertEquals(actual, expected);
  }
}
