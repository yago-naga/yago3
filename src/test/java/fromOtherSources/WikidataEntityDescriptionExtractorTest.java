package fromOtherSources;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

import extractors.MultilingualExtractor;

/**
 * Test case for WikidataEntityDescriptionExtractorTest.
 * Using a small sample wikidata-terms as input.
 * 
 * @author Ghazaleh Haratinezhad Torbati
 *
 */
public class WikidataEntityDescriptionExtractorTest {
  private static final String RESOURCESPATH = "src/test/resources/" + WikidataEntityDescriptionExtractor.class.getName();
  private WikidataEntityDescriptionExtractor ex;
  
  @Test
  public void extractorTest() throws Exception {
     MultilingualExtractor.wikipediaLanguages = Arrays.asList("en", "de");
     ex = new WikidataEntityDescriptionExtractor(new File(RESOURCESPATH + "/input/sample-wikidata-20170220-all-BETA.ttl"));
     
     // The first argument should be the folder containing the themes needed in the extractor.
     // WikidataEntityDescriptionExtractorTest needs "wikidataInstances.tsv" as input.
     // I made small sample file for this matter.
     // Output of the extract function (wikidataEntityDescriptions.tsv) is written in the second second argument.
     ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing WikidataEntityDescriptionExtractorTest");
     compareOutputWithExpected();
  }
  
  public void compareOutputWithExpected() throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataEntityDescriptions.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataEntityDescriptions.tsv")));
    
    assertEquals(expected, actual);
  }
}
