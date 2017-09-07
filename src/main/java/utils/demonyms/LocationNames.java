package utils.demonyms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.FactComponent;

public class LocationNames {

  Map<String, Set<String>> locationToPart = new HashMap<>();

  Map<String, Set<String>> partToLocation = new HashMap<>();

  public static Pattern mainPartPattern = Pattern.compile(
      "(\\b(former|(culture|people|middle age|city|cities|civilization|caliphate|khanate|subdivision|empire|dynasty|kingdom|sultanate|khaganate)s?|of|the)\\b)|\\(.*\\)",
      Pattern.CASE_INSENSITIVE);

  protected void add(String location, String part) {
    locationToPart.computeIfAbsent(location, k -> new HashSet<>()).add(part);
    partToLocation.computeIfAbsent(part, k -> new HashSet<>()).add(location);
  }

  public void add(String location) {
    if (location == null) return;
    String title = FactComponent.stripCat(location);
    title = FactComponent.unYagoEntity(title);

    add(location, title);
    for (String part : getParts(Arrays.asList(title))) {
      add(location, part);
    }
  }

  private static Set<String> emptySet = new HashSet<>(0);

  private void remove(String location) {
    Set<String> parts = locationToPart.getOrDefault(location, emptySet);
    locationToPart.remove(location);
    for (String part : parts) {
      Set<String> locs = partToLocation.get(part);
      if (locs != null) {
        locs.remove(location);
      }
    }
  }

  private void remove(String location, String mainPart) {
    locationToPart.getOrDefault(location, new HashSet<>()).remove(mainPart);
    partToLocation.getOrDefault(mainPart, new HashSet<>()).remove(location);
  }

  public static List<? extends String> getParts(List<String> mainParts) {
    List<String> newParts = new ArrayList<>();
    for (String part : mainParts) {
      String[] candidates = LocationNames.mainPartPattern.split(part);
      for (String candidate : candidates) {
        candidate = candidate.trim();
        if (candidate.length() == 0) continue;
        part = candidate;
      }
      newParts.add(part);
    }
    return newParts;
  }

  public void populate() {

    String empires = DemonymPattern.getPage("List_of_empires", "en");

    for (List<String> row : WikiTable.getTables(empires).get(0).rows) {
      String location = row.get(0);
      Matcher m = DemonymPattern.wikiLink.matcher(location);
      if (m.find()) {
        String subject = m.group("page").replaceAll("#.*", "");
        add(FactComponent.forWikipediaTitle(subject));
      }
    }

    String antiquity = DemonymPattern.getPage("List_of_adjectival_and_demonymic_forms_of_place_names", "en");
    antiquity = antiquity.substring(antiquity.indexOf("Greco-Roman antiquity"));

    for (List<String> row : WikiTable.getTables(antiquity).get(0).rows) {
      String location = row.get(0);
      Matcher m = DemonymPattern.wikiLink.matcher(location);
      if (m.find()) {
        String subject = m.group("page").replaceAll("#.*", "");
        add(FactComponent.forWikipediaTitle(subject));
      }
    }

    Map<String, List<String>> demonyms = DemonymsPageExtractor.getLocationToDemonym();
    for (String loc : demonyms.keySet()) {
      if (loc == null) continue;
      String entity = FactComponent.forWikipediaTitle(loc);
      add(entity);
      for (String dem : demonyms.get(loc)) {
        add(entity, dem);
      }
    }

    // TODO: apply redirections
    // <United_States_of_America> -> <United_States>
    remove("<Americas>");
    remove("<Springfield_(The_Simpsons)>");
    remove("<Rome>");
    remove("<Macao>", "Chinese");
    remove("<Isle_of_Man>", "Man");
    remove("<Byzantium>", "Byzantine");
    remove("<Phoenix_(disambiguation)>");
    partToLocation.put("Roman Catholic", new HashSet<>(0));
    partToLocation.put("Roman-era", new HashSet<>(0));
    partToLocation.put("Phoenician", new HashSet<>(Arrays.asList("<Phoenicia>")));
  }

  private String print() {
    StringBuilder sb = new StringBuilder();

    for (String loc : locationToPart.keySet()) {
      sb.append(loc);
      sb.append(" --> ");
      sb.append(locationToPart.get(loc));
      sb.append("\n");
    }

    sb.append("--------------------------------------------------------------------------------");

    for (String part : partToLocation.keySet()) {
      sb.append(part);
      sb.append(" --> ");
      sb.append(partToLocation.get(part));
      sb.append("\n");
    }

    return sb.toString();
  }

  public void printToFile(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    out.write(print());
    out.close();
  }

  private static boolean accept(String cat, String part) {
    if (part == null) return false;
    cat = FactComponent.unYagoEntity(cat);
    if (cat.contains(part)) {
      Pattern p = Pattern.compile("\\b" + part + "\\b");
      if (p.matcher(cat).find()) {
        return true;
      }
    }
    return false;
  }

  public List<String> locations(String wikicat) {
    Map<String, Set<String>> partToLoc = new HashMap<>();
    for (String part : partToLocation.keySet()) {
      if (accept(wikicat, part)) {
        partToLoc.put(part, partToLocation.get(part));
      }
    }

    // check whether part contains the other
    // e.g.: if both "Holy Roman", and "Roman" are found, only add "Holy Roman"
    List<String> parts = new ArrayList<>(partToLoc.keySet());
    Set<String> result = new HashSet<>();
    outer: for (int i = 0; i < parts.size(); i++) {
      for (int j = 0; j < parts.size(); j++) {
        if (i != j && accept(parts.get(j), parts.get(i))) {
          continue outer;
        }
      }
      result.addAll(partToLoc.get(parts.get(i)));
    }
    return new ArrayList<>(result);
  }

  public static void main(String[] args) throws IOException {
    LocationNames ln = new LocationNames();
    //System.out.println(ln.print());

    ln.populate();
    System.out.println(ln.locationToPart.get("<Holy_Roman_Empire>"));
    System.out.println(ln.partToLocation.get("Phoenician"));

    //ln.printToFile("data/locations/locationsToMainPart.txt");

    System.out.println(ln.locations("<wikicat_14th-century_Roman_Catholic_bishops"));
  }

}
