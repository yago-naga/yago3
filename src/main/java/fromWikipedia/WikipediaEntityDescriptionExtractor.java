/*
 * This class is part of the YAGO project at the Max Planck Institute
 * for Informatics/Germany and Télécom ParisTech University/France:
 * http://yago-knowledge.org
 * 
 * This class is copyright 2017 Ghazaleh Haratinezhad Torbati.
 * 
 * YAGO is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * YAGO is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
 * */

package fromWikipedia;

 import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
import extractors.Extractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;
import utils.WikipediaTextCleanerHelper;

/** Extracts entity description from wikipedia.
 * 
*/

public class WikipediaEntityDescriptionExtractor extends MultilingualWikipediaExtractor {

  public static final MultilingualTheme WIKIPEDIA_ENTITY_DESCRIPTIONS = new MultilingualTheme("wikipediaEntityDescriptions", 
      "Descriptions extracted from Wikipedia for entities.");

  public static final MultilingualTheme WIKIPEDIA_ENTITY_DESCRIPTIONS_NEED_TRANSLATION = new MultilingualTheme("wikipediaEntityDescriptionsNeedTranslation", 
      "Descriptions extracted from Wikipedia for entities.");

  
  private static String categoryWord = null;

  private ExecutorService executor = Executors.newSingleThreadExecutor();
  

  public WikipediaEntityDescriptionExtractor(String language, File wikipedia) {
    super(language, wikipedia);
  }

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<Theme>(Arrays.asList(
        PatternHardExtractor.TITLEPATTERNS, 
        RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(language), 
        DictionaryExtractor.CATEGORYWORDS));
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  @Override
  public Set<Theme> output() {
    if (isEnglish()) {
      return (new FinalSet<>(WIKIPEDIA_ENTITY_DESCRIPTIONS.inLanguage(language)));
    }
    else {
      return (new FinalSet<>(WIKIPEDIA_ENTITY_DESCRIPTIONS_NEED_TRANSLATION.inLanguage(language)));
    }
  }
  
  @Override
  public Set<FollowUpExtractor> followUp() {
    if (isEnglish()) {
      return (Collections.emptySet());
    }
    return (new FinalSet<FollowUpExtractor>(
        new EntityTranslator(WIKIPEDIA_ENTITY_DESCRIPTIONS_NEED_TRANSLATION.inLanguage(this.language), WIKIPEDIA_ENTITY_DESCRIPTIONS.inLanguage(this.language), this)));
  }

  @Override
  public void extract() throws Exception {
    categoryWord = DictionaryExtractor.CATEGORYWORDS.factCollection().getObject(FactComponent.forString(language), "<_hasCategoryWord>");
    categoryWord = FactComponent.stripQuotes(categoryWord);
    
    TitleExtractor titleExtractor = new TitleExtractor(language);
    
    Set<String> redirects = new HashSet<>();
    Set<Fact> redirectFacts = RedirectExtractor.REDIRECT_FACTS_DIRTY
        .inLanguage(language)
        .factCollection()
        .getFactsWithRelation("<redirectedFrom>");
    
    for (Fact f : redirectFacts) {
      String entity = titleExtractor.createTitleEntity(FactComponent.stripQuotesAndLanguage(f.getObject()));
      redirects.add(entity);
    }
    
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    
    while (FileLines.findIgnoreCase(in, "<title>") != -1) {
      String titleEntity = titleExtractor.getTitleEntity(in);
      
      // We don't want to extract non named entity or redirects:
      if (titleEntity == null || redirects.contains(titleEntity)) {
        continue;
      }
      
      String page = FileLines.readBetween(in, "<text", "</text>");
      String description;
      // Due to not clean Wikipedia dumps, we use a time out to avoid getting into infinite loops.
      try {
        description = executor.submit(new DescriptionExtractorCallable(page)).get(1, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        description = null;
      }
      
      // Write description to themes. If the language is not English, write it to a theme that 
      // needs translation which is done in follow up extractor.
      if(description != null) {
        if(isEnglish()) {
          WIKIPEDIA_ENTITY_DESCRIPTIONS.inLanguage(language).write(new Fact(
              titleEntity, 
              YAGO.hasLongDescription, 
              FactComponent.forString(Char17.decodeBackslash(description))));
        } else {
          WIKIPEDIA_ENTITY_DESCRIPTIONS_NEED_TRANSLATION.inLanguage(language).write(new Fact(
              titleEntity, 
              YAGO.hasLongDescription, 
              FactComponent.forString(Char17.decodeBackslash(description))));
        }
      }
    }
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
   // This text appeared in some wikipedia articles, and has no information. Remove it.
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
  * Cleaning the text to human readable.
  * 
  * @param inputText The description for the entity that needs cleaning.
  * @return Clean description text.
  */
 private String cleanText(String inputText) {
   
   // Replacing internal wikipedia links with their text.
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
   
   // Remove extra whites paces.
   inputText = WikipediaTextCleanerHelper.whiteSpaces.transform(inputText);
   
   inputText = inputText.replaceAll("&nbsp;", " ");
   inputText = inputText.replaceAll("\\u0022", "\"");

   
   if(inputText.length() < WikipediaTextCleanerHelper.MIN_TEXT_LENGTH)
     return null;
   
   return inputText;
 }
 
  private class DescriptionExtractorCallable implements Callable<String> {
    
    private String text;
    
    public DescriptionExtractorCallable(String text) {
      this.text = text;
    }
    
    @Override
    public String call() throws Exception {
      return getDescription(text);
    }
    
    
    /** 
     * Returns a clean description.
     * 
     * @param page The Wikipedia page to get description from.
     * @return A clean description.
     */
    private String getDescription(String page) {
      int start = page.indexOf(">");
      page = page.substring(start + 1);
      page = Char17.decodeAmpersand(page);
      page = removePatterns(page);
      
      // Choose the first paragraph.
      Matcher matcher = WikipediaTextCleanerHelper.firstParagraph.matcher(page);
      if (matcher.find()) {
        return cleanText(matcher.group(1));
      }
      
      return null;
    } 
    
  }

}
