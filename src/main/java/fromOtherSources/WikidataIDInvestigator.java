package fromOtherSources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;

public class WikidataIDInvestigator {
  
  private static final String folderPath = "/home/ghazaleh/Projects/data/wikidataID_investigation/";
  private static final String wikipediaPath = "/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/en/20160701/enwiki-20160701-pages-articles.xml";
  private static final String wikidataPath  = "/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/wikidata_dump/20160621/wikidata-sitelinks.nt";

  //Pattern of the redirect tags in wiki pages
  private static final Pattern redirectPattern = Pattern.compile("<redirect\\s+title=(.*?)\\s*\\/>");
  
  //List of entity ids which are the same as titles (replace _ with space) and
  // the same as URL if we just add "en.wikipedia.org/wiki/" to the beginning.
  // Since only 1 entity was out of english wikipedia I continue with this and deleted that one entity from the file. which is: 21611198;"Vision discography";"http://De.wikipedia.org/wiki/Vision%20discography";"<De/Vision_discography>"
  private static Set<String> entitiesWithNoIdsFromDB = new HashSet<String>();
  
  // List of the entity ids that DOES have a page in the dump. (should be the same as 
  // the previous list, but I do it for checking.
  private static List<String> entitiesWithPageInDump = new LinkedList<>();
  
  // Map of entity id to the redirect title extracted from wikipedia.
  // redirect pages do not have to have a wikidataID.
  private static List<String> redirectsIdtoTitle = new LinkedList<>();
  
  // List of the entity ids that do have a page in wikipedia dump and 
  // where not redirects. I want to investigate on these that why they
  // do not have wikidataIDs.
  private static Set<String> entitiesWithPageNotRedirect  = new HashSet<>();
  
  // Map of entity ids to the wikidataID. Only for the entities that have a page
  // and are not redirects.
  private static List<String> entitiesWithPageNotRedirectWithWikidataID = new LinkedList<>();
  
  
  private static PrintWriter redirectWriter;
  private static PrintWriter entitiesWithNoPageWriter;
  private static PrintWriter entitiesWithPageNotRedirectWriter;
  private static PrintWriter entitiesWithWikidataIDWriter;
  private static PrintWriter entitiesWithPageNotRedirectNoWikidataIDWriter;
  
  public static void main(String[] args) {
    try {
      redirectWriter = new PrintWriter(folderPath + "redirects", "UTF-8");
      entitiesWithNoPageWriter = new PrintWriter(folderPath + "entitiesWithNoPage", "UTF-8");
      entitiesWithPageNotRedirectWriter = new PrintWriter(folderPath + "entitiesWithPageNotRedirect", "UTF-8");
      entitiesWithWikidataIDWriter = new PrintWriter(folderPath + "entitiesWithWikidataID", "UTF-8");
      entitiesWithPageNotRedirectNoWikidataIDWriter = new PrintWriter(folderPath + "entitiesWithPageNotRedirectNoWikidataID", "UTF-8");
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    readEntitiesWithNoId(folderPath + "entitiesWithNoIdsFromDB", ';');
    
    findRedirectPages();
    
    // entities with page and not being redirects.
    for (String entity:entitiesWithPageInDump) {
      if (redirectsIdtoTitle.contains(entity)) continue;
      entitiesWithPageNotRedirect.add(entity);
      entitiesWithPageNotRedirectWriter.print(entity + "\n");
      entitiesWithPageNotRedirectWriter.flush();
    }
    entitiesWithPageNotRedirectWriter.close();
    
    
    findEntitiesInWikidata();
    
    
    System.out.println("Total Number of Entities with No ID from DB: " + entitiesWithNoIdsFromDB.size());
    System.out.println("Number of Entities with page in the dump: " + entitiesWithPageInDump.size());
    System.out.println("Number of Redirect Pages: " + redirectsIdtoTitle.size());
    System.out.println("Number of entities that have page but are not redirects: " + entitiesWithPageNotRedirect.size());
    System.out.println("Number of entities that does not have page: " + (entitiesWithNoIdsFromDB.size()-entitiesWithPageInDump.size()));
    System.out.println("Number of entities that does have id (have page not redirect): " + entitiesWithPageNotRedirectWithWikidataID.size());
    System.out.println("Number of entities that does not have id (have page not redirect): " + (entitiesWithPageNotRedirect.size()-entitiesWithPageNotRedirectWithWikidataID.size()));
    
    // Write entities with no page in dump
     for(String entity:entitiesWithNoIdsFromDB) {
       if(entitiesWithPageInDump.contains(entity)) continue;
       entitiesWithNoPageWriter.print(entity + "\n");
       entitiesWithNoPageWriter.flush();
     }
     entitiesWithNoPageWriter.close();
    
     for (String entity:entitiesWithPageNotRedirect) {
       if(entitiesWithPageNotRedirectWithWikidataID.contains(entity)) continue;
       entitiesWithPageNotRedirectNoWikidataIDWriter.print(entity + "\n");
       entitiesWithPageNotRedirectNoWikidataIDWriter.flush();
     }
     entitiesWithPageNotRedirectNoWikidataIDWriter.close();
    
   
    
  }
  
  private static void findEntitiesInWikidata() {
    try {
      N4Reader nr = new N4Reader(new File(wikidataPath));
      while (nr.hasNext()) {
        Fact f = nr.next();
        if (f.getRelation().equals("<http://schema.org/about>")) {
          String id = FactComponent.stripBrackets(FactComponent.forWikipediaURL(f.getSubject()));
          if (entitiesWithPageNotRedirect.contains(id)) {
            entitiesWithPageNotRedirectWithWikidataID.add(id);
            entitiesWithWikidataIDWriter.print(id + " " + f.getObject() + "\n");
            entitiesWithWikidataIDWriter.flush();
          }
        }
      }
      nr.close();
      entitiesWithWikidataIDWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static void findRedirectPages() {
    try {
      BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipediaPath);
      while (FileLines.findIgnoreCase(in, "<title>") != -1 ) {
        String title = FileLines.readToBoundary(in, "</title>");
        String idFromTitle = title.replaceAll("[\\p{Zl}\\p{Zs}\\p{Zp}]+", "_");
        if (entitiesWithNoIdsFromDB.contains(idFromTitle)) {
          entitiesWithPageInDump.add(idFromTitle);
          String page = FileLines.readTo(in, "</page>").toString();
          Matcher matcher = redirectPattern.matcher(page);
          if (matcher.find()) {
            redirectsIdtoTitle.add(idFromTitle);
            // Write redirects in a file
            redirectWriter.print(idFromTitle + " " + matcher.group(1) + "\n");
            redirectWriter.flush();
          }
        }
      }
      in.close();
      redirectWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void readEntitiesWithNoId(String filePath, char seperator) {
    CSVReader reader = null;
    try {
      reader = new CSVReader(new FileReader(filePath), seperator);
      String[] line;
      reader.readNext();//skip name of columns 
      while ((line = reader.readNext()) != null) {
        String id = FactComponent.stripBrackets(line[3]);
        entitiesWithNoIdsFromDB.add(id);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
