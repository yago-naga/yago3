package fromWikipedia;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import followUp.FollowUpExtractor;

/**
 * Test case for {@link WikipediaEntityDescriptionExtractor}.
 * Small Wikipedia dumps were compiled for English and German and are tested with this extractor
 * For German, also the follow-up extractors are tested.
 */
public class WikipediaEntityDescriptionExtractorTest {
  
  private static final String RESOURCES_PATH = "src/test/resources/" + WikipediaEntityDescriptionExtractor.class.getName();
  
  private WikipediaEntityDescriptionExtractor ex;
  private static String enWikiFileName = "sample-enwiki-20170201-pages-articles.xml";
  private static String deWikiFileName = "sample-dewiki-20170201-pages-articles.xml";
  private static String zhWikiFileName = "sample-zhwiki-20170201-pages-articles.xml";
  
  

  @Test
  public void testExtractorZH() throws Exception {
    testExtractor("zh", zhWikiFileName);
  }
  
    @Test
  public void testExtractorEN() throws Exception {
    testExtractor("en", enWikiFileName);
  }
  
  @Test
  public void testExtractorDE() throws Exception {
    testExtractor("de", deWikiFileName);
  }

  /**
   * Tests the extractor by comparing the actual output with the expected output.
   * 
   * @param language The ISO 639-1 language code.
   * @param wikiFileName The file name of the sample Wikipedia dump.
   * @throws Exception
   */
  public void testExtractor(String language, String wikiFileName) throws Exception {
    ex = new WikipediaEntityDescriptionExtractor(language, new File(RESOURCES_PATH + "/input/" + wikiFileName));

    // These files need to be present for the extractor: _titlePatterns.tsv, yagoPreferredMeanings.tsv,
    // and redirectLabelsNeedsTranslationTypeChecking.tsv. All are put in the designated folder together, as
    // they are required for the follow-up extractors. 
    ex.extract(
        new File(RESOURCES_PATH + "/output"),
        new File(RESOURCES_PATH + "/output"),
        "testing WikipediaEntityDescriptionExtractor in language: " + language);
    
    // Run the follow-up extractors.
    for (FollowUpExtractor fex:ex.followUp()) {
      fex.extract(
          new File(RESOURCES_PATH + "/output"),
          new File(RESOURCES_PATH + "/output"),
          "testing followUp WikipediaEntityDescriptionExtractor in language: " + language);
    }
    
    compareActualWithExpectedOutput(language);
  }
  
  /**
   * Compares the actual output with the expected one while ignoring the very first line in the output file.
   * 
   * @param language The ISO 639-1 language code.
   * @throws IOException
   */
  public void compareActualWithExpectedOutput(String language) throws IOException{
    // Make sure that the language code is in lowercase.
    language = language.toLowerCase();
    
    // Compare the actual output with the expected one.
    List<String> actualLines = readAllLinesFromFileIgnoringFirstLine(Paths.get(RESOURCES_PATH + "/output/wikipediaEntityDescriptions_" + language + ".tsv"));
    List<String> expectedLines = readAllLinesFromFileIgnoringFirstLine(Paths.get(RESOURCES_PATH + "/output/expected_wikipediaEntityDescriptions_" + language + ".tsv"));
    
    assertTrue(expectedLines.equals(actualLines));
    
    // For all languages other than EN, we also need to check the "needs translation" output.
    if (!language.equals("en")) {
      actualLines = readAllLinesFromFileIgnoringFirstLine(Paths.get(RESOURCES_PATH + "/output/wikipediaEntityDescriptionsNeedTranslation_" + language + ".tsv"));
      expectedLines = readAllLinesFromFileIgnoringFirstLine(Paths.get(RESOURCES_PATH + "/output/expected_wikipediaEntityDescriptionsNeedTranslation_" + language + ".tsv"));
    
      assertTrue(expectedLines.equals(actualLines));
    }
  }
  
  /**
   * Reads all lines from a file but removes the very first line.
   * 
   * @param path Path to file.
   * @return List of lines that are read from the file.
   * @throws IOException
   */
  private List<String> readAllLinesFromFileIgnoringFirstLine(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path);
    lines.remove(0);
    
    return lines;    
  }

}
