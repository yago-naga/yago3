package deduplicators;

import java.util.Set;

import basics.Fact;
import basics.YAGO;
import extractors.Extractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.datatypes.FinalSet;
import utils.EntityType;
import utils.Theme;

public class AllNamedEntitiesDumpThemeExtractor extends Extractor {

  public static final Theme ALLNAMEDENTITIES = new Theme("allNamedEntities",
      "List of all named entities with the isNamedEntity relation as a dump theme for later use.");
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(ALLNAMEDENTITIES);
  }

  @Override
  public void extract() throws Exception {
    for (String entity:TransitiveTypeExtractor.TRANSITIVETYPE.factCollection().getSubjects()) {
      ALLNAMEDENTITIES.write(new Fact(entity, YAGO.isNamedEntity, EntityType.NAMED_ENTITY.getYagoName()));
    }
  }

}
