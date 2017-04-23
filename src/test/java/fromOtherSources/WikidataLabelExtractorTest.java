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
 * Test cases to check WikidataLabelExtractor.
 * This extractor extract multilingual labels from wikidata.
 * @author Ghazaleh Haratinezhad Torbati
 *
 */

public class WikidataLabelExtractorTest {
  
  private static final String RESOURCESPATH = "src/test/resources/" + WikidataLabelExtractor.class.getName();
  private WikidataLabelExtractor ex;
  
  @Test
  public void extractorTest() throws Exception {
     MultilingualExtractor.wikipediaLanguages = Arrays.asList("en", "de");
     ex = new WikidataLabelExtractor(new File(RESOURCESPATH + "/input/sample-wikidata-20170220-all-BETA.ttl"));
     
     // The first argument should be the folder containing the themes needed in the extractor.
     // I made small sample file for this matter.
     // Outputs of the extract function are written in the second second argument.
     ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing WikidataLabelExtractor");
     compareOutputsWithExpectedFiles();
  }
  
  public void compareOutputsWithExpectedFiles() throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikipediaLabels.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikipediaLabels.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikipediaLabelSources.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikipediaLabelSources.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataInstances.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataInstances.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataMultiLabels.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataMultiLabels.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataMultiLabelSources.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataMultiLabelSources.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wikidataTranslations.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wikidataTranslations.tsv")));
    
    assertEquals(expected, actual);
  }
}
