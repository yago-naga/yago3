package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class CategoryGlossExtractor extends MultilingualWikipediaExtractor {

  private static Pattern categoryExplanation = Pattern.compile("\\{\\{Category explanation\\|(.*?)\\}\\}");

  private static Pattern emptyStringPattern = Pattern.compile("\\s*");
  
  private String categoryWord = null;
  
  private static final int MINTEXTLENGTH = 10;
  
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
		while(FileLines.findIgnoreCase(in, "<title>" + categoryWord + ":") != -1) {
			String title = FileLines.readToBoundary(in, "</title>");
			title = Char17.decodeAmpersand(title);
			if (title != null){
				String category = FactComponent.forWikiCategory(FactComponent.stripBrackets(title));
				String page = FileLines.readBetween(in, "<text", "</text>");
		    String gloss = getGloss(page /*, replacement*/, category);
        if (gloss != null)
        	CATEGORYGLOSSES.inLanguage(language).write(new Fact(category, YAGO.hasGloss, gloss));
			}
		}
		in.close();
	}

  private String getGloss(String page /*, PatternList replacement*/, String category) {
    String gloss = null;
    // If exist "Category explanation" use it
    Matcher match = categoryExplanation.matcher(Char17.decodeAmpersand(Char17.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " "))));
    if (match.find()) {
      gloss = match.group(1);
    } 
//    else {
//      int start = page.indexOf(">");
//      page = page.substring(start + 1);
//      page = removePatterns(page);      
//      if (emptyStringPattern.matcher(page).matches()) {
//        return null;
//      }
//      gloss = page;
//    }
    else {
      int start = page.indexOf(">");
      page = page.substring(start + 1);
      page = removeBrackets(page);
      gloss = page.replaceAll("(.+)?\\n(.*)","$1");
    }
    
    if (emptyStringPattern.matcher(gloss).matches()) return null;
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

  private String cleanText(String inputText){
    inputText = inputText.replaceAll("\\*", "");
    inputText = inputText.replaceAll( ":{2,}", "");
    inputText = inputText.replaceAll("\\[\\[[^\\]\n]+?\\|([^\\]\n]+?)\\]\\]", "$1");
    inputText = inputText.replaceAll("\\[\\[([^\\]\n]+?)\\]\\]", "$1");
    inputText = inputText.replaceAll("'{2,}", "");
    inputText = inputText.replaceAll("\\s+([\\.!;,\\?])", "$1");
    inputText = inputText.replaceAll("[^\\p{L}\\s]+(.*?[\\.\\?!:;,])", "$1");
    inputText = inputText.replaceAll("^\\s+", "");
    inputText = inputText.replaceAll("\\P{L}+$", ".");
    return inputText;
  }
  
  // Extracting gloss text by removing some patterns that are observed to not have clean and good information
  private String removePatterns(String inputText) {
    
    inputText = inputText.replaceAll("(([Ss]ee [Aa]lso.*?)|([Ff]or more.*?)|([Ff]or specific.*?)|([Ss]ee [Tt]he)|([Ss]ee:)|(For .+?[,-] see)|([Cc]lick [Oo]n))(.*)", "");
    //page = page.replaceAll("(([Ss]iehe auch))(.*)", "");// This is not a solution.
    inputText = inputText.replaceAll("<br */>", " ");
    inputText = inputText.replaceAll("<br *>", " ");
    inputText = inputText.replaceAll("</ *br *>", " ");
    inputText = Char17.decodeAmpersand(Char17.decodeAmpersand(inputText.replaceAll("[\\s\\x00-\\x1F]+", " ")));
    //inputText = removeBrackets(inputText); moved to up
    inputText = inputText.replaceAll("\\[\\[Category:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[" + categoryWord + ":(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("<!--(.*?)-->", "");
    inputText = inputText.replaceAll("\\[\\[.{0,3}:[Cc]ategory:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[.{0,3}:" + categoryWord + ":(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[File:(.+?)\\]\\]", "");
    //page = page.replaceAll("\\[\\[Datei:(.+?)\\]\\]", "");// "Datei" is German for "File", it was observed in results
    inputText = inputText.replaceAll("\\[\\[Image:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("\\[\\[wp:(.+?)\\]\\]", "");
    inputText = inputText.replaceAll("==(.*?)==", "");
    inputText = inputText.replaceAll("The (.*?)magic word(.*?) <nowiki>__NOGALLERY__</nowiki> is used in this category to turn off thumbnail display since this category list unfree images, the display of which is restricted to certain areas of Wikipedia.", "");
    inputText = inputText.replaceAll("<table(.*?)>(.*?)</table>", "");
    inputText = inputText.replaceAll("<gallery>(.*)</gallery>", "");
    inputText = inputText.replaceAll("<imagemap>(.*)</imagemap>", "");
    //page = replacement.transform(page); // the patterns above where in the pattern file before.
    inputText = inputText.replaceAll("__.*?__", "");
    inputText = inputText.replaceAll("<(.*?)>", "");
    inputText = inputText.replaceAll("[\\s\\x00-\\x1F]+", " ");
    return inputText;
  }
  
}
