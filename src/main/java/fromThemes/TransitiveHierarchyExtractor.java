package fromThemes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import utils.FactCollection;
import utils.Theme;

/**
 * Extracts locations in categories and writes the transitive hierarchy.
 * What is missing is the transformation of this structured information into
 * real class names.
 *
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Felix Keller.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/
public class TransitiveHierarchyExtractor extends Extractor {

  // === Settings
  // writes out the transitive locations for each category
  public static boolean WRITE_TRANSITIVE_CLOSURE = false;

  // writes out cats with no location (or more than one) as cat with no
  // location (HAS_NO_PART_OF_PROPERTY)
  public static boolean WRITE_CATS_WITHOUT_PROPERTY = false;

  /**
   * Classes deduced from categories with their connection to WordNet
   */
  public static final Theme TRANSITIVEHIERARCHY = new Theme("transitiveHierarchy", "The Transitive Hierarchy of things");

  @Override
  public Set<Theme> input() {
    return new HashSet<>(Arrays.asList(CategoryClassHierarchyExtractor.CATEGORYCLASSHIERARCHY.inEnglish(), CategoryMapper.CATEGORYFACTS.inEnglish(),
        TransitiveTypeExtractor.TRANSITIVETYPE));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(TRANSITIVEHIERARCHY);
  }

  protected static final String IS_LOCATED_IN = "<isLocatedIn>";

  private static final String HAS_PART_OF_PROPERTY = "<hasPartOfProperty>";

  private static final String HAS_NO_PART_OF_PROPERTY = "<hasNoPartOfProperty>";

  @Override
  public void extract() throws Exception {
    // Fabian: Don't do that, switching this on in a parallel system
    // will generate gigabytes of output because it influences the
    // other extractors
    // Announce.Level oldLevel = Announce.setLevel(Announce.Level.STATE);
    Announce.doing("Caching locations");
    FactCollection locationHierarchy = new FactCollection();
    Set<String> locations = new HashSet<>();
    for (Fact fact : CategoryMapper.CATEGORYFACTS.inEnglish()) {
      if (fact.getRelation().equals(IS_LOCATED_IN)) {
        locationHierarchy.add(fact);
        locations.add(fact.getSubject());
        locations.add(fact.getObject());
      }
    }
    locations.addAll(Name.nationality2country.values());
    Announce.done("done (Locations: " + locations.size() + ")");

    Announce.doing("Caching nonLocation entities");
    Set<String> nonLocationEntities = new HashSet<>();
    for (Fact fact : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (!locations.contains(fact.getSubject())) nonLocationEntities.add(fact.getSubject());
    }
    Announce.done("done (non Location Entities: " + nonLocationEntities.size() + ")");

    Map<String, Set<String>> transitiveProperties = new HashMap<>();
    for (Fact fact : CategoryClassHierarchyExtractor.CATEGORYCLASSHIERARCHY.inEnglish()) {
      transitiveProperties.put(fact.getSubject(), getPossibleTPs(fact.getSubject(), locations, nonLocationEntities));
    }

    // compute the transitive closure only when its needed
    FactCollection locationHierarchyTransitiveClosure = null;
    if (WRITE_TRANSITIVE_CLOSURE) locationHierarchyTransitiveClosure = computeTransitiveClosure(locationHierarchy);

    for (Entry<String, Set<String>> e : transitiveProperties.entrySet()) {
      // we don't want to have two locations for one category so only
      // categories with exactly one location are valid
      if (e.getValue().size() != 1) {
        if (WRITE_CATS_WITHOUT_PROPERTY) TRANSITIVEHIERARCHY.write(new Fact(e.getKey(), HAS_NO_PART_OF_PROPERTY, ""));
      } else {
        String possibleTP = e.getValue().iterator().next();
        TRANSITIVEHIERARCHY.write(new Fact(e.getKey(), HAS_PART_OF_PROPERTY, possibleTP));
        // write out the transitive locations when the option is set
        if (WRITE_TRANSITIVE_CLOSURE) for (Fact f : locationHierarchyTransitiveClosure.getFactsWithSubjectAndRelation(possibleTP, IS_LOCATED_IN))
          TRANSITIVEHIERARCHY.write(new Fact(e.getKey(), HAS_PART_OF_PROPERTY, f.getObject()));
      }
    }
    // Announce.setLevel(oldLevel);
  }

  protected static Set<String> getPossibleTPs(String category, Set<String> locations, Set<String> nonLocationEntities) {
    category = FactComponent.stripCat(category);

    Set<String> result = new HashSet<>();
    Stack<NounGroup> partsToCheck = new Stack<>();
    partsToCheck.push(new NounGroup(category));
    while (!partsToCheck.empty()) {
      NounGroup curNounGroup = partsToCheck.pop();
      // check if the current part of the category is an entity and
      // continue if yes
      if (nonLocationEntities.contains(FactComponent.forYagoEntity(curNounGroup.original()))) continue;
      // check if the whole NounGroup is a location
      String unifiedNounGroup = getUnifiedProperty(curNounGroup.original(), locations);
      if (unifiedNounGroup != null) {
        // if yes add this location and break;
        result.add(FactComponent.forYagoEntity(unifiedNounGroup));
        // don't split this group up because its already a location
        continue;
      }
      // check if the head is a location and add it to the result if true
      String unifiedHead = getUnifiedProperty(curNounGroup.head(), locations);
      if (unifiedHead != null) result.add(FactComponent.forYagoEntity(unifiedHead));

      // spit up the noun group to find every location possible.
      if (curNounGroup.preModifier() != null) partsToCheck.push(new NounGroup(curNounGroup.preModifier()));
      if (curNounGroup.postModifier() != null) partsToCheck.push(curNounGroup.postModifier());
    }
    return result;
  }

  private static String getUnifiedProperty(String possibleProperty, Set<String> locations) {
    // TODO: find a solutions for things like "the United States"
    if (possibleProperty == null) return null;

    // if it is a nationality we have to find the propert nation
    if (Name.isNationality(possibleProperty)) return Name.nationForNationality(possibleProperty);
    // if not we look it up in all locations
    if (locations.contains(FactComponent.forYagoEntity(possibleProperty))) return possibleProperty;

    return null;
  }

  /**
   * Computes the transitive closure of a graph
   * 
   * @return The transitive closure
   */
  protected FactCollection computeTransitiveClosure(FactCollection graph) {
    FactCollection subsequentFacts = new FactCollection();
    subsequentFacts.justAddAll(graph);
    FactCollection precedingFacts = subsequentFacts.getReverse();

    { // compute transitive closure
      HashSet<String> nodes = new HashSet<>(subsequentFacts.getSubjects());
      nodes.retainAll(precedingFacts.getSubjects());
      Announce.progressStart("Compute transitive closure", nodes.size());
      for (String nodeId : nodes) {
        Announce.progressStep();
        for (Fact subsequentFact : subsequentFacts.getFactsWithSubjectAndRelation(nodeId, IS_LOCATED_IN)) {
          for (Fact precedingFact : precedingFacts.getFactsWithSubjectAndRelation(nodeId, IS_LOCATED_IN)) {
            subsequentFacts.add(new Fact(precedingFact.getObject(), IS_LOCATED_IN, subsequentFact.getObject()));
            precedingFacts.add(new Fact(subsequentFact.getObject(), IS_LOCATED_IN, precedingFact.getObject()));
          }
        }
      }
    }
    Announce.progressDone();

    return subsequentFacts;
  }
}
