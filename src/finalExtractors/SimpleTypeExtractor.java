package finalExtractors;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import extractors.Extractor;

public class SimpleTypeExtractor extends Extractor {

  /** Branches of YAGO, order matters! */
  public static final List<String> yagoBranches = Arrays.asList(YAGO.person, YAGO.organization, YAGO.building, YAGO.artifact, YAGO.location, YAGO.abstraction,
      YAGO.physicalEntity);

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TypeExtractor.YAGOTYPES, ClassExtractor.YAGOTAXONOMY);
  }

  /** The theme of simple types*/
  public static final Theme SIMPLETYPES = new Theme("yagoSimpleTypes",
      "A simplified rdf:type system. This theme contains all instances, and links them with rdf:type facts to the leaf level of WordNet. Use with yagoSimpleTaxonomy.", Theme.ThemeGroup.SIMPLETAX);

  /** Simple taxonomy */
  public static final Theme SIMPLETAXONOMY = new Theme("yagoSimpleTaxonomy",
      "A simplified rdfs:subClassOf taxonomy. This taxonomy contains just WordNet leaves, the main YAGO branches, and " + YAGO.entity
          + ". Use with " + SIMPLETYPES + ".", ThemeGroup.SIMPLETAX);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(SIMPLETYPES, SIMPLETAXONOMY);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactCollection types = new FactCollection();
    FactCollection taxonomy = new FactCollection(input.get(ClassExtractor.YAGOTAXONOMY));
    Set<String> leafClasses = new HashSet<>();
    Announce.doing("Loading YAGO types");
    for (Fact f : input.get(TypeExtractor.YAGOTYPES)) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      String clss = f.getArg(2);
      if (clss.startsWith("<wikicategory")) clss = taxonomy.getArg2(clss, RDFS.subclassOf);
      leafClasses.add(clss);
      types.add(new Fact(f.getArg(1), RDFS.type, clss));
    }
    Announce.done();

    Announce.doing("Writing types");
    for (Fact f : types) {
      output.get(SIMPLETYPES).write(f);
    }
    Announce.done();
    types = null;

    Announce.doing("Writing classes");
    for (String branch : yagoBranches)
      output.get(SIMPLETAXONOMY).write(new Fact(branch, RDFS.subclassOf, YAGO.entity));
    for (String clss : leafClasses) {
      String branch = yagoBranch(clss, taxonomy);
      if (branch == null) {
        //Announce.warning("No branch for", clss);
      } else {
        output.get(SIMPLETAXONOMY).write(new Fact(clss, RDFS.subclassOf, branch));
      }
    }
    Announce.done();

  }

  /** returns the super-branch that this class belongs to */
  public static String yagoBranch(String clss, FactCollection taxonomy) {
    Set<String> supr = taxonomy.superClasses(clss);
    for (String b : yagoBranches) {
      if (supr.contains(b)) return (b);
    }
    return (null);
  }

  public static void main(String[] args) throws Exception {
    new SimpleTypeExtractor().extract(new File("c:/fabian/data/yago2s"), "test\n");
  }

}
