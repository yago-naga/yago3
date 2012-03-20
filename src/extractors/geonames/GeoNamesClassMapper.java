package extractors.geonames;

import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor;
import extractors.PatternHardExtractor;
import extractors.WordnetExtractor;

/**
 * The GeoNamesClassMapper maps geonames classes to WordNet synsets.
 * 
 * Needs the GeoNames featureCodes_en.txt as input.
 * 
 * @author Johannes Hoffart
 *
 */
public class GeoNamesClassMapper extends Extractor {

  private File geonamesFeatureCodes;
  
  /** YAGO geo class */
  public static final String GEO_CLASS = "<yagoGeoEntity>";
  
  private Set<String> geographicalWordNetClasses;
  private BreakIterator bi = BreakIterator.getWordInstance();
  private Pattern NON_WORD_CHAR = Pattern.compile("^[^\\w]*$");

  /** geonames class links */
  public static final Theme GEONAMESCLASSSIDS = new Theme("geonamesClassIds", "IDs from GeoNames classes");
    /** geonames classes */
  public static final Theme GEONAMESCLASSES = new Theme("geonamesClasses", "Classes from GeoNames");
   /** geonames glosses */
  public static final Theme GEONAMESGLOSSES = new Theme("geonamesGlosses", "Glosses from GeoNames");
  
  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(
        WordnetExtractor.WORDNETGLOSSES, WordnetExtractor.WORDNETWORDS, WordnetExtractor.WORDNETCLASSES, PatternHardExtractor.HARDWIREDFACTS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(GEONAMESCLASSES, GEONAMESGLOSSES, GEONAMESCLASSSIDS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    geographicalWordNetClasses = new HashSet<String>();
    FactCollection hardFacts = new FactCollection(input.get(PatternHardExtractor.HARDWIREDFACTS));
    for (Fact f : hardFacts.getBySecondArgSlow(RDFS.subclassOf, GEO_CLASS)) {
      geographicalWordNetClasses.add(f.getArg(1));
    }
    
    FactCollection wordnetWords = new FactCollection(input.get(WordnetExtractor.WORDNETWORDS));
    FactCollection wordnetGlosses = new FactCollection(input.get(WordnetExtractor.WORDNETGLOSSES));
    FactCollection wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
    
    for (String line : new FileLines(geonamesFeatureCodes, "Loading feature code mappings")) {
      String[] data = line.split("\t");

      String featureId = data[0];
      String featureClass = data[1].replaceAll("\\(.*\\)", "").trim().toLowerCase();

      String featureGloss = null;
      if (data.length > 2) {
        featureGloss = data[2];
      }
      
      String geoClass = FactComponent.forGeoNamesClass(featureClass);
      String wordNetClass = mapGeonamesCategory(featureClass, featureGloss, wordnetWords, wordnetGlosses, wordnetClasses);
      String parentClass = (wordNetClass != null) ? wordNetClass : GEO_CLASS;

      output.get(GEONAMESCLASSES).write(new Fact(null, geoClass, RDFS.subclassOf, parentClass));
      output.get(GEONAMESCLASSSIDS).write(new Fact(null, geoClass, "<hasGeonamesClassId>", FactComponent.forString(featureId)));

      if (featureGloss != null) {
        // there is a gloss
        Fact gloss = new Fact(null, geoClass, "<hasGloss>", FactComponent.forString(data[2]));
        output.get(GEONAMESGLOSSES).write(gloss);
      }
    }
  }
  
  private String mapGeonamesCategory(String cat, String gloss, FactCollection wordnetWords, FactCollection wordnetGlosses, FactCollection wordnetClasses) throws IOException {
    NounGroup category = new NounGroup(cat.toLowerCase());
    String stemmedHead = PlingStemmer.stem(category.head());

    List<String> wordnetMeanings = null;
    String lookup = FactComponent.forString(category.preModifier() + ' ' + stemmedHead);
    
    // Try premodifier + head, if no results, only head
    
    if (category.preModifier() != null) {
      wordnetMeanings = getAllMeanings(lookup, wordnetWords);
    }
    
    if (wordnetMeanings == null || wordnetMeanings.size() == 0) {
      lookup = FactComponent.forString(stemmedHead);
      
      wordnetMeanings = getAllMeanings(lookup, wordnetWords);
    }
   
    String preferredMeaning = null;
    List<Fact> preferredLabels = wordnetWords.getBySecondArgSlow("skos:prefLabel", lookup);
    if (preferredLabels.size() > 0) {
      preferredMeaning = preferredLabels.get(0).getArg(1);
    }
    String wordnet = getBestMeaning(cat, gloss, preferredMeaning, wordnetMeanings, wordnetGlosses, wordnetClasses);
    if (wordnet != null && wordnet.startsWith("<wordnet_")) return (wordnet);
    Announce.debug("Could not find type in", category, "(no wordnet match)");
    return (null);
  }

  private String getBestMeaning(String word, String gloss, String preferredMeaning, List<String> meanings, FactCollection wnGlosses, FactCollection wnClasses) {
    if (meanings.size() == 0) {
      return null;
    }

    // 1. check if only one of the possible meanings has a geographical sense
    Set<String> geoMeanings = new HashSet<String>();

    for (String meaning : meanings) {
      Set<String> ancs = wnClasses.superClasses(meaning);

      ancs.retainAll(geographicalWordNetClasses);

      if (ancs.size() > 0) {
        geoMeanings.add(meaning);
      }
    }

    if (geoMeanings.size() == 1) {
      return geoMeanings.iterator().next();
    } else if (geoMeanings.size() > 1) {
      // remove all non-geo-meanings - this improves precision in the later
      // token-based mapping
      meanings.retainAll(geoMeanings);
    }

    // 2. if 0 or >1 meanings are geoMeanings, fall back to gloss token overlap
    if (gloss != null) {
      Set<String> glossTokens = getTokensForGloss(gloss);

      double maxOverlap = 0.0;
      String bestMatch = null;

      for (String meaning : meanings) {
        String wnGloss = FactComponent.asJavaString(wnGlosses.getArg2(meaning, "<hasGloss>"));
        Set<String> wnGlossTokens = getTokensForGloss(wnGloss);

        //        for (String synonym : Basics.facts.getArg1s("means", meaning)) {
        //          wnGlossTokens.add(PlingStemmer.stem(Normalize.unString(synonym)));
        //        }

        double overlap = calcJaccardSimilarity(glossTokens, wnGlossTokens);

        if (overlap > maxOverlap) {
          bestMatch = meaning;
          maxOverlap = overlap;
        }
      }

      if (bestMatch != null) {
        return bestMatch;
      }
    }

    // 3. fallback -> return preferred meaning if it has geo meaning
    if (preferredMeaning != null) {
      Set<String> preferredMeaningAncs = wnClasses.superClasses(preferredMeaning);

      preferredMeaningAncs.retainAll(geographicalWordNetClasses);

      if (preferredMeaningAncs.size() > 0) {
        return preferredMeaning;
      }
    }

    // 4. last fallback - just return yagoGeoEntity as most general location class
    return GEO_CLASS;
  }

  private Set<String> getTokensForGloss(String gloss) {
    Set<String> tokens = new HashSet<String>();

    bi.setText(gloss);

    int start = bi.first();
    for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
      String token = gloss.substring(start, end);

      Matcher m = NON_WORD_CHAR.matcher(token);

      if (!m.find() && !Stopwords.isStopword(token)) {
        tokens.add(token);
      }
    }

    return tokens;
  }

  private double calcJaccardSimilarity(Set<String> glossTokens, Set<String> wnGlossTokens) {
    Set<String> union = new HashSet<String>(glossTokens);
    union.addAll(wnGlossTokens);
    
    Set<String> intersection = new HashSet<String>(glossTokens);
    intersection.retainAll(wnGlossTokens);
    
    double jaccardSim = (double) intersection.size() / (double) union.size();

    return jaccardSim;
  }
  
  private List<String> getAllMeanings(String lookup, FactCollection wordnetWords) {
    List<String> meanings = new LinkedList<String>();
    
    List<Fact> labels = wordnetWords.getBySecondArgSlow(RDFS.label, lookup);
    for (Fact f : labels) {
      meanings.add(f.getArg(1));
    }
    
    return meanings;
  }
  
  public GeoNamesClassMapper(File geonamesFeatureCodes) {
    this.geonamesFeatureCodes = geonamesFeatureCodes;
  }
}
