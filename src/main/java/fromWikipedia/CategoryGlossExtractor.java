package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualWikipediaExtractor;
import fromOtherSources.DictionaryExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.MultilingualTheme;
import utils.Theme;

/** Extracts category glosses from Wikipedia
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Ghazaleh Haratinezhad.

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
public class CategoryGlossExtractor extends MultilingualWikipediaExtractor {

  private String categoryWord = null;
  
  private static final int MINTEXTLENGTH = 15;
  
  public static final MultilingualTheme CATEGORYGLOSSES = new MultilingualTheme("wikipediaCategoryGlosses", "Category glosses extracted from wikipedia");

  public CategoryGlossExtractor(String language, File wikipedia) {
    super(language, wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(DictionaryExtractor.CATEGORYWORDS));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(CATEGORYGLOSSES.inLanguage(language)));
  }

  @Override
	public void extract() throws Exception {
    //TODO: make the pattern file and use this instead of lots of patterns in the code
    /*
    FactCollection patterns = new FactCollection();
    File categoryGlossCleaningFile = new File("/home/ghazaleh/Projects/data/_categoryGlossCleaning.tsv");
    for(Fact f: FactSource.from(categoryGlossCleaningFile)){
      patterns.add(f);
    }
    PatternList replacement = new PatternList(patterns, "<_replaceBy>");
//    for (Pair<Pattern, String> pattern : replacement.patterns) {
//      System.out.println("Pattern: "+ pattern);
//    }
// problem: the order is not kept    
  */
    
    categoryWord = DictionaryExtractor.CATEGORYWORDS.factCollection().getObject(FactComponent.forString(language), "<_hasCategoryWord>");
    categoryWord = FactComponent.stripQuotes(categoryWord);
    
    System.out.println(language + " - " + categoryWord);
    
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		// Find pages about categories. example in English: <title>Category:Baroque_composers<\title>
		while(FileLines.findIgnoreCase(in, "<title>" + categoryWord + ":", "<title>" + "Category:") != -1) {
			String title = FileLines.readToBoundary(in, "</title>");
			title = Char17.decodeAmpersand(title);
			if (title != null){
				String category = FactComponent.forWikiCategory(FactComponent.stripBrackets(title));
				String page = FileLines.readBetween(in, "<text", "</text>");
		    String gloss = getGloss(page /*, replacement*/, category);
        if (gloss != null)
        	CATEGORYGLOSSES.inLanguage(language).write(new Fact(category, YAGO.hasGloss, FactComponent.forString(gloss)));
			}
		}
		in.close();
	}

  private String getGloss(String page /*, PatternList replacement*/, String category) {
    String gloss = null;
    // If exist "Category explanation" use it
    String normalizedPage = Char17.decodeAmpersand(page.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " "));
    if (normalizedPage.matches(".*\\{\\{[Cc]ategory [Ee]xplanation\\|.*")) {
      gloss = extractCategoryExplanation(normalizedPage);
    }
    else {
      int start = page.indexOf(">");
      page = page.substring(start + 1);
      page = removeBrackets(page);
      page = Char17.decodeAmpersand(page);
      // Choose the first paragraph:
      gloss = page.replaceAll("(.+)?\\n(.*)","$1");
      
      // For example: in the case below choosing a first paragraph is leading us to noise.
      // <!-- BITTE bei den Biografien der entsprechenden Personen .... \n
      // ... -->
      if (gloss.matches("^\\s*(<|&lt;).*")) 
        gloss = "";
      
      gloss = removePatterns(gloss);
    }
    
    // Cleaning up the gloss text
    gloss = cleanText(gloss);

    if(gloss.length() < MINTEXTLENGTH)
      return null;
    
    return gloss;
  }
  
// Remove lines such as: {{ text... }} 
  private String removeBrackets(String page) {
    StringBuilder result = new StringBuilder();
    int brackets = 0;
    for(int i = 0; i < page.length(); i++){
      char current = page.charAt(i);
      if(current == '{'){
        brackets++;
      }
      else if (current == '}') {
        brackets--;
      }
      else if( brackets == 0)
        result.append(current);
    }
    return result.toString().trim();
  }
  
  // Cleaning the gloss before returning it as output.
  private String cleanText(String inputText){
    // Replace links with text:
    // examlpe: "[[Cavalier|Royalists]]" replace it with "Royalists"
    //          "[[Robert Owen]]" replace it with "Robert Owen".
    inputText = inputText.replaceAll("\\[\\[[^\\]\\n]+?\\|([^\\]\\n]+?)\\]\\]", "$1");
    inputText = inputText.replaceAll("\\[\\[([^\\]\\n]+?)\\]\\]", "$1");
    inputText = inputText.replaceAll("\\{\\{[^\\}\\n]+?\\|([^\\}\\n]+?)\\}\\}", "$1");
    inputText = inputText.replaceAll("\\{\\{([^\\}\\n]+?)\\}\\}", "$1");
    
    inputText = inputText.replaceAll("\\*", "");
    inputText = inputText.replaceAll( ":{2,}", "");
    inputText = inputText.replaceAll("'{2,}", "");
    inputText = inputText.replaceAll("\\d+px", "");

    // Remove whitespace before the punctuations:
    inputText = inputText.replaceAll("\\s+([\\.!;,\\?])", "$1");
    
    // Remove Urls:
    inputText = inputText.replaceAll("\\[(http|https)://[^\\p{Zl}\\p{Zs}\\p{Zp}]+[\\p{Zl}\\p{Zs}\\p{Zp}](.*?)\\]", "$2");
    inputText = inputText.replaceAll("[\\[\\]]", "");
    
    // Remove punctuations from the beginning of the gloss.
    inputText = inputText.replaceAll("^[\\.!;:,\\?]+", "");
    // Remove Whitespaces from beginning and end of gloss:
    inputText = inputText.replaceAll("^[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", "");
    inputText = inputText.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+$", "");
    // Change any more than 1 whitespace to only 1 whitespace:
    inputText = inputText.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " ");

    return inputText;
  }
  
  // Extracting gloss text by removing some patterns that are observed to not have clean and good information
  private String removePatterns(String inputText) {
    // Remove some language specific texts such as: see also:...
    inputText = inputText.replaceAll("(([Ss]ee [Aa]lso.*?)|(Note:)|([Ff]or more.*?)|([Ff]or specific.*?)|([Ss]ee [Tt]he)|([Ss]ee:)|(For .+?[,-] see)|([Cc]lick [Oo]n))(.*)", "");
    inputText = inputText.replaceAll("(([Ss]iehe [Aa]uch)|(Hinweis:))(.*)", "");
    // Remove line breake in form of <br>
    inputText = inputText.replaceAll("<br */>", " ");
    inputText = inputText.replaceAll("<br *>", " ");
    inputText = inputText.replaceAll("</ *br *>", " ");
    // Replace new line, and any other whitespace with one whitespace:
    inputText.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+", " ");
    // Remove HTML tags and what is inside them such as:
    inputText = inputText.replaceAll("<table.*?>(.*)?</table>", "");
    inputText = inputText.replaceAll("<gallery>(.*)?</gallery>", "");
    inputText = inputText.replaceAll("<imagemap>(.*)?</imagemap>", "");
    // Remove patterns such as below. They appeared to be noise.
    inputText = inputText.replaceAll("<!--(.*?)-->", "");
    inputText = inputText.replaceAll("==(.*?)==", "");
    inputText = inputText.replaceAll("__(.*?)__", "");
    inputText = inputText.replaceAll("<(.*?)>", "");
    // Remove links to categories or files or images:
    inputText = inputText.replaceAll("\\[\\[Category:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[" + categoryWord + ":(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[.{0,3}:[Cc]ategory:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[.{0,3}:" + categoryWord + ":(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[File:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[Datei:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[Image:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[Bild:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[wp:(.+?)\\]\\]", "");
    // This text appeared in some wikipedia articles, and has no information. Remove it.
    inputText = inputText.replaceAll("The (.*?)magic word(.*?) <nowiki>__NOGALLERY__</nowiki> is used in this category to turn off thumbnail display since this category list unfree images, the display of which is restricted to certain areas of Wikipedia.", "");
    
    
    return inputText;
  }
  
  // Return the text in category explanations: {{category explanation|text...}}
  private String extractCategoryExplanation(String inputText) {
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
  
}
