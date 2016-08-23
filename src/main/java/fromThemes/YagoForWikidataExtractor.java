package fromThemes;

import java.io.File;
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
  public static final Theme WIKIDATA_TAXONOMY_BRANCHES = new Theme("yagoForWikidataTaxonomyBranches",
      "A simplified rdf:type system. This theme contains all instances, and links them with the taxonomy branch (person, organization, building, location, artifact, abstraction, physicalEntity). Fictional entities have the type<wordnet_imaginary_being_109483738>",
      Theme.ThemeGroup.INTERNAL);

  /** The theme of simple types */
  public static final Theme WIKIDATATA_WORDNET_LEAVES = new Theme("yagoForWikidataWordnetLeaves",
      "A simplified rdf:type system. This theme contains all instances, and links them with the taxonomy branch (person, organization, building, location, artifact, abstraction, physicalEntity)",
      Theme.ThemeGroup.INTERNAL);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(WIKIDATA_TAXONOMY_BRANCHES, WIKIDATATA_WORDNET_LEAVES);
  }

  @Override
  public void extract() throws Exception {
    FactCollection branches = new FactCollection(), leaves = new FactCollection();
    FactCollection taxonomy = ClassExtractor.YAGOTAXONOMY.factCollection();
    Announce.doing("Loading YAGO types");
    for (Fact f : CoherentTypeExtractor.YAGOTYPES) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      String clss = f.getArg(2);
      if (clss.startsWith("<wikicategory")) clss = taxonomy.getObject(clss, RDFS.subclassOf);
      branches.add(new Fact(f.getArg(1), RDFS.type, clss));

      String yagoBranch = SimpleTypeExtractor.yagoBranch(clss, taxonomy);
      if (yagoBranch != null) {
        branches.add(new Fact(f.getArg(1), RDFS.type, yagoBranch));
      } else {
        Announce.message("no branch for " + f.getArg(1));
      }

      Set<String> superClasses = taxonomy.superClasses(clss);
      if (superClasses.contains("<wordnet_imaginary_being_109483738>")) {
        branches.add(new Fact(f.getArg(1), RDFS.type, "<wordnet_imaginary_being_109483738>"));
      }

      String wordnetLeafType = wordnetLeafType(clss, taxonomy);
      if (wordnetLeafType != null) {
        leaves.add(new Fact(f.getArg(1), RDFS.type, wordnetLeafType));
      }

    }
    Announce.done();

    Announce.doing("Writing types");
    for (Fact f : branches) {
      WIKIDATA_TAXONOMY_BRANCHES.write(f);
    }
    for (Fact f : leaves) {
      WIKIDATATA_WORDNET_LEAVES.write(f);
    }
    Announce.done();
    branches = null;
  }

  public static void main(String[] args) throws Exception {
    new YagoForWikidataExtractor().extract(new File("/san/suchanek/yago3/"), "test\n");
  }

  /**
   * Adds parent categories of cat to the set, i.e. follows "&lt;wikipediaSubCategoryOf&gt;" links
   */
  protected void wikiSuperCategories(String cat, Set<String> superCats, FactCollection fc) {
    wikiSuperCategories(cat, superCats, fc, null);
  }

  protected void wikiSuperCategories(String cat, Set<String> superCats, FactCollection fc, String prefix) {
    if (!superCats.add(cat)) {
      return;
    }
    /*if (prefix != null) {
      System.out.println(prefix + cat);
    }*/
    for (String s : fc.collectObjects(cat, CategoryClassHierarchyExtractor.WIKIPEDIA_RELATION)) {
      wikiSuperCategories(s, superCats, fc, prefix == null ? null : prefix + "  ");
    }
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
