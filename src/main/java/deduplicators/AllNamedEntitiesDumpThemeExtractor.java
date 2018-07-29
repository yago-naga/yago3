package deduplicators;

import basics.Fact;
import basics.YAGO;
import extractors.Extractor;
import fromThemes.TransitiveTypeSubgraphExtractor;
import javatools.datatypes.FinalSet;
import utils.EntityType;
import utils.Theme;

import java.util.Set;

public class AllNamedEntitiesDumpThemeExtractor extends Extractor {

  public static final Theme ALLNAMEDENTITIES = new Theme("allNamedEntities",
      "List of all named entities with the isNamedEntity relation as a dump theme for later use.");
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TransitiveTypeSubgraphExtractor.YAGOTRANSITIVETYPE);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(ALLNAMEDENTITIES);
  }

  @Override
  public void extract() throws Exception {
    for (String entity : TransitiveTypeSubgraphExtractor.YAGOTRANSITIVETYPE.factCollection().getSubjects()) {
      ALLNAMEDENTITIES.write(new Fact(entity, YAGO.isNamedEntity, EntityType.NAMED_ENTITY.getYagoName()));
    }
  }
}