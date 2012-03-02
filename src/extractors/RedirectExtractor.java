package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractorUtils.TitleExtractor;

/**
 * Takes the facts from the InfoboxExtractor and checks if
 * any of the entities are actually a redirect and resolves
 * them
 * 
 * @author Johannes Hoffart
 *
 */
public class RedirectExtractor extends Extractor {

  /** Input file */
  private File wikipedia;

  private static final Pattern pattern = Pattern.compile("#REDIRECT ?\\[\\[(.*?)\\]\\]");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(InfoboxExtractor.DIRTYINFOBOXFACTS, PatternHardExtractor.TITLEPATTERNS));
  }

  /** Redirected Infobox facts, non-checked */
  public static final Theme REDIRECTEDINFOBOXFACTS = new Theme("redirectedInfoxboxFacts");

  @Override
  public Map<Theme, String> output() {
    return new FinalMap<Theme, String>(REDIRECTEDINFOBOXFACTS,
        "Facts extracted from the Wikipedia infoboxes with redirects resolved - still to be type-checked");
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    // Extract the information
    Announce.doing("Extracting Redirects");
    Map<String, String> redirects = new HashMap<>();

    BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    TitleExtractor titleExtractor = new TitleExtractor(input);

    String titleEntity = null;
    redirect: while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "#REDIRECT")) {
        case -1:
          Announce.done();
          in.close();
          break redirect;
        case 0:
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        default:
          if (titleEntity == null) continue;
          String redirect = FileLines.readTo(in, "]]").toString().trim();
          String redirectTarget = getRedirectTarget(redirect);

          if (redirectTarget != null) {
            redirects.put(titleEntity, redirectTarget);
          }
      }
    }
    
    FactWriter out = output.get(REDIRECTEDINFOBOXFACTS);
    
    FactSource dirtyInfoboxFacts = input.get(InfoboxExtractor.DIRTYINFOBOXFACTS);
    
    Announce.doing("Applying redirects to Infobox facts");
    
    for (Fact dirtyFact : dirtyInfoboxFacts) {
      Fact redirectedDirtyFact = redirectArguments(dirtyFact, redirects);
      out.write(redirectedDirtyFact);
    }
    Announce.done();
  }

  private String getRedirectTarget(String redirect) {
    Matcher m = pattern.matcher(redirect);

    if (m.find()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  private Fact redirectArguments(Fact dirtyFact, Map<String, String> redirects) {
    String redirectedArg1 = dirtyFact.getArg(1);
    if (redirects.containsKey(dirtyFact.getArg(1))) {
      redirectedArg1 = redirects.get(dirtyFact.getArg(1));
    }
    
    String redirectedArg2 = dirtyFact.getArg(2);
    if (redirects.containsKey(dirtyFact.getArg(2))) {
      redirectedArg2 = redirects.get(dirtyFact.getArg(2));
    }
    
    Fact redirectedFact = new Fact(dirtyFact.getId(), redirectedArg1, dirtyFact.getRelation(), redirectedArg2, dirtyFact.getdataType());
        
    return redirectedFact;
  }

  /**
   * Needs Wikipedia as input
   * 
   * @param wikipedia Wikipedia XML dump
   */
  public RedirectExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

}
