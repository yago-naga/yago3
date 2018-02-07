package utils;

import java.util.Arrays;
import java.util.List;

public enum EntityType {
  NAMED_ENTITY("NAMED_ENTITY", "TRUE", "<true>"),
  CONCEPT("CONCEPT", "FALSE", "<false>"),
  BOTH("BOTH", "<both>"),
  UNKNOWN("UNKNOWN", "<unknown>");
  
  List<String> values;
  EntityType (String ...values) {
    this.values = Arrays.asList(values);
  }
  
  public static EntityType find(String name) {
    for (EntityType type : EntityType.values()) {
        if (type.getValues().contains(name)) {
            return type;
        }
    }
    return null;
  }
  
  public String getYagoName() {
    switch (this) {
      case NAMED_ENTITY:
        return "<true>";
      case CONCEPT:
        return "<false>";
      case BOTH:
        return "<both>";
      default:
        return "<unknown>";
    }
  }

  private List<String> getValues() {
    return values;
  }
}
