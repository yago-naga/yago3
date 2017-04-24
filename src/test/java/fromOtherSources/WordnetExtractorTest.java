package fromOtherSources;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing Wordnet Extractor.
 * 
 * @author Ghazaleh Haratinezhad Torbati
 *
 */
public class WordnetExtractorTest {
  
  private static final String RESOURCESPATH = "src/test/resources/" + WordnetExtractor.class.getName();
  private WordnetExtractor ex;
  
  @Before
  public void testWordnetExtractor() throws Exception {
    ex = new WordnetExtractor(new File(RESOURCESPATH + "/input/"));
    
    ex.extract(new File(RESOURCESPATH + "/output"), new File(RESOURCESPATH + "/output"),"testing WordnetExtractorTest");
  }

  @Test
  public void compareWordnetClassesOutput() throws IOException{
    String actual   = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wordnetClasses.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wordnetClasses.tsv")));
    assertEquals(expected, actual);
  }
  
  @Test
  public void compareWordnetGlossesOutput() throws IOException{
    String actual = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wordnetGlosses.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wordnetGlosses.tsv")));
    assertEquals(expected, actual);
  }
  
  @Test
  public void compareWordnetWordsOutput() throws IOException{
    String actual = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/wordnetWords.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_wordnetWords.tsv")));
    assertEquals(expected, actual);
  }
  
  @Test
  public void compareYagoPreferredMeaningOutput() throws IOException{
    String actual = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/yagoPreferredMeanings.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_yagoPreferredMeanings.tsv")));
    assertEquals(expected, actual);
  }
  
  @Test
  public void compareYagoWordnetIdsOutput() throws IOException{
    String actual = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/yagoWordnetIds.tsv")));
    String expected = new String(Files.readAllBytes(Paths.get(RESOURCESPATH + "/output/expected_yagoWordnetIds.tsv")));
    assertEquals(expected, actual);
  }

}
