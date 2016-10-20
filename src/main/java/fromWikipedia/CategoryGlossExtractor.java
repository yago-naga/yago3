package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.EnglishWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.Theme;

public class CategoryGlossExtractor extends EnglishWikipediaExtractor {

  private static Pattern categoryExplanation = Pattern.compile("\\{\\{Category explanation\\|(.*?)\\}\\}");

  private static Pattern emptyStringPattern = Pattern.compile("\\s*");
  
  public static final Theme CATEGORYGLOSSES = new Theme("wikipediaCategoryGlosses", "Category glosses extracted from wikipedia");

  public CategoryGlossExtractor(File wikipedia) {
    super(wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.PREFMEANINGS));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(CATEGORYGLOSSES));
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
    
    
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia());
		// Find pages about categories. example: <title>Category:Baroque_composers<\title>
		while(FileLines.findIgnoreCase(in, "<title>Category:") != -1) {
			String title = FileLines.readToBoundary(in, "</title>");
			title = Char17.decodeAmpersand(title);
			if (title != null){
				String category = FactComponent.forWikiCategory(FactComponent.stripBrackets(title));
				String page = FileLines.readBetween(in, "<text", "</text>");
		    String gloss = getGloss(page /*, replacement*/, category);
        if (gloss != null)
        	CATEGORYGLOSSES.write(new Fact(category, YAGO.hasGloss, gloss));
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
    else {
      // Extracting gloss text by removing some patterns that are observed to not have clean and good information
      int start = page.indexOf(">");
      page = page.substring(start + 1);
      page = page.replaceAll("(([Ss]ee [Aa]lso.*?)|([Ff]or more.*?)|([Ff]or specific.*?)|([Ss]ee [Tt]he)|([Ss]ee:)|(For .+?[,-] see)|([Cc]lick [Oo]n))(.*)", "");
      page = page.replaceAll("<br ?/>", " ");
      page = page.replaceAll("<br ?>", " ");
      page = page.replaceAll("</ ?br ?>", " ");
      page = Char17.decodeAmpersand(Char17.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " ")));
      page = removeBrackets(page);
      page = page.replaceAll("\\[\\[Category:(.+?)\\]\\]", "");
      page = page.replaceAll("<!--(.*?)-->", "");
      page = page.replaceAll("\\[\\[.{0,3}:Category:(.+?)\\]\\]", "");
      page = page.replaceAll("\\[\\[[Ff]ile:(.+?)\\]\\]", "");
      page = page.replaceAll("\\[\\[[Ii]mage:(.+?)\\]\\]", "");
      page = page.replaceAll("==(.*?)==", "");
      page = page.replaceAll("The (.*?)magic word(.*?) <nowiki>__NOGALLERY__</nowiki> is used in this category to turn off thumbnail display since this category list unfree images, the display of which is restricted to certain areas of Wikipedia.", "");
      page = page.replaceAll("<table(.*?)>(.*?)</table>", "");
      page = page.replaceAll("<gallery>(.*)</gallery>", "");
      page = page.replaceAll("<imagemap>(.*)</imagemap>", "");
      //page = replacement.transform(page); // the patterns above where in the pattern file before.
      page = page.replaceAll("__[A-Z]+__", "");
      page = page.replaceAll("__[a-z]+__", "");
      page = page.replaceAll("<(.*?)>", "");
      page = page.replaceAll("[\\s\\x00-\\x1F]+", " ");
      
      if (emptyStringPattern.matcher(page).matches()) {
        return null;
      }
      gloss = page;
    }
    
    // Cleaning up the gloss text
    gloss = gloss.replaceAll("\\*", "");
    gloss = gloss.replaceAll( ":{2,}", "");
    gloss = gloss.replaceAll("\\[\\[[^\\]\n]+?\\|([^\\]\n]+?)\\]\\]", "$1");
    gloss = gloss.replaceAll("\\[\\[([^\\]\n]+?)\\]\\]", "$1");
    gloss = gloss.replaceAll("'{2,}", "");
    gloss = gloss.replaceAll("\\s+:([a-zA-Z0-9])", " $1");
    gloss = gloss.replaceAll("\\s+([\\.!;,\\?])", "$1");
    gloss = gloss.replaceAll("^[^a-zA-Z0-9]+", "");
    gloss = gloss.replaceAll("[^a-zA-Z0-9 ]+$", ".");

    if(gloss.length() < 10)
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

}
