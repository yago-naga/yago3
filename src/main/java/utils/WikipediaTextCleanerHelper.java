package utils;

import java.util.Arrays;
import java.util.regex.Pattern;

import javatools.datatypes.Pair;

public class WikipediaTextCleanerHelper {
  
  private static final int MAX_ESTIMATED_FIRST_PARAGRAPH_SIZE = 30000;
  public static final int MIN_TEXT_LENGTH = 15;
  
  
  /**
   * Patterns for internal Wikipedia links.
   * This list is used to replace them with their text.
   * 
   * For example:
   * "[[Cavalier|Royalists]]" which should be replaced by "Royalists".
   * "[[Robert Owen]]" which should be replaced by "Robert Owen".
  */
  public static final PatternList internalLinks = new PatternList(Arrays.asList(
      new Pair<>(Pattern.compile("\\[\\[[^\\]\\n]+?\\|([^\\]\\n]+?)\\]\\]"), "$1"),
      new Pair<>(Pattern.compile("\\[\\[([^\\]\\n]+?)\\]\\]"), "$1"),
      new Pair<>(Pattern.compile("\\{\\{[^\\}\\n]+?\\|([^\\}\\n]+?)\\}\\}"), "$1"),
      new Pair<>(Pattern.compile("\\{\\{([^\\}\\n]+?)\\}\\}"), "$1")));

  /**
   * Patterns for internal Wikipedia links to Wikipedia Categories.
   * This list is used to remove them (replace them with empty string).
   * 
   * For example:
   * "[[Category:Business theory]]" which should be replaced by "".
  */
  public static final PatternList categoryLinks = new PatternList(Arrays.asList(
      new Pair<>(Pattern.compile("\\[\\[Category:(.+?)\\]\\]", Pattern.CASE_INSENSITIVE), ""),
      new Pair<>(Pattern.compile("\\[\\[.{0,3}:Category:(.+?)\\]\\]", Pattern.CASE_INSENSITIVE), "")));
  
  /**
   * Patterns for HTML tags used in Wikipedia page contents.
   * This list is used for mainly remove them.
   *  
  */
  public static final PatternList htmlTags = new PatternList(Arrays.asList(
      new Pair<>(Pattern.compile("<br */>"), "\n"),
      new Pair<>(Pattern.compile("<br *>"), "\n"),
      new Pair<>(Pattern.compile("</ *br *>"), "\n"),
      new Pair<>(Pattern.compile("<(.*?)/>"), ""),
      new Pair<>(Pattern.compile("<table.*?>(.*?)</table>"), ""),
      new Pair<>(Pattern.compile("<gallery.*?>(.*?)</gallery>"), ""),
      new Pair<>(Pattern.compile("<imagemap.*?>(.*?)</imagemap>"), ""),
      new Pair<>(Pattern.compile("<ref.*?>(.*?)</ref>"), ""),
      new Pair<>(Pattern.compile("<nowiki.*?>(.*?)</nowiki>"), "")));
  
  /**
   * Patterns for white spaces.
   * This list of patterns is used in order:
   * 1- Removing white spaces before punctuation.
   * 2- Removing white spaces from beginning of the text.
   * 3- Removing white spaces from the end of the text.
   * 4- Replacing any more than one white spaces to only one white space.
  */
  public static final PatternList whiteSpaces = new PatternList(Arrays.asList(
      new Pair<>(Pattern.compile("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+([\\.!;:,\\?])"), "$1"),
      new Pair<>(Pattern.compile("^[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+"), ""),
      new Pair<>(Pattern.compile("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+$"), ""),
      new Pair<>(Pattern.compile("[\\p{Zl}\\p{Zs}\\p{Zp}\\n]+"), " ")));
  
  
  public static Pattern firstParagraph = Pattern.compile("^(.+?)\\n(.*)");
  
  //new Pair<>(Pattern.compile(""), ""),
  
  /** 
   * Remove useless links from Wikipedia page content.
   * Such as linkes to Files, Images, ect.
   * 
   * @param page The Wikipedia page content, from which the useless links are removed.
   * @return The page content without the useless links.
   */
   public static String removeUselessLinks(String page) {
     StringBuilder result = new StringBuilder(page);
     String removePatterns[] = {"[[File:","[[Datei", "[[Image:", "[[Bild:", "[[wp:"};
   
     for (int r = 0; r < removePatterns.length; r++) {
       int idx = result.indexOf(removePatterns[r]);
       
       if (idx > MAX_ESTIMATED_FIRST_PARAGRAPH_SIZE) {
         continue;
       }
       
       if (idx != -1) {
         // In order to search the same pattern again.
         r--;
         int brackets = 0;
         
         for (int i = idx ; i < result.length(); i++) {
           char current = result.charAt(i);
           
           if (current == '[') {
             brackets++;
           }
           else if (current == ']') {
             brackets--;
           }
         
           if (brackets == 0 || current == '\n') {
             result.delete(idx, i+1);
             break;
           }
         
           if (brackets == -1) {
             brackets = 0;
           }
         }
             
         if(brackets != 0) {
           result.delete(idx, result.length());
         }
       }
           
     }
       
     return result.toString().trim();
   }
 
 /**
  * Remove any accruing parentheses and the content between them from the page content.
  * The reason for this is observation that the content in parentheses are nor relevant to out purpose.
  * 
  * @param page The Wikipedia page content, from which the parentheses are removed.
  * @return The page content without the parantheses.
  */
  public static String removeParentheses(String page) {
   StringBuilder result = new StringBuilder();
   int parenthesis = 0;
   
   for (int i = 0; i < page.length(); i++) {
     char current = page.charAt(i);
     
     if (current == '(') {
       parenthesis++;
     }
     else if (current == ')') {
       parenthesis--;
     }
     else if (parenthesis == 0) {
       result.append(current);
     }
     
     if (parenthesis == -1) {
       parenthesis = 0;
     }
   }
   
   return result.toString().trim();
  }

  /**
   * Remove content between curly  brackets and the brackets themselves.
   * In Wikipedia curly brackets are used to point to templates which are 
   * not relevant to getting the gloss/description.
   *
   * @param page The Wikipedia page content, from which the curly brackets are removed.
   * @return The page content without the curly brackets.
   */
  public static String removeBrackets(String page) {
    StringBuilder result = new StringBuilder();
    int brackets = 0;
    
    for (int i = 0; i < page.length(); i++) {
      char current = page.charAt(i);
     
      if (current == '{') {
        brackets++;
      }
      else if (current == '}') {
        brackets--;
      }
      else if (brackets == 0) {
        result.append(current);
      }
     
      if (brackets == -1) {
        brackets = 0;
      }
    }
    
    return result.toString().trim();
  }

}
