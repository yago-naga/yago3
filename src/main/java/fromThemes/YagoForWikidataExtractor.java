package fromThemes;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import deduplicators.ClassExtractor;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * YAGO2s - YAGO for Wikidata
 *
 * Produces a simplified taxonomy of just 3 layers.
 *
 * @author Thomas Rebele
 *
 */

public class YagoForWikidataExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CoherentTypeExtractor.YAGOTYPES, ClassExtractor.YAGOTAXONOMY);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY);
  }

  /** The theme of simple types */
  public static final Theme WIKIDATATYPES = new Theme("yagoForWikidata",
      "A simplified rdf:type system. This theme contains all instances, and links them with rdf:type facts to the leaf level of WordNet (use with yagoSimpleTaxonomy)",
      Theme.ThemeGroup.INTERNAL);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(WIKIDATATYPES);
  }

  @Override
  public void extract() throws Exception {

    System.out.println(WIKIDATATYPES.file().getAbsolutePath());

    FactCollection types = new FactCollection();
    FactCollection taxonomy = ClassExtractor.YAGOTAXONOMY.factCollection();
    Set<String> leafClasses = new HashSet<>();
    Announce.doing("Loading YAGO types");
    for (Fact f : CoherentTypeExtractor.YAGOTYPES) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      String clss = f.getArg(2);
      if (clss.startsWith("<wikicategory")) clss = taxonomy.getObject(clss, RDFS.subclassOf);
      leafClasses.add(clss);
      types.add(new Fact(f.getArg(1), RDFS.type, clss));

      String yagoBranch = SimpleTypeExtractor.yagoBranch(clss, taxonomy);
      if (yagoBranch != null) {
        types.add(new Fact(f.getArg(1), RDFS.type, yagoBranch));
      } else {
        Announce.message("no branch for " + f.getArg(1));
      }

      String wordnetLeafType = wordnetLeafType(clss, taxonomy);
      if (wordnetLeafType != null) {
        types.add(new Fact(f.getArg(1), RDFS.type, wordnetLeafType));
      }
    }
    Announce.done();

    Announce.doing("Writing types");
    for (Fact f : types) {
      WIKIDATATYPES.write(f);
    }
    Announce.done();
    types = null;
  }

  public static void main(String[] args) throws Exception {
    new YagoForWikidataExtractor().extract(new File("/san/suchanek/yago3/"), "test\n");
  }

  public static String wordnetLeafType(String clss, FactCollection fc) {
    if (FactComponent.wordnetWord(clss) != null) return clss;
    Set<String> set = fc.collectObjects(clss, RDFS.subclassOf);
    for (String supertype : set) {
      if (FactComponent.wordnetWord(clss) != null) return supertype;
    }
    for (String supertype : set) {
      String wordnetType = wordnetLeafType(supertype, fc);
      if (wordnetType != null) return wordnetType;
    }
    return null;
  }
}
