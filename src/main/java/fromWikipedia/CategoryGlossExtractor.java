/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Ghazaleh Haratinezhad Torbati.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualWikipediaExtractor;
import followUp.CategoryTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.DictionaryExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;
import utils.WikipediaTextCleanerHelper;

/** Extracts category glosses from Wikipedia
 * 
*/
public class CategoryGlossExtractor extends MultilingualWikipediaExtractor {

  public static final MultilingualTheme CATEGORYGLOSSES = new MultilingualTheme("wikipediaCategoryGlosses", 
      "Category glosses extracted from wikipedia");
  
  public static final MultilingualTheme CATEGORYGLOSSESNEEDSTRANSLATION = new MultilingualTheme("wikipediaCategoryGlossesNeedsTranslation", 
      "Category glosses extracted from wikipedia");

  private static String categoryWord = null;
  
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  
  public CategoryGlossExtractor(String language, File wikipedia) {
    super(language, wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(DictionaryExtractor.CATEGORYWORDS));
  }

  @Override
  public Set<Theme> output() {
    if (isEnglish()) {
      return (new FinalSet<>(CATEGORYGLOSSES.inLanguage(language)));
    }
    else {
      return (new FinalSet<>(CATEGORYGLOSSESNEEDSTRANSLATION.inLanguage(language)));
    }
  }


  @Override
  public Set<FollowUpExtractor> followUp() {
    if (isEnglish()) {
      return (Collections.emptySet());
    }
    return (new FinalSet<FollowUpExtractor>(
        new CategoryTranslator(CATEGORYGLOSSESNEEDSTRANSLATION.inLanguage(this.language), CATEGORYGLOSSES.inLanguage(this.language), this, true, true)));
  }
  
  
  @Override
	public void extract() throws Exception {
    categoryWord = DictionaryExtractor.CATEGORYWORDS.factCollection().getObject(FactComponent.forString(language), "<_hasCategoryWord>");
    categoryWord = FactComponent.stripQuotes(categoryWord);
    
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		
		// Find pages about categories. example in English: <title>Category:Baroque_composers<\title>
		while(FileLines.findIgnoreCase(in, "<title>" + categoryWord + ":", "<title>" + "Category:") != -1) {
			String title = FileLines.readToBoundary(in, "</title>");
			title = Char17.decodeAmpersand(title);
			if (title != null) {
				String category = FactComponent.forForeignWikiCategory(FactComponent.stripBrackets(title), language);
				String page = FileLines.readBetween(in, "<text", "</text>");
				String gloss;
				// Due to not clean Wikipedia dumps, we use a time out to avoid getting into infinite loops.
				try {
				  gloss = executor.submit(new DescriptionExtractorCallable(page)).get(1, TimeUnit.SECONDS);
				}
				catch (InterruptedException | ExecutionException | TimeoutException e) {
	        gloss = null;
	      }
				
        if (gloss != null) {
          if(isEnglish()) {
            CATEGORYGLOSSES.inLanguage(language).write(new Fact(category, YAGO.hasGloss, FactComponent.forString(gloss)));
          }
          else {
            CATEGORYGLOSSESNEEDSTRANSLATION.inLanguage(language).write(new Fact(category, YAGO.hasGloss, FactComponent.forString(gloss)));
          }
            
        }
			}
		}
		in.close();
	}


  /**
   * Cleaning the text to human readable.
   * 
   * @param inputText The description for the entity that needs cleaning.
   * @return Clean description text.
   */
  private static String cleanText(String inputText){
  
  //Replacing internal wikipedia links with their text.
  inputText = WikipediaTextCleanerHelper.internalLinks.transform(inputText);
  
  inputText = inputText.replaceAll("\\*", "");
  inputText = inputText.replaceAll( ":{2,}", "");
  inputText = inputText.replaceAll("'{2,}", "");
  inputText = inputText.replaceAll("\\d+px", "");

  // Remove whitespace before the punctuations:
  inputText = inputText.replaceAll("\\s+([\\.!;,\\?])", "$1");
  
  // Remove Urls:
  inputText = inputText.replaceAll("\\[(http|https)://[^\\p{Zl}\\p{Zs}\\p{Zp}]+[\\p{Zl}\\p{Zs}\\p{Zp}](.*?)\\]", "$2");
  inputText = inputText.replaceAll("[\\[\\]]", "");
  
  // Remove everything in parenthesis:
  inputText = WikipediaTextCleanerHelper.removeParentheses(inputText);
  
  // Remove punctuations from the beginning of the gloss.
  inputText = inputText.replaceAll("^[\\.!;:,\\?]+", "");
  
  inputText = inputText.replaceAll("&nbsp;", " ");
  inputText = inputText.replaceAll("\\u0022", "\"");
  inputText = inputText.replaceAll("•", "");
  
  // Remove extra whites paces.
  inputText = WikipediaTextCleanerHelper.whiteSpaces.transform(inputText);
  
  
  if (inputText.matches("^\\s*(<|&lt;).*")) 
    return null;

  
  if(inputText.length() < WikipediaTextCleanerHelper.MIN_TEXT_LENGTH)
    return null;
  
  return inputText;
}

/**
 * Extracting description text by removing patterns to make the text human readable.
 * 
 * @param inputText Wikipedia page content from which the patterns are removed.
 * @return The page with the pattern removed.
 */
private static String removePatterns(String inputText) {
  // Remove links to files and images.
  inputText = WikipediaTextCleanerHelper.removeUselessLinks(inputText);
  // Remove everything in curly brackets.
  inputText = WikipediaTextCleanerHelper.removeBrackets(inputText);
  
  // Remove some language specific texts such as: see also:...
  inputText = inputText.replaceAll("(([Ss]ee [Aa]lso.*?)|(Note:)|([Ff]or more.*?)|([Ff]or specific.*?)|([Ss]ee [Tt]he)|([Ss]ee:)|(For .+?[,-] see)|([Cc]lick [Oo]n))(.*)", "");
  inputText = inputText.replaceAll("(([Ss]iehe [Aa]uch)|(Hinweis:))(.*)", "");
//This text appeared in some wikipedia articles, and has no information. Remove it.
  inputText = inputText.replaceAll("The (.*?)magic word(.*?) <nowiki>__NOGALLERY__</nowiki> is used in this category to turn off thumbnail display since this category list unfree images, the display of which is restricted to certain areas of Wikipedia.", "");
  
  // Remove HTML tags.
  inputText = WikipediaTextCleanerHelper.htmlTags.transform(inputText);
  
  // Remove patterns such as below. They appeared to be noise.
  inputText = inputText.replaceAll("<!--(.*?)-->", "");
  inputText = inputText.replaceAll("==(.*?)==", "");
  inputText = inputText.replaceAll("__(.*?)__", "");
  inputText = inputText.replaceAll("<(.*?)>", "");
   
  // Remove links to categories.
  inputText = WikipediaTextCleanerHelper.categoryLinks.transform(inputText);
  inputText = inputText.replaceAll("\\[\\[" + categoryWord + ":(.+?)\\]\\]", "");
  inputText = inputText.replaceAll("\\[\\[.{0,3}:" + categoryWord + ":(.+?)\\]\\]", "");

  // Remove all white spaces at the beginning. 
  inputText = inputText.replaceAll("^[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", "");
  return inputText;
}

  /**
   * In some of the Wikipedia pages there exist a template "category explanation".
   * This function will return the text of this template as the category gloss.
   * 
   * Example:
   * {{Category explanation|[[viticulturist]]s, people who cultivate [[grape]]s, especially for winemaking}}
   * "viticulturists, people who cultivate grapes, especially for winemaking" is the gloss for the category.
   * 
   * @param inputText The Wikipedia page content.
   * @return Category gloss.
   */
  private static String extractCategoryExplanation(String inputText) {
    int start = inputText.toLowerCase().indexOf("{{category explanation|");
    int end = inputText.length()-1;
    int brackets = 0;
    for (int i=start;i<inputText.length();i++) {
      if (inputText.charAt(i) == '{')
        brackets++;
      else if (inputText.charAt(i) == '}')
        brackets--;
      if(brackets == 0) {
        end = i+1;
        break;
      }
    }
    inputText = inputText.substring(start+"{{category explanation|".length(), end-"}}".length());
    inputText = removePatterns(inputText);
    return inputText;
  }

  private class DescriptionExtractorCallable implements Callable<String> {

    private String text;
    
    public DescriptionExtractorCallable(String text) {
      this.text = text;
    }
    
    @Override
    public String call() throws Exception {
      return getGloss(text);
    }
    
    
  /** 
   * Returns a clean gloss for a category.
   * 
   * @param page The Wikipedia page to get the gloss from.
   * @return A clean gloss.
   */
    private String getGloss(String page) {
      // If exist "Category explanation" use it
      String normalizedPage = Char17.decodeAmpersand(page.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " "));
      if (normalizedPage.matches(".*\\{\\{[Cc]ategory [Ee]xplanation\\|.*")) {
        return cleanText(extractCategoryExplanation(normalizedPage));
      }
      else {
        int start = page.indexOf(">");
        page = page.substring(start + 1);
        page = Char17.decodeAmpersand(page);
        page = removePatterns(page);
        // Choose the first paragraph:
        Matcher matcher = WikipediaTextCleanerHelper.firstParagraph.matcher(page);
        if (matcher.find())
          return cleanText(matcher.group(1));
      }
      return null;
    }
    
  }
  
}
