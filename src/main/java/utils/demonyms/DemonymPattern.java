package utils.demonyms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import javatools.filehandlers.FileUtils;

/**
 * Helper functions/patterns to deal with Wikipedia
 * @author Thomas Rebele
 *
 */
public class DemonymPattern {

  public static Pattern simplePattern = Pattern.compile("[a-zA-ZéñÅ. ]*");

  public static Pattern paranthesisPattern = Pattern.compile("\\([^\\)]*\\)");

  public static Pattern quotePattern = Pattern.compile("''([^']*)''");

  public static Pattern wikiLink = Pattern.compile("\\[\\[(?<page>[^\\|\\]#]*)(#(?<section>[^\\|\\]]*))?(\\|(?<anchor>[^|\\]]*))?\\]\\]");

  public static Pattern refPattern = Pattern.compile(Pattern.quote("<ref") + "[^<]*" + Pattern.quote("</ref>"));

  public static Pattern bracesPattern = Pattern.compile(Pattern.quote("{{") + "[^}]*" + Pattern.quote("}}"));

  /**
   * Extract demonyms from wikipedia; creates a cache file in temporary folder
   * @param title
   * @param language
   * @return
   */
  public static String getPage(String title, String language) {
    // load file locally if exists
    String dir = "data/demonyms/";
    String filename = "wiki-" + language + "-" + title + ".txt";
    File cacheFile = new File(dir, filename);
    String content = null;
    if (cacheFile.exists()) {
      // load from cache
      try {
        content = FileUtils.getFileContent(cacheFile);
        return content;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    /*try {
      // download from wikipedia.org
      String url = "https://" + language + ".wikipedia.org/w/index.php?title=" + URLEncoder.encode(title, "utf-8") + "&action=raw";
      content = httpGetRequest(url);
      FileUtils.writeFileContent(cacheFile, content);
    } catch (IOException e) {
      e.printStackTrace();
    }*/
    return content;
  }

  private static String httpGetRequest(String urlString) {
    try {
      URL url = new URL(urlString);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("User-Agent", "Mozilla/5.0");

      //int responseCode = con.getResponseCode();

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      StringBuffer response = new StringBuffer();

      String output;
      while ((output = in.readLine()) != null) {
        response.append(output + "\n");
      }
      in.close();

      return response.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

}
