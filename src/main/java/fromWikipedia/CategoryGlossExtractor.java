package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.YAGO;
import extractors.EnglishWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.PatternList;
import utils.Theme;
import utils.TitleExtractor;

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
    
    FactCollection patterns = new FactCollection();
    File categoryGlossCleaningFile = new File("/home/ghazaleh/Projects/data/_categoryGlossCleaning.tsv");
    for(Fact f: FactSource.from(categoryGlossCleaningFile)){
      patterns.add(f);
    }
    PatternList replacement = new PatternList(patterns, "<_replaceBy>");
    
    
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia());
		// Find pages about categories. example: <title>Category:Baroque_composers<\title>
		while(FileLines.findIgnoreCase(in, "<title>Category:") != -1) {
			
			String title = FileLines.readToBoundary(in, "</title>");
			title = Char17.decodeAmpersand(title);
			
			if (title != null){
				String category = FactComponent.forWikiCategory(FactComponent.stripBrackets(title));
				String page = FileLines.readBetween(in, "<text", "</text>");
				
		    String gloss = getGloss(page, replacement);
		    
		    
        if (gloss != null)
        	CATEGORYGLOSSES.write(new Fact(category, YAGO.hasGloss, gloss));
        
        if(category.equals("<wikicat_Romance_book_cover_images>") || category.equals("<wikicat_American_sportspeople_of_Afghan_descent>")) {
          System.out.println("Title: " + title);
          System.out.println("Cat  : " + category);
          System.out.println("page : " + page);
          System.out.println("Gloss: " + gloss);
        }
		        
			}
			
		}
		in.close();
	}

  private String getGloss(String page, PatternList replacement) {
    String gloss = null;
    // If exist "Category explanation" use it
    Matcher match = categoryExplanation.matcher(Char17.decodeAmpersand(Char17.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " "))));
    if (match.find()) {
      gloss = match.group(1);
    } 
    else {
      int start = page.indexOf(">");
      page = page.substring(start + 1);
      page = page.replaceAll("(([Ss]ee [Aa]lso.*?)|([Ff]or more.*?)|([Ff]or specific.*?))(.*)", "");
      page = Char17.decodeAmpersand(Char17.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " ")));
      page = removeBrackets(page);
      page = replacement.transform(page);
      page = page.replaceAll("__[A-Z]+__", "");
      page = page.replaceAll("__[a-z]+__", "");
      page = page.replaceAll("<(.*?)>", "");
      page = page.replaceAll("[\\s\\x00-\\x1F]+", " ");
      
      if (emptyStringPattern.matcher(page).matches()) {
        return null;
      }
      gloss = page;
    }
    
    gloss = gloss.replaceAll("\\[\\[[^\\]\n]+?\\|([^\\]\n]+?)\\]\\]", "$1");
    gloss = gloss.replaceAll("\\[\\[([^\\]\n]+?)\\]\\]", "$1");
    gloss = gloss.replaceAll("'{2,}", "");
    gloss =  gloss.replaceAll("^[^a-zA-Z0-9]+", "");
    gloss = gloss.replaceAll("[^a-zA-Z0-9\\.]+$", "");
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

//output: <wikicat_category> <hasGloss> <gloss>
// gloss e hano bade
