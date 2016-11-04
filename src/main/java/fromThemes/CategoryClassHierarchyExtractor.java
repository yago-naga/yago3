package fromThemes;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.RDFS;
import basics.YAGO;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryHierarchyExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import utils.FactCollection;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * Extracts the hierarchy of Wikipedia categories, matching them to WordNet on the coarsest granular level possible.
 * This means that we extract for example:
 *
 * Clothing Companies from Italy -> Cothing Comapnies -> Companies
 * from Wikipedia categories, them natch Companies to the proper WordNet synset.
 *
 * TODO this is multilingual but the heuristics only work for English - probably a mistake!
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
public class CategoryClassHierarchyExtractor extends MultilingualExtractor {

  // === Settings:
  // allows only cat A to be a subcat of cat B if the generated calss of A is a subcalss of the generated class of B
  public static boolean USE_WORDNET_SUBCLASS_FOR_FACT_REMOVAL = true; // default = true

  public static boolean USE_ONLY_COUNTING_FOR_BAD_FACT_REMOVAL = false; // default = false

  public static boolean FORCE_COUNTING_FOR_BAD_CATS = true; // default = true

  public static boolean REMOVE_C2C_NULLS = true; // default = true

  public static final MultilingualTheme CATEGORYCLASSHIERARCHY = new MultilingualTheme("categoryClassHierarchy",
      "Wikipedia category hierarchy combined with the WordNet hierarchy, still to be translated");

  public static final MultilingualTheme CATEGORYCLASSHIERARCHY_TRANSLATED = new MultilingualTheme("categoryClassHierarchyTranslated",
      "Wikipedia category hierarchy combined with the WordNet hierarchy translated subjects and objects");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<>(
        Arrays.asList(CategoryClassExtractor.CATEGORYCLASSES, WordnetExtractor.WORDNETWORDS, PatternHardExtractor.CATEGORYPATTERNS,
            CoherentTypeExtractor.YAGOTYPES, WordnetExtractor.WORDNETCLASSES, WordnetExtractor.PREFMEANINGS, HardExtractor.HARDWIREDFACTS));
    if (isEnglish()) result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY.inLanguage(language));
    else result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY_TRANSLATED.inLanguage(language));
    return result;
  }

  @Override
  public Set<Theme> inputCached() {
    Set<Theme> result = new TreeSet<>(Arrays.asList(CategoryClassExtractor.CATEGORYCLASSES, WordnetExtractor.WORDNETCLASSES));
    if (isEnglish()) result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY.inLanguage(language));
    else result.add(CategoryHierarchyExtractor.CATEGORYHIERARCHY_TRANSLATED.inLanguage(language));
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(CATEGORYCLASSHIERARCHY.inLanguage(language));
  }

  public static final String WORDNET_RELATION = RDFS.subclassOf;

  // Write everything as subclassOf, everything is a class.
  public static final String WIKIPEDIA_RELATION = RDFS.subclassOf;
  //  public static final String WIKIPEDIA_RELATION = "<wikipediaSubCategoryOf>";

  private PrintWriter debugWriter_removeBadCats;

  private PrintWriter debugWriter_removeBadFacts;

  @Override
  public void extract() throws Exception {
    // Fabian: Don't do that, switching this on in a parallel system
    // will generate gigabytes of output because it influences the
    // other extractors 
    //Announce.Level oldLevel = Announce.setLevel(Announce.Level.DEBUG);
    Announce.doing("Extracting the CategoryClassHierarchy");
    //		debugWriter_removeBadCats = new PrintWriter(new File("debugOutput_removeBadCats.txt"));
    //		debugWriter_removeBadFacts = new PrintWriter(new File("debugOutput_removeBadFacts.txt"));
    Map<String, String> preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings();

    Set<String> nonConceptualCategories = new HashSet<>();
    for (Fact fact : PatternHardExtractor.CATEGORYPATTERNS) {
      if (fact.getObject().equals("<_yagoNonConceptualWord>") && fact.getRelation().equals(RDFS.type))
        nonConceptualCategories.add(FactComponent.asJavaString(fact.getSubject()));
    }
    Set<String> wordnetWords = new HashSet<>();
    for (Fact fact : WordnetExtractor.WORDNETWORDS) {
      if (fact.getRelation().equals(RDFS.label)) wordnetWords.add(FactComponent.asJavaString(fact.getObject()));
    }

    IndexedGraph indexedCategoryHierarchy;
    IndexedGraph indexedCategoryClasses;
    IndexedGraph indexedClassHierarchy;
    Index<String> index;
    {
      Theme categoryClasses = CategoryClassExtractor.CATEGORYCLASSES;
      Theme wordnetClasses = WordnetExtractor.WORDNETCLASSES;
      Theme categoryHierarchy;
      if (isEnglish()) categoryHierarchy = CategoryHierarchyExtractor.CATEGORYHIERARCHY.inLanguage(language);
      else categoryHierarchy = CategoryHierarchyExtractor.CATEGORYHIERARCHY_TRANSLATED.inLanguage(language);

      index = new Index<>();

      Announce.doing("Caching category classes");
      indexedCategoryClasses = new IndexedGraph();
      for (Fact fact : categoryClasses) {
        if (fact.getRelation().equals(WORDNET_RELATION) && !fact.getSubject().equals(fact.getObject())) {
          index.addHigh(fact.getSubject()); // is wikiCat so add high
          index.addLow(fact.getObject()); // is wnClass so add low
          indexedCategoryClasses.put(fact, index);
        }
      }
      Announce.done();

      Announce.doing("Caching category hierarchy");
      indexedCategoryHierarchy = new IndexedGraph();
      for (Fact fact : categoryHierarchy) {
        if (!fact.getSubject().equals(fact.getObject())) {
          index.addHigh(fact.getSubject()); // is wikiCat so add high
          index.addHigh(fact.getObject()); // is wikiCat so add high
          indexedCategoryHierarchy.put(fact, index);
        }
      }
      Announce.done();

      Announce.doing("Caching class hierarchy");
      indexedClassHierarchy = new IndexedGraph();
      for (Fact fact : wordnetClasses) {
        if (!fact.getSubject().equals(fact.getObject())) {
          index.addLow(fact.getSubject()); // is wnClass so add low
          index.addLow(fact.getObject()); // is wnClass so add low
          indexedClassHierarchy.put(fact, index);
        }
      }
      List<Fact> factsToCheck = new ArrayList<>(HardExtractor.HARDWIREDFACTS.factCollection().getFactsWithRelation(WORDNET_RELATION));
      FactCollection notMatchingFacts = new FactCollection();
      for (int i = 0; i < factsToCheck.size(); i++) {
        Fact fact = factsToCheck.get(i);
        if ((index.contains(fact.getSubject()) || index.contains(fact.getObject())) && !fact.getSubject().equals(fact.getObject())) {
          index.addLow(fact.getSubject()); // is wnClass so add low
          index.addLow(fact.getObject()); // is wnClass so add low
          indexedClassHierarchy.put(fact, index);
          factsToCheck.addAll(notMatchingFacts.getFactsWithSubjectAndRelation(fact.getSubject(), WORDNET_RELATION));
          factsToCheck.addAll(notMatchingFacts.getFactsWithSubjectAndRelation(fact.getObject(), WORDNET_RELATION));
          factsToCheck.addAll(notMatchingFacts.seekFactsWithRelationAndObject(WORDNET_RELATION, fact.getSubject()));
          factsToCheck.addAll(notMatchingFacts.seekFactsWithRelationAndObject(WORDNET_RELATION, fact.getObject()));
        } else notMatchingFacts.add(fact);
      }
      Announce.done();
    }

    IndexedGraph graph;

    // get the raw Wikipedia hierarchy
    graph = getRawClassCategoryHierarchy(indexedCategoryClasses, indexedCategoryHierarchy);
    indexedCategoryClasses = null;
    indexedCategoryHierarchy = null;

    // remove all bad facts
    graph = removeBadFacts(graph, index, indexedClassHierarchy, nonConceptualCategories, preferredMeanings, wordnetWords);
    indexedClassHierarchy = null;

    // remove all bad categories decided by the "isGoodCategory" function
    graph = removeBadCategories(graph, index, wordnetWords, nonConceptualCategories, preferredMeanings);

    // break all cycles
    breakCyclesNoSinks(graph);

    // remove all transitive edges
    removeUnnecessaryTransitiveEdges(graph, index);

    if (!graph.equals(cleanGraph(graph))) Announce.warning("Graph has not connected parts");

    FactCollection result = graph.toFactCollection(index);
    //		printGraphInfo(result, true);

    // writing
    for (Fact f : result) {
      CATEGORYCLASSHIERARCHY.inLanguage(language).write(f);
    }

    if (debugWriter_removeBadCats != null) {
      debugWriter_removeBadCats.flush();
      debugWriter_removeBadCats.close();
    }
    if (debugWriter_removeBadFacts != null) {
      debugWriter_removeBadFacts.flush();
      debugWriter_removeBadFacts.close();
    }

    Announce.done();
    //Announce.setLevel(oldLevel);
  }

  //====================
  //=== Functions to combine the Wikipedia category graph and the WordNet class graph leaves
  //

  /**
   * Combines the Wikipedia categories with the WordNet class category connection.
   * <ul>
   * <li>The Wikipedia WordNet connections are extracted by using the {@link fromThemes.CategoryClassExtractor#CATEGORYCLASSES}</li>
   * <li>The Wikipedia categorys hierarchy is extracted by using {@link fromWikipedia.CategoryHierarchyExtractor#CATEGORYHIERARCHY}</li>
   * </ul>
   *
   * @return The combination of the two graphs.
   */
  protected IndexedGraph getRawClassCategoryHierarchy(IndexedGraph indexedCategoryClasses, IndexedGraph indexedCategoryHierarchy) throws Exception {
    Announce.doing("Get relevant Wikipedia subgraph");
    IndexedGraph result;

    // reverse the category classes and the category hierarchy to get better access
    IndexedGraph indexedCategoryClassesReversed = indexedCategoryClasses.getReversed();
    IndexedGraph indexedCategoryHierarchyReversed = indexedCategoryHierarchy.getReversed();

    Announce.doing("Getting subgraph");
    IndexedGraph subGraphReversed = new IndexedGraph();
    {
      Set<Integer> currentFacts = indexedCategoryClasses.keySet();

      for (Integer wpCat : currentFacts) {
        if (indexedCategoryHierarchyReversed.containsKey(wpCat)) subGraphReversed.put(wpCat, indexedCategoryHierarchyReversed.get(wpCat));
      }

      // go down the Wikipedia hierarchy and save all categories
      while (!currentFacts.isEmpty()) {
        Set<Integer> nextFacts = new HashSet<>();
        for (Integer curNodeId : currentFacts) {
          Set<Integer> factsToCheck = indexedCategoryHierarchyReversed.get(curNodeId);
          if (factsToCheck == null) continue;
          for (Integer curFactToCheck : factsToCheck) {
            // check if the fact has already been processed if not adding it to the queue
            if (!subGraphReversed.containsKey(curFactToCheck)) nextFacts.add(curFactToCheck);
          }
        }

        // adding the result of this run to a collection and prepare the other variables
        for (Integer nodeId : nextFacts)
          if (indexedCategoryHierarchyReversed.containsKey(nodeId)) subGraphReversed.put(nodeId, indexedCategoryHierarchyReversed.get(nodeId));

        currentFacts = nextFacts;
      }

    }
    Announce.done();

    Announce.progressStart("Getting subcatregory roots", indexedCategoryClassesReversed.keySet().size());
    {
      IndexedGraph subgraphRoots = new IndexedGraph();

      for (Integer wnClass : indexedCategoryClassesReversed.keySet()) {
        Announce.progressStep();
        Set<Integer> currentFacts = indexedCategoryClassesReversed.get(wnClass);
        Set<Integer> possibleRoots = new HashSet<>(currentFacts);
        Set<Integer> alreadyChecked = new HashSet<>();
        // go down the Wikipedia hierarchy again
        while (!currentFacts.isEmpty() && possibleRoots.size() > 1) {
          Set<Integer> nextFacts = new HashSet<>();
          for (Integer curFact : currentFacts) {
            Set<Integer> factsToCheck = indexedCategoryHierarchyReversed.get(curFact);
            if (factsToCheck == null) continue;
            for (Integer curFactToCheck : factsToCheck) {
              // check if the fact has already been processed if not adding it to the queue
              if (!alreadyChecked.contains(curFactToCheck)) nextFacts.add(curFactToCheck);

              // because a cycle could remove all categories i have to left at least one in the collection
              if (possibleRoots.size() == 1) break;

              // if i encounter a category that's usually directly below the wordnet classes i remove it from the list
              // because it is reachable from the node i started from via wikipedia categories
              possibleRoots.remove(curFactToCheck);
            }
            if (possibleRoots.size() == 1) break;
          }

          // adding the result of this run to a collection and prepare the other variables
          alreadyChecked.addAll(nextFacts);
          currentFacts = nextFacts;
        }

        // adding the possible roots that are left as roots to the roots collection
        subgraphRoots.put(wnClass, possibleRoots);
      }

      // join the roots and the subgraph
      result = new IndexedGraph();
      result.putAll(subGraphReversed);
      result.putAll(subgraphRoots);

    }
    Announce.progressDone();

    // join the roots and the subgraph
    result = new IndexedGraph();
    result.putAll(subGraphReversed);
    result.putAll(indexedCategoryClasses.getReversed());

    Announce.done("done (Graph edges: " + result.size() + ")");

    return result.getReversed();
  }

  //====================
  //=== Functions and classes to remove the bad bad categories from the graph
  //

  /**
   * Removes the bad categories from a graph.
   * If a subgraph is cut off by removing a bad node the root of this subgraph is attached to the Wordnet Class
   * determined by the {@link fromThemes.CategoryClassHierarchyExtractor#category2class} function.
   *
   * @return The graph without the bad categories
   */
  protected IndexedGraph removeBadCategories(IndexedGraph hierarchy, Index<String> index, Set<String> wordnetWords,
      Set<String> nonConceptualCategories, Map<String, String> preferredMeanings) {
    Announce.doing("Removing bad categories");
    // get the bad categories
    Announce.progressStart("Getting bad categories", hierarchy.keySet().size());
    Set<Integer> badCategories = new HashSet<>();
    for (Integer category : hierarchy.keySet()) {
      Announce.progressStep();
      // a category is bad if the isGoodCategory function is false
      if (!isGoodCategory(index.get(category), wordnetWords, nonConceptualCategories)) {
        badCategories.add(category);
        if (debugWriter_removeBadCats != null) debugWriter_removeBadCats.println("Is bad category: " + index.get(category));
        // or if the category2class functions is null
      } else if (REMOVE_C2C_NULLS && category2class(index.get(category), nonConceptualCategories, preferredMeanings) == null) {
        badCategories.add(category);
        if (debugWriter_removeBadCats != null) debugWriter_removeBadCats.println("category2class is null: " + index.get(category));
      } else if (debugWriter_removeBadCats != null) debugWriter_removeBadCats.println("Is good category: " + index.get(category));
    }
    Announce.progressDone();
    Announce.debug("bad categories: " + badCategories.size());

    IndexedGraph successorNodes = new IndexedGraph(hierarchy);
    IndexedGraph predecessorNodes = successorNodes.getReversed();

    // removing all bad categories
    Announce.progressStart("Removing the bad nodes", badCategories.size());
    {
      //			int count = 0;
      // for every bed category
      for (Integer badCategoryId : badCategories) {
        Announce.progressStep();
        //				System.out.println("Bad cat: " + ++count + " / " + badCategories.size() + " | " + successorNodes.size());
        // check if there are no predecessorNodes for the bad node
        if (!predecessorNodes.keySet().contains(badCategoryId))
          // remove the bad node from the predecessorNodes of the bad category's successorNodes
          for (Integer successorNodeId : successorNodes.get(badCategoryId))
          predecessorNodes.remove(successorNodeId, badCategoryId);
        // check if there are no successorNodes for the bad node
        else if (!successorNodes.keySet().contains(badCategoryId))
          // remove the bad node from the successorNodes of the bad category's predecessorNodes
          for (Integer predecessorNodeId : predecessorNodes.get(badCategoryId))
          successorNodes.remove(predecessorNodeId, badCategoryId);
        else {
          // compute the transitiveClosure for the current bad node
          for (Integer predecessorNodeId : predecessorNodes.get(badCategoryId)) {
            if (!badCategoryId.equals(predecessorNodeId)) {
              for (Integer successorNodeId : successorNodes.get(badCategoryId)) {
                successorNodes.put(predecessorNodeId, successorNodeId);
                predecessorNodes.put(successorNodeId, predecessorNodeId);
              }
            }
            // removing the bad category from the predecessorNode's successorNodes
            successorNodes.remove(predecessorNodeId, badCategoryId);
          }
          // removing the bad category from all successorNode's predecessorNodes
          for (Integer successorNodeId : successorNodes.get(badCategoryId))
            predecessorNodes.remove(successorNodeId, badCategoryId);
        }
        // removing the key for the bad node from the graph
        successorNodes.removeKey(badCategoryId);
        predecessorNodes.removeKey(badCategoryId);
      }
    }
    Announce.progressDone();
    Announce.done("done (categories removed: " + badCategories.size() + ")");

    return successorNodes;
  }

  /**
   * Decides if a category is a good or a bad category
   *
   * @return if the given category is a good true, otherwise false
   */
  protected static boolean isGoodCategory(String cat, Set<String> wordnetWords, Set<String> nonConceptualCategories) {
    boolean result = true;
    String categoryName = FactComponent.stripCat(cat);

    // Check if the category is a valid semantic category.
    // Valid categories have a headword in plural
    // and if there is a postModifier it must be a named entity.
    NounGroup category = new NounGroup(categoryName);
    NounGroup categoryLower = new NounGroup(categoryName.toLowerCase());
    String stemmedHead = PlingStemmer.stem(categoryLower.head());
    if (category.head() == null) {
      Announce.debug("Could not find type in", categoryName, "(has empty head)");
      result = false;
    } else if (Name.isAbbreviation(category.head())) {
      // If the category is an acronym, drop it
      Announce.debug("Could not find type in", categoryName, "(is abbreviation)");
      result = false;
    } else if (PlingStemmer.isSingular(categoryLower.head()) && !category.head().toLowerCase().equals("people")) {
      // Only plural words are good hypernyms
      Announce.debug("Could not find type in", categoryName, "(is singular)");
      result = false;
    } else if (nonConceptualCategories.contains(stemmedHead)) {
      Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
      result = false;
    } else if (categoryName.contains("WikiProject")) {
      Announce.debug("Could not find type in", categoryName, "(is a WikiProject)");
      result = false;
    } else if (cat.contains("Wikipedia_requested")) {
      Announce.debug("Could not find type in", categoryName, "(is a Wikipedia request category)");
      result = false;
    } else if (categoryLower.postModifier() != null) {
      NounGroup curNounGroup = categoryLower;
      NounGroup lastNounGroup;
      while (curNounGroup.postModifier() != null) {
        lastNounGroup = curNounGroup;
        curNounGroup = curNounGroup.postModifier();
        if (lastNounGroup.preposition() != null && lastNounGroup.preposition().equals("by") && wordnetWords.contains(curNounGroup.head())) {
          Announce.debug("Could not find type in", categoryName, "(has non-entity post-modifier)");
          result = false;
          break;
        }
      }
    }
    return result;
  }

  //====================
  //=== Functions to make sure that categories are connected with just one class
  //

  /**
   * Removes the bad facts from a graph.
   * Bad Facts are edges between categories that belong to different WordNet classes.
   *
   * @return The Graph without the bad facts
   */
  protected IndexedGraph removeBadFacts(IndexedGraph graph, Index<String> index, IndexedGraph indexedClassHierarchy,
      Set<String> nonConceptualCategories, Map<String, String> preferredMeanings, Set<String> wordnetWords) {
    Announce.doing("Remove bad facts");
    Announce.doing("Prepare yago branch groups");
    // reverse the graph to get better access
    IndexedGraph graphReversed = graph.getReversed();
    //		graph = null;

    // getting all wnClasses
    Set<Integer> wnClasses = new HashSet<>();
    for (Integer nodeId : graphReversed.keySet())
      if (isWordnetClass(nodeId)) wnClasses.add(nodeId);

    // all yago branches as integers
    Integer person = index.get(YAGO.person), organization = index.get(YAGO.organization), building = index.get(YAGO.building),
        location = index.get(YAGO.location), artifact = index.get(YAGO.artifact), abstraction = index.get(YAGO.abstraction),
        physicalEntity = index.get(YAGO.physicalEntity);

    // list of all yago branches
    Integer[] realYagoBranches = new Integer[] { person, organization, building, location, artifact, abstraction, physicalEntity };

    for (Integer yagoBranch : realYagoBranches) {
      if (debugWriter_removeBadFacts != null) debugWriter_removeBadFacts.println(yagoBranch + " ==> " + index.get(yagoBranch));
    }

    // define how to bundle up the yago branches
    Set<List<Integer>> yagoBranchCondenseRulesSets = new HashSet<>();
    yagoBranchCondenseRulesSets.add(Arrays.asList(building, organization, location));
    yagoBranchCondenseRulesSets.add(Arrays.asList(person, physicalEntity));
    yagoBranchCondenseRulesSets.add(Arrays.asList(abstraction));
    yagoBranchCondenseRulesSets.add(Arrays.asList(artifact));
    // this is just for better debug output
    // the yago branches don't have to be indexed, they could easily assigned with a custom index
    IndexedGraph yagoBranchCondenseRules = new IndexedGraph();
    for (List<Integer> yagoBranchCondenseRule : yagoBranchCondenseRulesSets) {
      List<String> yagoBranchStrings = new ArrayList<>();
      for (Integer yagoBranchId : yagoBranchCondenseRule)
        yagoBranchStrings.add(index.get(yagoBranchId));
      yagoBranchCondenseRules.putAll(index.addLow(yagoBranchStrings.toString()), yagoBranchCondenseRule);
    }

    // TODO(fkeller): physical entity and location are on the same level so i cant decide automatically which one
    // should be included and which shouldn't. So i have to add this additional fact
    // to make sure location is treated as a subclass of physical entity. etc...
    indexedClassHierarchy.put(location, physicalEntity);
    indexedClassHierarchy.put(location, abstraction);
    indexedClassHierarchy.put(location, artifact);
    IndexedGraph wnClassesTransitiveClosureReversed = computeTransitiveClosure(indexedClassHierarchy.getReversed());

    // check all bundle rules
    IndexedGraph condensedYagoBranches = new IndexedGraph();
    for (Integer condensedYagoBranch : yagoBranchCondenseRules.keySet()) {
      // add all subclasses from the branches specified in the rule to the condensed branch
      for (Integer yagoBranch : yagoBranchCondenseRules.get(condensedYagoBranch)) {
        condensedYagoBranches.putAll(condensedYagoBranch, wnClassesTransitiveClosureReversed.get(yagoBranch));
        // we add the branch class itself too so we can look up this class aswell
        condensedYagoBranches.put(condensedYagoBranch, yagoBranch);
      }
      // remove all branches from the condensed branch which are sub branches but not one of the branches defined in the rules
      for (Integer yagoBranch : realYagoBranches) {
        if (yagoBranchCondenseRules.get(condensedYagoBranch).contains(yagoBranch)) continue;
        if (condensedYagoBranches.contains(condensedYagoBranch, yagoBranch)) {
          condensedYagoBranches.removeAll(condensedYagoBranch, wnClassesTransitiveClosureReversed.get(yagoBranch));
          condensedYagoBranches.remove(condensedYagoBranch, yagoBranch);
        }
      }
    }

    Map<Integer, Visitor> visitors = new HashMap<>();
    // mark all WordNet nodes as visited
    for (Iterator<Integer> wnClassIterator = wnClasses.iterator(); wnClassIterator.hasNext();) {
      Integer wnClass = wnClassIterator.next();
      for (Integer yagoBranch : condensedYagoBranches.keySet()) {
        if (condensedYagoBranches.contains(yagoBranch, wnClass)) {
          Visitor newVisitor = new Visitor(wnClass, yagoBranch, wnClass);
          visitors.put(wnClass, newVisitor);
          break;
        }
      }
      if (visitors.get(wnClass) == null) wnClassIterator.remove();
    }
    Announce.done("done (Yago branch groups: " + condensedYagoBranches.keySet().size() + ")");
    Announce.progressStart("Removing bad Facts", graphReversed.size());
    int factsRemoved = 0;
    Set<Integer> curNodes = wnClasses;
    Map<Integer, Set<Visitor>> notConnectedNodes = new HashMap<>();
    while (!curNodes.isEmpty()) {
      // go down the Wikipedia hierarchy
      while (!curNodes.isEmpty()) {
        Set<Integer> nextNodes = new HashSet<>();
        for (Integer curNode : curNodes) {
          // get subCategories
          Set<Integer> nodesToCheck = graphReversed.get(curNode);
          if (nodesToCheck == null) continue;
          for (Integer curNodeToCheck : nodesToCheck) {
            Announce.progressStep();
            Visitor curVisitor = visitors.get(curNode);
            // check if the current sub category was already visited
            if (visitors.containsKey(curNodeToCheck)) {
              // get the visit information of the subcategory
              Visitor wantToVisit = visitors.get(curNodeToCheck);
              // check if both nodes are in the same branch
              if (curVisitor.yagoBranch.equals(wantToVisit.yagoBranch)
                  // and if the flag is set check also if the wn class of the lower node is the same or a subclass fo the cur node
                  && (!USE_WORDNET_SUBCLASS_FOR_FACT_REMOVAL || (curVisitor.wnClass.equals(wantToVisit.wnClass)
                      || wnClassesTransitiveClosureReversed.contains(curVisitor.wnClass, wantToVisit.wnClass)))) {
                // add the current node to the parents of the sub node
                wantToVisit.visitors.add(curVisitor);
                // check if the yagoBranch of the current node is a superclass of the sub node
              } else {
                // nothing => indirectly deleting the curVisitor => wantToVisit edge
                factsRemoved++;
                if (debugWriter_removeBadFacts != null) debugWriter_removeBadFacts.println("Removing Fact: " + index.get(wantToVisit.id) + " ["
                    + index.get(wantToVisit.yagoBranch) + "]" + " => " + index.get(curVisitor.id) + " [" + index.get(curVisitor.yagoBranch) + "]");
              }
            } else {
              // generate the wnClass via cat2class
              Integer generatedWnClass = null;
              if (isGoodCategory(index.get(curNodeToCheck), wordnetWords, nonConceptualCategories)) {
                String generatedWnClassString = category2class(index.get(curNodeToCheck), nonConceptualCategories, preferredMeanings);
                if (generatedWnClassString != null) generatedWnClass = index.get(generatedWnClassString);
              }
              // check if the sub node is a subclass of the current yagoBranch
              if (generatedWnClass != null && condensedYagoBranches.contains(curVisitor.yagoBranch, generatedWnClass)
                  && (!USE_WORDNET_SUBCLASS_FOR_FACT_REMOVAL || (curVisitor.wnClass.equals(generatedWnClass)
                      || wnClassesTransitiveClosureReversed.contains(curVisitor.wnClass, generatedWnClass)))) {
                // mark the sub node as visited and add the current node as parent
                Visitor newVisitor = new Visitor(curNodeToCheck, curVisitor.yagoBranch, generatedWnClass);
                newVisitor.visitors.add(curVisitor);
                visitors.put(curNodeToCheck, newVisitor);
                nextNodes.add(curNodeToCheck);
              } else {
                // the sub node could not be connected to the current node
                // => add it to the notConnected list
                Set<Visitor> curVisitorSet = notConnectedNodes.get(curNodeToCheck);
                if (curVisitorSet == null) {
                  notConnectedNodes.put(curNodeToCheck, (curVisitorSet = new HashSet<>()));
                }
                curVisitorSet.add(curVisitor);
              }
            }
          }
        }
        curNodes = nextNodes;
      }

      // subtracting all connected nodes from the notConnected list
      notConnectedNodes.keySet().removeAll(visitors.keySet());

      // heuristic to determine where a not connected category should be connected to
      for (Integer key : notConnectedNodes.keySet()) {
        Integer generatedYagoBranch = null;
        String generatedClassString = null;
        if (!USE_ONLY_COUNTING_FOR_BAD_FACT_REMOVAL
            && (!FORCE_COUNTING_FOR_BAD_CATS || isGoodCategory(index.get(key), wordnetWords, nonConceptualCategories))) {
          generatedClassString = category2class(index.get(key), nonConceptualCategories, preferredMeanings);
          Integer generatedClass = null;
          if (generatedClassString != null) {
            // via cat2class => determine the yagoBranch via cat2class and connect it to this
            generatedClass = index.get(generatedClassString);

            for (Integer curYagoBranch : condensedYagoBranches.keySet()) {
              if (condensedYagoBranches.contains(curYagoBranch, generatedClass)) {
                generatedYagoBranch = curYagoBranch;
                break;
              }
            }
          }
          if (generatedYagoBranch != null) {
            Visitor newVisitor = new Visitor(key, generatedYagoBranch, generatedClass);
            newVisitor.visitors.add(new Visitor(generatedClass, generatedYagoBranch, generatedClass));
            visitors.put(key, newVisitor);
            curNodes.add(key);
            if (debugWriter_removeBadFacts != null)
              debugWriter_removeBadFacts.println("Added a new Root edge: " + index.get(key) + " " + WORDNET_RELATION + " " + generatedClassString);
            continue;
          }
        }
        // via counting => count the direct connections to this node and keep all connections from the yagoBranch with the highest representation
        if (USE_WORDNET_SUBCLASS_FOR_FACT_REMOVAL) {
          Map<Integer, Map<Integer, Set<Visitor>>> visitorsPerClassPerBranch = new HashMap<>();
          for (Visitor visitor : notConnectedNodes.get(key)) {
            Map<Integer, Set<Visitor>> curYagoBranchMap = visitorsPerClassPerBranch.get(visitor.yagoBranch);
            if (curYagoBranchMap == null) visitorsPerClassPerBranch.put(visitor.yagoBranch, (curYagoBranchMap = new HashMap<>()));
            boolean added = false;
            for (Integer wnClass : new HashSet<>(curYagoBranchMap.keySet())) {
              if (wnClass.equals(visitor.wnClass) || wnClassesTransitiveClosureReversed.contains(wnClass, visitor.wnClass)) {
                Set<Visitor> curClassSet = curYagoBranchMap.get(wnClass);
                if (curClassSet == null) curYagoBranchMap.put(wnClass, (curClassSet = new HashSet<>()));
                curClassSet.add(visitor);
                added = true;
              } else if (wnClassesTransitiveClosureReversed.contains(visitor.wnClass, wnClass)) {
                Set<Visitor> curClassSet = curYagoBranchMap.remove(wnClass);
                if (curClassSet == null) curClassSet = new HashSet<>();
                curClassSet.add(visitor);
                curYagoBranchMap.put(visitor.wnClass, curClassSet);
                added = true;
              }
            }
            if (!added) {
              Set<Visitor> curClassSet = new HashSet<>();
              curClassSet.add(visitor);
              curYagoBranchMap.put(visitor.wnClass, curClassSet);
            }
          }
          Integer maxYagoBranch = null;
          Integer maxWnClass = null;
          Set<Visitor> maxYagoBranchVisitors = new HashSet<>();
          for (Integer yagoBranch : visitorsPerClassPerBranch.keySet()) {
            for (Map.Entry<Integer, Set<Visitor>> wnClassVisitors : visitorsPerClassPerBranch.get(yagoBranch).entrySet()) {
              if (wnClassVisitors.getValue().size() > maxYagoBranchVisitors.size()) {
                maxYagoBranch = yagoBranch;
                maxWnClass = wnClassVisitors.getKey();
                maxYagoBranchVisitors = wnClassVisitors.getValue();
              }
            }
          }
          if (maxYagoBranch != null) {
            Visitor newVisitor = new Visitor(key, maxYagoBranch, maxWnClass);
            newVisitor.visitors = maxYagoBranchVisitors;
            visitors.put(key, newVisitor);
            curNodes.add(key);
            if (debugWriter_removeBadFacts != null) debugWriter_removeBadFacts
                .println("Connected via counting: " + index.get(key) + " to branch: " + index.get(maxYagoBranch) + " [direct class: "
                    + generatedClassString + ", branch: " + (generatedYagoBranch != null ? index.get(generatedYagoBranch) : null) + "]");
          }
        } else {
          Map<Integer, Integer> visitorYagoBranchCounter = new HashMap<>();
          Map<Integer, Set<Visitor>> visitorsPerBranch = new HashMap<>();
          for (Visitor subVisitor : notConnectedNodes.get(key)) {
            Integer curWnClassCounter = visitorYagoBranchCounter.get(subVisitor.yagoBranch);
            Set<Visitor> curVisitorSet = visitorsPerBranch.get(subVisitor.yagoBranch);
            if (curWnClassCounter == null) {
              curWnClassCounter = 0;
              curVisitorSet = new HashSet<>();
              visitorsPerBranch.put(subVisitor.yagoBranch, curVisitorSet);
            }
            curVisitorSet.add(subVisitor);
            visitorYagoBranchCounter.put(subVisitor.yagoBranch, curWnClassCounter + 1);
          }
          Integer maxYagoBranch = 0;
          for (Integer yagoBranch : visitorYagoBranchCounter.keySet()) {
            if (maxYagoBranch.equals(0) || visitorYagoBranchCounter.get(yagoBranch) > visitorYagoBranchCounter.get(maxYagoBranch))
              maxYagoBranch = yagoBranch;
          }
          if (!maxYagoBranch.equals(0)) {
            Visitor newVisitor = new Visitor(key, maxYagoBranch, null);
            newVisitor.visitors = visitorsPerBranch.get(maxYagoBranch);
            visitors.put(key, newVisitor);
            curNodes.add(key);
            if (debugWriter_removeBadFacts != null) debugWriter_removeBadFacts
                .println("Connected via counting: " + index.get(key) + " to branch: " + index.get(maxYagoBranch) + " [direct class: "
                    + generatedClassString + ", branch: " + (generatedYagoBranch != null ? index.get(generatedYagoBranch) : null) + "]");
          }
        }
      }
    }

    IndexedGraph result = new IndexedGraph();

    for (Integer key : visitors.keySet()) {
      for (Visitor visitor : visitors.get(key).visitors)
        result.put(key, visitor.id);
    }

    Announce.progressDone();
    Announce.done("done (Facts removed: " + factsRemoved + ")");

    return result;
  }

  /**
   * This class represents basically a node with attributes that helps you to remove the bad facts
   */
  private class Visitor {

    public Integer id;

    public Set<Visitor> visitors;

    public Integer yagoBranch;

    public Integer wnClass;

    public Visitor(Integer id, Integer yagoBranch, Integer wnClass) {
      this.id = id;
      this.yagoBranch = yagoBranch;
      this.wnClass = wnClass;
      visitors = new HashSet<>();
    }

    @SuppressWarnings("unused")
    public boolean removeParentById(Integer id) {
      return visitors.remove(new Visitor(id, null, null));
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Visitor other = (Visitor) obj;
      if (id == null) {
        if (other.id != null) return false;
      } else if (!id.equals(other.id)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "Visitor [id=" + id + ", visitors=" + visitors + ", yagoBranch=" + yagoBranch + "]";
    }
  }

  /**
   * Maps a category to a wordnet class
   *
   * @return The class for the given category
   */
  private String category2class(String categoryName, Set<String> nonConceptualCategories, Map<String, String> preferredMeanings) {
    categoryName = FactComponent.stripCat(categoryName);
    // Check out whether the new category is worth being added
    NounGroup category = new NounGroup(categoryName);
    if (category.head() == null) {
      Announce.debug("Could not find type in", categoryName, "(has empty head)");
      return (null);
    }

    // If the category is an acronym, drop it
    if (Name.isAbbreviation(category.head())) {
      Announce.debug("Could not find type in", categoryName, "(is abbreviation)");
      return (null);
    }
    category = new NounGroup(categoryName.toLowerCase());

    // Only plural words are good hypernyms

    if (PlingStemmer.isSingular(category.head()) && !category.head().toLowerCase().equals("people")) {
      Announce.debug("Could not find type in", categoryName, "(is singular)");
      return (null);
    }

    String stemmedHead = PlingStemmer.stem(category.head());

    // Exclude the bad guys
    if (nonConceptualCategories.contains(stemmedHead)) {
      Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
      return (null);
    }

    // Try all premodifiers (reducing the length in each step) + head
    if (category.preModifier() != null) {
      String wordnet = null;
      String preModifier = category.preModifier().replace('_', ' ');

      for (int start = 0; start != -1 && start < preModifier.length() - 2; start = preModifier.indexOf(' ', start + 1)) {
        wordnet = preferredMeanings.get((start == 0 ? preModifier : preModifier.substring(start + 1)) + " " + stemmedHead);
        // take the longest matching sequence
        if (wordnet != null) return (wordnet);
      }
    }

    // Try postmodifiers to catch "head of state"
    if (category.postModifier() != null && category.preposition() != null && category.preposition().equals("of")) {
      String wordnet = preferredMeanings.get(stemmedHead + " of " + category.postModifier().head());
      if (wordnet != null) return (wordnet);
    }

    // Try head
    String wordnet = preferredMeanings.get(stemmedHead);
    if (wordnet != null) return (wordnet);
    Announce.debug("Could not find type in", categoryName, "(" + stemmedHead + ") (no wordnet match)");
    return (null);
  }

  /**
   * Computes the transitive closure of a graph
   *
   * @return The transitive closure
   */
  protected IndexedGraph computeTransitiveClosure(IndexedGraph graph) {
    IndexedGraph successorNodes = new IndexedGraph(graph);
    IndexedGraph predecessorNodes = successorNodes.getReversed();

    { // compute transitive closure
      Set<Integer> nodes = new HashSet<>(successorNodes.keySet());
      nodes.retainAll(predecessorNodes.keySet());
      Announce.progressStart("Compute transitive closure", nodes.size());
      for (Integer nodeId : nodes) {
        Announce.progressStep();
        // predecessor => node => successor
        // V V V
        // predecessor => successor
        // && predecessor => node => successor
        for (Integer predecessorNodeId : predecessorNodes.get(nodeId)) {
          if (!nodeId.equals(predecessorNodeId)) {
            for (Integer successorNodeId : successorNodes.get(nodeId)) {
              successorNodes.put(predecessorNodeId, successorNodeId);
              predecessorNodes.put(successorNodeId, predecessorNodeId);
            }
          }
        }
      }
      Announce.progressDone();
    }

    return successorNodes;
  }

  //====================
  //=== Functions to break the cycles in the graph
  //

  /**
   * Breaks the cycles of a graph and don't produce additional sinks.
   * If there is a strongly connected component that is a own not connected subgraph this algorithm wont find a edge to break;
   *
   * @return The removed edges
   */
  protected IndexedGraph breakCyclesNoSinks(IndexedGraph hierarchy) {
    Announce.doing("Breaking cycles");
    Announce.doing("Looking for cycles");
    Set<IndexedGraph> cycles = trajan(hierarchy);
    Announce.done("done (Cycles found: " + cycles.size() + ")");

    if (cycles.isEmpty()) {
      return new IndexedGraph();
    }

    Announce.doing("Get Facts to break");
    IndexedGraph factsToBreak = new IndexedGraph();

    for (IndexedGraph cycle : cycles) {
      List<Integer> vertexSequence = new ArrayList<>();
      List<CycleNode> cycleNodes = new ArrayList<>();
      { // generate vertex Sequence
        for (Integer cycleNodeId : cycle.keySet()) {
          cycleNodes.add(
              new CycleNode(cycleNodeId, new HashSet<>(cycle.get(cycleNodeId)), cycle.get(cycleNodeId).size() < hierarchy.get(cycleNodeId).size()));
        }
        while (!cycleNodes.isEmpty()) {
          Collections.sort(cycleNodes);
          int curNodeId = cycleNodes.remove(0).getId();
          for (CycleNode cycleNode : cycleNodes)
            cycleNode.remove(curNodeId);
          vertexSequence.add(curNodeId);
        }
      }

      // remove all edges in the vertex sequence from left to right
      for (int i = 0; i < vertexSequence.size() - 1; i++) {
        for (int j = i + 1; j < vertexSequence.size(); j++) {
          if (cycle.contains(vertexSequence.get(i), vertexSequence.get(j))) {
            factsToBreak.put(vertexSequence.get(i), vertexSequence.get(j));
          }
        }
      }
    }

    Announce.done("done (Facts to break: " + factsToBreak.size() + ")");
    hierarchy.removeAll(factsToBreak);

    for (Integer nodeId : new HashSet<>(hierarchy.keySet())) {
      hierarchy.remove(nodeId, nodeId);
    }
    Announce.done();

    return factsToBreak;
  }

  private class CycleNode implements Comparable<CycleNode> {

    private int id;

    private Set<Integer> outEdges;

    private boolean modified;

    private CycleNode(int id, Set<Integer> outEdges, boolean modified) {
      this.id = id;
      this.outEdges = outEdges;
      this.modified = modified;
    }

    public int getId() {
      return id;
    }

    public void remove(int id) {
      if (outEdges.remove(id)) modified = true;
    }

    @Override
    public int compareTo(CycleNode cn) {
      int result;
      if (modified && !cn.modified) return -1;
      else if (!modified && cn.modified) return 1;
      else if ((result = Integer.compare(outEdges.size(), cn.outEdges.size())) != 0) return result;
      else return Integer.compare(id, cn.id);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CycleNode cycleNode = (CycleNode) o;

      return id == cycleNode.id;

    }

    @Override
    public int hashCode() {
      return id;
    }

    @Override
    public String toString() {
      return "CycleNode{" + "id=" + id + ", outEdges=" + outEdges.size() + ", modified=" + modified + '}';
    }
  }

  /**
   * @return a map of nodes out of a given graph
   */
  private Map<Integer, Node> createNodes(IndexedGraph graph) {
    Map<Integer, Node> nodes = new HashMap<>();
    for (Integer wpCat : graph.keySet()) {
      Set<Integer> curAdjacentNodes = graph.get(wpCat);
      if (!curAdjacentNodes.isEmpty()) {
        Node nodeToAdd = new Node(wpCat);
        nodeToAdd.adjacentNodes.addAll(curAdjacentNodes);
        nodes.put(wpCat, nodeToAdd);
      }
    }

    return nodes;
  }

  /**
   * uses trajans algorithm to get all strongly connected components
   *
   * @return Strongly connected components
   */
  private Set<IndexedGraph> trajan(IndexedGraph graph) {
    Map<Integer, Node> nodes = createNodes(graph);
    Set<IndexedGraph> result = new HashSet<>();
    int index = 0;
    Stack<Node> tarStack = new Stack<>();
    for (Integer cat : nodes.keySet()) {
      Node u = nodes.get(cat);
      if (!nodes.get(cat).visited) {
        u.index = index;
        u.visited = true;
        u.lowlink = index;
        index++;
        u.vindex = 0;
        tarStack.push(u);
        u.caller = null;
        Node last = u;
        IndexedGraph curSCC = new IndexedGraph();
        Set<Node> curSCCnodes = new HashSet<>();
        while (true) {
          if (last.vindex < last.adjacentNodes.size()) {
            Node w = nodes.get(last.adjacentNodes.get(last.vindex));
            last.vindex++;
            if (w == null) continue;
            if (!w.visited) {
              w.caller = last;
              w.vindex = 0;
              w.index = index;
              w.visited = true;
              w.lowlink = index;
              index++;
              tarStack.push(w);
              last = w;
            } else if (tarStack.contains(w)) {
              last.lowlink = Math.min(last.lowlink, w.lowlink);
            }
          } else {
            if (last.lowlink == last.index) {
              Node top = tarStack.pop();
              curSCCnodes.add(top);

              while (!top.key.equals(last.key)) {
                top = tarStack.pop();
                curSCCnodes.add(top);
              }

              if (!curSCCnodes.isEmpty()) {
                for (Node n : curSCCnodes) {
                  for (Integer adjacent : n.adjacentNodes) {
                    if (curSCCnodes.contains(nodes.get(adjacent))) curSCC.put(n.key, adjacent);
                  }
                }
                if (!curSCC.isEmpty()) result.add(curSCC);
              }

              curSCCnodes.clear();
              curSCC = new IndexedGraph();
            }

            Node newLast = last.caller;
            if (newLast != null) {
              newLast.lowlink = Math.min(newLast.lowlink, last.lowlink);
              last = newLast;
            } else {
              break;
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * This class represents a Node of the graph with the needed attributes for trajans algorithm
   */
  private class Node {

    public Integer key;

    public int index;

    public int lowlink;

    public Node caller;

    public int vindex;

    public ArrayList<Integer> adjacentNodes;

    public boolean visited;

    public Node() {
      adjacentNodes = new ArrayList<>();
    }

    public Node(Integer key) {
      this();
      this.key = key;
    }

    @Override
    public int hashCode() {
      return key;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Node other = (Node) obj;
      return key.equals(other.key);
    }

    @SuppressWarnings("unused")
    private CategoryClassHierarchyExtractor getOuterType() {
      return CategoryClassHierarchyExtractor.this;
    }
  }

  //====================
  //=== Functions to remove unnecessary transitive edges
  //

  /**
   * Removes unnecessary transitive edges from a graph
   *
   * @return removed edges
   */
  protected IndexedGraph removeUnnecessaryTransitiveEdges(IndexedGraph graph, Index<String> index) {
    IndexedGraph remove = new IndexedGraph();
    Announce.doing("Removing unnecessary transitive edges");
    IndexedGraph transitiveClosure = computeTransitiveClosure(graph);
    for (Integer i : graph.keySet()) {
      for (Integer j : graph.get(i)) {
        if (transitiveClosure.contains(i, j)) {
          for (Integer k : graph.get(i)) {
            if (k.equals(j)) continue;
            if (transitiveClosure.contains(j, k)) remove.put(i, k);
          }
        }
      }
    }
    graph.removeAll(remove);

    Announce.done("done (Transitive edges removed: " + remove.toFactCollection(index).size() + ")");

    return remove;
  }

  @SuppressWarnings("unused")
  private List<Integer> sortedNodes(IndexedGraph graph) {
    Set<Integer> leaves = new HashSet<>();
    IndexedGraph graphReversed = graph.getReversed();
    for (Integer wpCatSubId : graph.keySet()) {
      if (!graphReversed.containsKey(wpCatSubId)) {
        leaves.add(wpCatSubId);
      }
    }
    Map<Integer, Node> nodes = createNodes(graph);
    List<Integer> result = new ArrayList<>(leaves);
    for (Integer cat : leaves) {
      Node u = nodes.get(cat);
      if (!nodes.get(cat).visited) {
        u.visited = true;
        u.vindex = 0;
        u.caller = null;
        Node last = u;
        while (true) {
          if (last.vindex < last.adjacentNodes.size()) {
            Node w = nodes.get(last.adjacentNodes.get(last.vindex));
            last.vindex++;
            if (w == null) continue;
            if (!w.visited) {
              result.add(w.key);
              w.caller = last;
              w.vindex = 0;
              w.visited = true;
              last = w;
            }
          } else {
            Node newLast = last.caller;
            if (newLast != null) {
              last = newLast;
            } else {
              break;
            }
          }
        }
      }
    }
    System.out.println("Sorted Nodes: " + result.size());
    return result;
  }

  //====================
  //=== Utility functions
  //

  /**
   * This class represents an index for given nodes
   */
  protected static class Index<T> {

    private Map<Integer, T> id2tMap;

    private Map<T, Integer> t2idMap;

    private int highestIndex;

    private int lowestIndex;

    public Index() {
      id2tMap = new HashMap<>();
      t2idMap = new HashMap<>();
      highestIndex = 0;
      lowestIndex = 0;
    }

    public boolean contains(T value) {
      return t2idMap.containsKey(value);
    }

    public boolean add(int key, T value) {
      if (t2idMap.containsKey(value)) return false;
      if (key > highestIndex) highestIndex = key;
      else if (key < lowestIndex) lowestIndex = key;

      id2tMap.put(key, value);
      t2idMap.put(value, key);
      return true;
    }

    public T get(int key) {
      return id2tMap.get(key);
    }

    public Integer get(T value) {
      try {
        return t2idMap.get(value);
      } catch (Exception e) {
        System.out.println("Could not look up: " + value);
        return null;
      }
    }

    public Integer addHigh(T value) {
      if (t2idMap.containsKey(value)) return null;
      highestIndex++;
      id2tMap.put(highestIndex, value);
      t2idMap.put(value, highestIndex);
      return highestIndex;
    }

    public Integer addLow(T value) {
      if (t2idMap.containsKey(value)) return null;
      lowestIndex--;
      id2tMap.put(lowestIndex, value);
      t2idMap.put(value, lowestIndex);
      return lowestIndex;
    }

    public void addAllHigh(Collection<T> values) {
      for (T value : values)
        addHigh(value);
    }

    public void addAllLow(Collection<T> values) {
      for (T value : values)
        addLow(value);
    }
  }

  /**
   * This class represents a graph with indices
   */
  protected static class IndexedGraph {

    private int edgeCount;

    private Map<Integer, Set<Integer>> graph;

    public IndexedGraph() {
      graph = new HashMap<>();
      edgeCount = 0;
    }

    public IndexedGraph(IndexedGraph oriGraph) {
      this();
      for (Integer nodeId : oriGraph.keySet()) {
        putAll(nodeId, oriGraph.get(nodeId));
      }
    }

    public IndexedGraph(FactCollection factGraph, Index<String> index) {
      this();
      putAll(factGraph, index);
    }

    public boolean put(Integer key, Integer value) {
      if (value == null || key == null) return false;
      Set<Integer> curSet = graph.get(key);
      if (curSet == null) {
        curSet = new HashSet<>();
        graph.put(key, curSet);
      }
      if (curSet.add(value)) {
        edgeCount++;
        return true;
      }
      return false;
    }

    public void put(Fact fact, Index<String> index) {
      put(index.get(fact.getSubject()), index.get(fact.getObject()));
    }

    public boolean put(Integer key, Set<Integer> newSet) {
      if (newSet == null || key == null) return false;
      Set<Integer> oldSet = graph.put(key, newSet);
      if (oldSet != null) edgeCount -= oldSet.size();
      edgeCount += newSet.size();
      return true;
    }

    public void putAll(Collection<Fact> facts, Index<String> index) {
      for (Fact f : facts) {
        put(f, index);
      }
    }

    public void putAll(Integer key, Collection<Integer> values) {
      if (values == null || values.isEmpty() || key == null) return;
      Set<Integer> curSet = graph.get(key);
      if (curSet == null) {
        curSet = new HashSet<>();
        graph.put(key, curSet);
      }
      edgeCount -= curSet.size();
      curSet.addAll(values);
      edgeCount += curSet.size();
    }

    public void putAll(IndexedGraph graph2add) {
      for (Integer key : graph2add.keySet()) {
        putAll(key, graph2add.get(key));
      }
    }

    public Set<Integer> get(Integer key) {
      return graph.get(key);
    }

    public void remove(Integer where, Integer what) {
      Set<Integer> curSet = graph.get(where);
      if (curSet != null) {
        if (curSet.remove(what)) edgeCount--;
      }
      if (curSet == null || curSet.isEmpty()) removeKey(where);
    }

    public void remove(Integer what) {
      removeKey(what);
      for (Integer key : new HashSet<>(graph.keySet())) {
        Set<Integer> curSet = graph.get(key);
        if (curSet != null) {
          if (curSet.remove(what)) edgeCount--;
        }
        if (curSet == null || curSet.isEmpty()) removeKey(key);
      }
    }

    public void removeIgnoreKey(Integer what) {
      for (Integer key : new HashSet<>(graph.keySet())) {
        Set<Integer> curSet = graph.get(key);
        if (curSet != null) {
          if (curSet.remove(what)) edgeCount--;
        }
      }
    }

    public void removeKey(Integer key) {
      Set<Integer> oldSet = graph.remove(key);
      if (oldSet != null) edgeCount -= oldSet.size();
    }

    public void removeAll(IndexedGraph toRemove) {
      for (Integer nodeId : toRemove.keySet()) {
        for (Integer subNodeId : toRemove.get(nodeId))
          remove(nodeId, subNodeId);
      }
    }

    public void removeAll(Integer key, Collection<Integer> values) {
      Set<Integer> curSet = graph.get(key);
      edgeCount -= curSet.size();
      curSet.removeAll(values);
      if (curSet.isEmpty()) removeKey(key);
      else edgeCount += curSet.size();
    }

    public IndexedGraph getReversed() {
      IndexedGraph result = new IndexedGraph();
      for (int nodeId : graph.keySet()) {
        for (int subNodeId : graph.get(nodeId)) {
          result.put(subNodeId, nodeId);
        }
      }
      return result;
    }

    public FactCollection toFactCollection(Index<String> index) {
      FactCollection result = new FactCollection();
      for (Integer nodeId : graph.keySet()) {
        for (Integer subNodeId : graph.get(nodeId)) {
          if (isWordnetClass(subNodeId) || isWordnetClass(nodeId))
            result.addFast(new Fact(index.get(nodeId), WORDNET_RELATION, index.get(subNodeId)));
          else result.addFast(new Fact(index.get(nodeId), WIKIPEDIA_RELATION, index.get(subNodeId)));
        }
      }
      return result;
    }

    public Set<Integer> keySet() {
      return graph.keySet();
    }

    public boolean containsKey(Integer key) {
      return graph.containsKey(key);
    }

    public boolean containsValue(Integer value) {
      for (Integer key : graph.keySet())
        if (graph.get(key).contains(value)) return true;
      return false;
    }

    public boolean contains(Integer key, Integer value) {
      Set<Integer> curSet = graph.get(key);
      return curSet != null && curSet.contains(value);
    }

    public boolean isEmpty() {
      return graph.isEmpty();
    }

    public int size() {
      return edgeCount;
    }

    @Override
    public String toString() {
      return graph.toString();
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || obj.getClass() == this.getClass() && graph.equals(((IndexedGraph) obj).graph);
    }
  }

  private static boolean isWordnetClass(Integer nodeId) {
    return nodeId < 0;
  }

  /**
   * Removes the not connected parts of a graph
   *
   * @return The connected part of the graph
   */
  private IndexedGraph cleanGraph(IndexedGraph graph) {
    IndexedGraph graphReversed = graph.getReversed();
    IndexedGraph resultReversed = new IndexedGraph();

    Set<Integer> currentFacts = new HashSet<>();
    for (Integer nodeId : graphReversed.keySet())
      if (isWordnetClass(nodeId)) currentFacts.add(nodeId);

    // go down the Wikipedia hierarchy and save all categories
    while (!currentFacts.isEmpty()) {
      Set<Integer> nextFacts = new HashSet<>();
      for (Integer nodeId : currentFacts) {
        if (resultReversed.containsKey(nodeId)) continue;
        Set<Integer> factsToCheck = graphReversed.get(nodeId);
        if (factsToCheck == null) continue;
        resultReversed.put(nodeId, factsToCheck);
        nextFacts.addAll(factsToCheck);

      }
      currentFacts = nextFacts;
    }

    return resultReversed.getReversed();
  }

  protected static IndexedGraph readFileIndexedGraph(File file, Index<String> index) {
    IndexedGraph indexedGraph = new IndexedGraph();
    for (Fact fact : FactSource.from(file)) {
      indexedGraph.put(fact, index);
    }
    return indexedGraph;
  }

  /**
   * Removes the not connected parts of a graph
   *
   * @return The connected part of the graph
   */
  protected static FactCollection cleanGraph(FactCollection graph) {
    FactCollection graphReversed = graph.getReverse();
    FactCollection resultReversed = new FactCollection();
    FactCollection nextFacts = new FactCollection();
    FactCollection currentFacts = new FactCollection();
    currentFacts.justAddAll(graphReversed.getFactsWithRelation(WORDNET_RELATION));
    resultReversed.justAddAll(currentFacts);
    Collection<Fact> factsToCheck;

    // go down the Wikipedia hierarchy and save all categories
    while (true) {
      for (Fact f : currentFacts) {
        factsToCheck = graphReversed.getFactsWithSubjectAndRelation(f.getObject(), WIKIPEDIA_RELATION);
        for (Fact curFactToCheck : factsToCheck) {
          // check if the fact has already been processed if not adding it to the queue
          if (!resultReversed.containsSubject(curFactToCheck.getSubject())) nextFacts.addFast(curFactToCheck);
        }
      }
      // no more categories to progress? => finish
      if (nextFacts.isEmpty()) break;

      // adding the result of this run to a collection and prepare the other variables
      resultReversed.justAddAll(nextFacts);
      currentFacts = nextFacts;
      nextFacts = new FactCollection();
    }

    return resultReversed;
  }

  /**
   * prints out some useful graph information
   * (if detailed is true it will go through the whole graph and print the same information for this walkthrough
   */
  protected static void printGraphInfo(FactCollection graph, boolean detailed) {
    System.out.println("\tEdges: " + graph.size() + " \n" + "\tWordNetClasses: " + graph.getReverseMap(WORDNET_RELATION).keySet().size() + "\n"
        + "\tWordNetWikipediaCats: " + graph.getMap(WORDNET_RELATION).keySet().size() + "\n" + "\tWordNetFacts: "
        + graph.getFactsWithRelation(WORDNET_RELATION).size() + "\n" + "\tWikipediaCats: " + graph.getSubjects().size() + "\n" + "\tWikipediaFacts: "
        + graph.getFactsWithRelation(WIKIPEDIA_RELATION).size());
    if (detailed) {
      FactCollection checkedGraph = cleanGraph(graph);
      System.out.println("\tChecked:\n" + "\t\tEdges: " + checkedGraph.size() + " \n" + "\t\tWordNetClasses: "
          + checkedGraph.getMap(WORDNET_RELATION).keySet().size() + "\n" + "\t\tWordNetWikipediaCats: "
          + checkedGraph.getReverseMap(WORDNET_RELATION).keySet().size() + "\n" + "\t\tWordNetFacts: "
          + checkedGraph.getFactsWithRelation(WORDNET_RELATION).size() + "\n" + "\t\tWikipediaCats: " +
          /*checkedGraph.getObjects().size() + */"\n" + "\t\tWikipediaFacts: " + checkedGraph.getFactsWithRelation(WIKIPEDIA_RELATION).size());
    }
  }

  public CategoryClassHierarchyExtractor(String lang) {
    super(lang);
  }

  /**
   * check if the given category has roots and if so prints them
   * invalid roots are also printed out
   
   Fabian 2016-04-06: This code uses FactCollection.getObject(), which got removed.
   
   
  protected static String testCategory(String category, FactCollection cachedCategoryHierarchy,
  									 FactCollection cachedClassHierarchy) {
  	if (category.equals("random") || category.equals("rnd") || category.equals("")) {
  		int i = 0;
  		int random = (int) (Math.random() * cachedCategoryHierarchy.getSubjects().size());
  		for (String cat : cachedCategoryHierarchy.getSubjects()) {
  			if (i++ == random) {
  				category = cat;
  				System.out.println("Randomly chosen category: " + cat);
  				break;
  			}
  		}
  	} else {
  		if (!category.matches("<wordnet_[^>]+_[0-9]+>"))
  			category = FactComponent.forWikiCategory(FactComponent.stripCat(category));
  	}
  	Set<String> roots = new HashSet<>();
  	FactCollection alreadyProcessed;
  	{
  		FactCollection curFacts = new FactCollection();
  		alreadyProcessed = new FactCollection();
  		curFacts.add(new Fact("Start", "category", category));
  		while (!curFacts.isEmpty()) {
  			FactCollection nextFacts = new FactCollection();
  			boolean foundSomething = false;
  			for (Fact curFact : curFacts) {
  				if (alreadyProcessed.contains(curFact)) continue;
  				FactCollection tempFactCollection = new FactCollection();
  				tempFactCollection.addAll(
  					cachedCategoryHierarchy.getFactsWithSubjectAndRelation(curFact.getObject(),
  						WIKIPEDIA_RELATION));
  				tempFactCollection.addAll(
  					cachedCategoryHierarchy.getFactsWithSubjectAndRelation(curFact.getObject(), WORDNET_RELATION));
  				tempFactCollection.addAll(
  					cachedClassHierarchy.getFactsWithSubjectAndRelation(curFact.getObject(), WORDNET_RELATION));
  				for (String yagoBranch : SimpleTypeExtractor.yagoBranches) {
  					if (tempFactCollection.containsObject(yagoBranch)) {
  						List<Fact> topFacts =
  							cachedClassHierarchy.seekFactsWithRelationAndObject(WORDNET_RELATION, yagoBranch);
  						topFacts.addAll(
  							cachedCategoryHierarchy.seekFactsWithRelationAndObject(WORDNET_RELATION, yagoBranch));
  						for (Fact f : topFacts) {
  							roots.add(yagoBranch);
  							alreadyProcessed.add(f);
  						}
  						tempFactCollection.removeAll(topFacts);
  						foundSomething = true;
  					}
  				}
  				nextFacts.justAddAll(tempFactCollection);
  				if (foundSomething) break;
  			}
  			alreadyProcessed.addAll(curFacts);
  			curFacts = nextFacts;
  		}
  	}
  	alreadyProcessed = alreadyProcessed.getReverse();
  //		if (roots.size() > 1) {
  //			System.out.println("Multiple roots detected: " + roots);
  //		} else 
  	{
  		Stack<Fact> path = new Stack<>();
  		for (String root : roots) {
  			FactCollection curFacts = new FactCollection();
  			curFacts.add(new Fact("Yago", "Branch", root));
  			while (!curFacts.isEmpty()) {
  				FactCollection nextFacts = new FactCollection();
  				for (Fact curFact : curFacts) {
  					if (path.contains(curFact)) continue;
  					FactCollection tempFactCollection = new FactCollection();
  					tempFactCollection.addAll(
  						alreadyProcessed.getFactsWithSubjectAndRelation(curFact.getObject(), WIKIPEDIA_RELATION));
  					tempFactCollection.addAll(
  						alreadyProcessed.getFactsWithSubjectAndRelation(curFact.getObject(), WORDNET_RELATION));
  					nextFacts.justAddAll(tempFactCollection);
  					if (!tempFactCollection.isEmpty() || curFacts.size() == 1) {
  						path.add(curFact);
  						break;
  					}
  				}
  				curFacts = nextFacts;
  			}
  		}
  		if (path.isEmpty())
  			System.out.println(category + " was not found.");
  		while (!path.isEmpty()) {
  			Fact factToPrint = path.pop();
  			System.out.println(
  				new Fact(factToPrint.getObject(), factToPrint.getRelation(), factToPrint.getSubject()));
  		}
  	}
  
  	return "===================================";
  }*/

  public static void main(String[] args) throws Exception {
    FactCollection cachedCategoryHierarchy = new FactCollection(new File("/var/tmp/fkeller/yago3/output/categoryClassHierarchy_en.tsv"));
    FactCollection cachedClassHierarchy = new FactCollection(new File("/var/tmp/fkeller/yago3/output/wordnetClasses.tsv"));
    cachedClassHierarchy.addAll(new FactCollection(new File("/var/tmp/fkeller/yago3/output/hardWiredFacts.tsv")));
    printGraphInfo(cachedCategoryHierarchy, false);
    System.out.println("Enter a Wikipedia Category to check if it is connected to a root");
    while (true) {
      //D.p(testCategory(D.r(), cachedCategoryHierarchy, cachedClassHierarchy));
    }
  }
}
