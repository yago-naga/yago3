package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualWikipediaExtractor;
import followUp.CategoryTranslator;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

public class WikipediaEntityDescriptionExtractor extends MultilingualWikipediaExtractor {

  private static final MultilingualTheme WIKIPEDIAENTITYDESCRIPTIONS = new MultilingualTheme("wikipediaEntityDescription", 
      "Description extracted from wikipedia for entities");
  
  
  //TODO: discuss this: In yago there are 2 different ways of saving files for the translated facts:
  //1- you create the not translated like this "categoryMembers" and then make a translated like this: "categoryMembersTranslated" and second one is used
  //2- you create need translated theme for non english like this "conteXtFactsNeedsTranslation" and then make a translated like this "yagoConteXtFacts" and use this.
  private static final MultilingualTheme WIKIPEDIAENTITYDESCRIPTIONS_TRANSLATED = new MultilingualTheme("wikipediaEntityDescriptionTranslatedEntities", 
      "Description extracted from wikipedia for translated entities");


  private static Pattern firstParagraph = Pattern.compile("^(.+?)\\n(.*)");
  private static final int MINTEXTLENGTH = 15;

  public WikipediaEntityDescriptionExtractor(String language, File wikipedia) {
    super(language, wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.PREFMEANINGS,
        RedirectExtractor.REDIRECTFACTSDIRTY.inLanguage(language)));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(WIKIPEDIAENTITYDESCRIPTIONS.inLanguage(language)));
  }
  
  @Override
  public Set<FollowUpExtractor> followUp() {
    if (language.equals("en")) return (Collections.emptySet());
    return (new FinalSet<FollowUpExtractor>(
        new EntityTranslator(WIKIPEDIAENTITYDESCRIPTIONS.inLanguage(this.language), WIKIPEDIAENTITYDESCRIPTIONS_TRANSLATED.inLanguage(this.language), this)));
  }

  @Override
  public void extract() throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor(language);
    
    Set<String>  redirects = new HashSet<>();
    Set<Fact> redirectFacts = RedirectExtractor.REDIRECTFACTSDIRTY.inLanguage(language).factCollection().getFactsWithRelation("<redirectedFrom>");
    for(Fact f:redirectFacts) {
      String entity = titleExtractor.createTitleEntity(FactComponent.stripQuotesAndLanguage(f.getObject()));
      redirects.add(entity);
    }
    
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    
    
    while(FileLines.findIgnoreCase(in, "<title>") != -1) {
      String titleEntity = titleExtractor.getTitleEntity(in);
      if (titleEntity == null || redirects.contains(titleEntity))  continue;
      
      String page = FileLines.readBetween(in, "<text", "</text>");
      // get first paragraph for desc clean it.
      String description = getDescription(page);
      System.exit(0);
      if(description != null) 
        WIKIPEDIAENTITYDESCRIPTIONS.inLanguage(language).write(new Fact(titleEntity, YAGO.hasLongDescription, FactComponent.forString(description)));
    }
    
  }

  private String getDescription(String page) {
    int start = page.indexOf(">");
    page = page.substring(start + 1);
    page = removeBrackets(page);
    page = Char17.decodeAmpersand(page);
    page = removePatterns(page);
    // Choose the first paragraph:
    Matcher matcher = firstParagraph.matcher(page);
    if (matcher.find())
      return cleanText(matcher.group(1));
    return null;
  }
  
//Remove lines such as: {{ text... }} 
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

   
   if(inputText.length() < MINTEXTLENGTH)
     return null;
   
   return inputText;
 }
 
 // Extracting gloss text by removing some patterns that are observed to not have clean and good information
 private String removePatterns(String inputText) {
   // Remove some language specific texts such as: see also:...
   inputText = inputText.replaceAll("(([Ss]ee [Aa]lso.*?)|(Note:)|([Ff]or more.*?)|([Ff]or specific.*?)|([Ss]ee [Tt]he)|([Ss]ee:)|(For .+?[,-] see)|([Cc]lick [Oo]n))(.*)", "");
   inputText = inputText.replaceAll("(([Ss]iehe [Aa]uch)|(Hinweis:))(.*)", "");
   // Remove line breake in form of <br>
   inputText = inputText.replaceAll("<br */>", "\n");
   inputText = inputText.replaceAll("<br *>", "\n");
   inputText = inputText.replaceAll("</ *br *>", "\n");
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
   inputText = inputText.replaceAll("\\[\\[.{0,3}:[Cc]ategory:(.+?)\\]\\]", "");
   inputText = inputText.replaceAll("\\[\\[File:(.+?)\\]\\]", "");
   inputText = inputText.replaceAll("\\[\\[Datei:(.+?)\\]\\]", "");
   inputText = inputText.replaceAll("\\[\\[Image:(.+?)\\]\\]", "");
   inputText = inputText.replaceAll("\\[\\[Bild:(.+?)\\]\\]", "");
   inputText = inputText.replaceAll("\\[\\[wp:(.+?)\\]\\]", "");
   // This text appeared in some wikipedia articles, and has no information. Remove it.
   inputText = inputText.replaceAll("The (.*?)magic word(.*?) <nowiki>__NOGALLERY__</nowiki> is used in this category to turn off thumbnail display since this category list unfree images, the display of which is restricted to certain areas of Wikipedia.", "");
   // Remove empty line.
   inputText = inputText.replaceAll("([\\p{Zl}\\p{Zs}\\p{Zp}]+)\\n+", "\n");

   return inputText;
 }

}
