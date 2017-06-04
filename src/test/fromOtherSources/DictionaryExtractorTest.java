package test.fromOtherSources;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

import extractors.MultilingualExtractor;

/**
 * Test cases for DictionaryExtractor
 * @author Ghazaleh Haratinezhad Torbati
 *
 */

public class DictionaryExtractorTest {

  private static final String RESOURCESPATH = "src/test/resources/" + DictionaryExtractor.class.getName() + "2";
  private DictionaryExtractor ex;
  
  @Test
  public void extractorTest() throws Exception {
     MultilingualExtractor.wikipediaLanguages = Arrays.asList("en", "de");
     ex = new DictionaryExtractor(new File(RESOURCESPATH + "/input/sample-wikidata-20170220-all-BETA.ttl"));
     
     // The first argument should be the folder containing the themes needed in the extractor.
     // DictionaryExtractor needs "_titlePatterns.tsv" and "yagoPreferredMeanings.tsv" as input.
     // I made small sample file for this matter.
     // Outputs of the extract function are written in the second second argument.
     ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing DictionaryExtractor");
     compareOutputsWithExpectedFiles();
  }
  
  public void compareOutputsWithExpectedFiles() throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/entityDictionary_de.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_entityDictionary_de.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/categoryDictionary_de.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_categoryDictionary_de.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/categoryWords.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_categoryWords.tsv")));
    
    assertEquals(expected, actual);
    
    actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/infoboxTemplateDictionary_de.tsv")));
    expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_infoboxTemplateDictionary_de.tsv")));
    
    assertEquals(expected, actual);
  }
  
  
}
