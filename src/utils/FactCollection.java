package utils;

import java.io.File;
import java.io.IOException;
import java.util.AbstractSet;
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
import java.util.TreeSet;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.RDFS;
import basics.YAGO;

/**
 * Class FactCollection
 * 
 * This code is part of the YAGO project at the Max Planck Institute for
 * Informatics and the Telecom ParisTech University. It is licensed under a
 * Creative Commons Attribution License by the YAGO team:
 * https://creativecommons.org/licenses/by/3.0/
 * 
 * This class represents a collection of facts, indexes them. Methods have 3
 * degrees of complexity: (1) getXYZ(): simple index access (2) collectXYZ():
 * requires creating a collection (3) seekXYZ(): requires an expensive traversal
 * of the index. The collection is no longer synchronized! This should be fine
 * as long as writing is done by a single process, and reading occurs only after
 * writing.
 * 
 * @author Fabian M. Suchanek
 */
public class FactCollection extends AbstractSet<Fact> {

	/** Holds the facts */
	protected Set<Fact> facts = new HashSet<Fact>();

	/** Holds the objects */
	protected Map<String, String> objects = new HashMap<String, String>();

	/** Maps first arg to relation to facts */
	protected Map<String, Map<String, List<Fact>>> index = new HashMap<String, Map<String, List<Fact>>>();

	/** Maps relation to facts */
	protected Map<String, List<Fact>> relindex = new HashMap<String, List<Fact>>();

	/** Adds a fact, adds a source fact and a technique fact */
	public boolean add(Fact fact, String source, String technique) {
		Fact sourceFact = fact.metaFact(YAGO.extractionSource,
				FactComponent.forUri(source));
		Fact techniqueFact = sourceFact.metaFact(YAGO.extractionTechnique,
				FactComponent.forString(technique));
		return (add(fact) && add(sourceFact) && add(techniqueFact));
	}

	/** Adds a fact, checks for duplicates */
	public boolean add(final Fact fact) {
		return (add(fact, null).added);
	}

	/** Type of things that happen when a fact is added */
	public enum Add {
		NULL(false, false, false, false), DUPLICATE(false, false, false, true), TOOGENERAL(
				false, false, false, true), NOID(false, false, false, false), FUNCLASH(
				false, false, true, false), MORESPECIFIC(true, true, false,
				true), ADDED(true, false, false, false), HASID(true, true,
				false, true);
		public final boolean added;
		public final boolean better;
		public final boolean contradiction;
		public final boolean confirmation;

		Add(boolean a, boolean b, boolean c, boolean conf) {
			added = a;
			better = b;
			contradiction = c;
			confirmation = conf;
		}
	}

	/** Adds a fact, checks for functional duplicates */
	public Add add(final Fact fact, Set<String> functions) {
		if (fact.getSubject() == null || fact.getObject() == null) {
			Announce.debug("Null fact not added:", fact);
			return (Add.NULL);
		}
		if (facts.contains(fact)) {
			Announce.debug("Duplicate fact not added:", fact);
			return (Add.DUPLICATE);
		}
		if (fact.getSubject().equals(fact.getObject())) {
			Announce.debug("Identical arguments not added", fact);
			return (Add.DUPLICATE);
		}
		Add result = Add.ADDED;
		Map<String, List<Fact>> map = index.get(fact.getSubject());
		if (map != null && map.containsKey(fact.getRelation())) {
			for (Fact other : map.get(fact.getRelation())) {
				if (FactComponent.isMoreSpecific(fact.getObject(),
						other.getObject())) {
					Announce.debug("Removed", other, "because of newly added",
							fact);
					remove(other);
					result = Add.MORESPECIFIC;
					break;
				}
				if (FactComponent.isMoreSpecific(other.getObject(),
						fact.getObject())) {
					Announce.debug("More general fact not added:", fact,
							"because of", other);
					return (Add.TOOGENERAL);
				}
				if (!other.getObject().equals(fact.getObject()))
					continue;
				if (other.getId() != null && fact.getId() == null) {
					Announce.debug("Fact without id not added:", fact,
							"because of", other);
					return (Add.NOID);
				}
				if (other.getId() == null && fact.getId() != null) {
					Announce.debug("Removed", other, "because of newly added",
							fact);
					remove(other);
					result = Add.HASID;
					break;
				}
			}
			if (functions != null && functions.contains(fact.getRelation())
					&& !map.get(fact.getRelation()).isEmpty()) {
				Announce.debug(
						"Functional fact not added because another fact is already there:",
						fact, ". Already there:", map.get(fact.getRelation()));
				return (Add.FUNCLASH);
			}
		}
		return (justAdd(fact) ? result : Add.DUPLICATE);
	}

	/** Adds a fact, does not check for duplicates */
	public boolean justAdd(final Fact fact) {
		if (facts.contains(fact)) {
			Announce.debug("Duplicate fact not added:", fact);
			return (false);
		}
		boolean changed = false;

		// Handle relation
		String canonicalizedRelation = fact.getRelation();
		List<Fact> factsWithRelation = relindex.get(fact.getRelation());
		if (factsWithRelation == null) {
			relindex.put(fact.getRelation(),
					factsWithRelation = new ArrayList<Fact>(1));
		} else if (!factsWithRelation.isEmpty()) {
			canonicalizedRelation = factsWithRelation.get(0).getRelation();
			changed = true;
		}

		// Handle subject
		String canonicalizedSubject = fact.getSubject();
		Map<String, List<Fact>> relation2facts = index.get(fact.getSubject());
		if (relation2facts == null) {
			index.put(fact.getSubject(),
					relation2facts = new HashMap<String, List<Fact>>());
		} else if (!relation2facts.isEmpty()) {
			// Canonicaliztion could happen here
			// Removing it for reasons of speed
			/*
			 * String anyRel = relation2facts.keySet().iterator().next();
			 * List<Fact> anyFacts = relation2facts.get(anyRel); if
			 * (!anyFacts.isEmpty()) { canonicalizedSubject =
			 * anyFacts.get(0).getSubject(); changed = true; }
			 */
		}
		if (!relation2facts.containsKey(fact.getRelation()))
			relation2facts.put(fact.getRelation(), new ArrayList<Fact>(1));

		String canonicalizedObject = objects.get(fact.getObject());
		if (canonicalizedObject == null) {
			objects.put(canonicalizedObject = fact.getObject(),
					fact.getObject());
		} else {
			changed = true;
		}

		// Add the fact
		Fact newFact = changed ? new Fact(fact.getId(),canonicalizedSubject,
				canonicalizedRelation, canonicalizedObject) : fact;
		relindex.get(fact.getRelation()).add(newFact);
		relation2facts.get(fact.getRelation()).add(newFact);
		facts.add(newFact);
		return (true);
	}

	/** Empty list */
	protected static final List<Fact> EMPTY = new ArrayList<Fact>(0);

	/** Returns relations of this subject (or NULL) */
	public Set<String> getRelations(String arg1) {
		return (index.get(arg1).keySet());
	}

	/** Returns facts with matching first arg and relation */
	public List<Fact> getFactsWithSubjectAndRelation(String arg1,
			String relation) {
		Map<String, List<Fact>> map = index.get(arg1);
		if (map == null)
			return (Collections.emptyList());
		List<Fact> list = map.get(relation);
		if (list == null)
			return (Collections.emptyList());
		return (list);
	}

	/** TRUE if the subject exists */
	public boolean containsSubject(String arg1) {
		return (index.containsKey(arg1));
	}

	/** TRUE if the object exists */
	public boolean containsObject(String arg1) {
		return (objects.containsKey(arg1));
	}

	/** Returns facts with matching first arg */
	public List<Fact> collectFactsWithSubject(String arg1) {
		List<Fact> result = new ArrayList<>();
		if (!index.containsKey(arg1))
			return (result);
		for (Collection<Fact> facts : index.get(arg1).values()) {
			result.addAll(facts);
		}
		return (result);
	}

	/** Returns the first object (or null) */
	public String getObject(String arg1, String relation) {
		Map<String, List<Fact>> map = index.get(arg1);
		if (map == null)
			return (null);
		List<Fact> list = map.get(relation);
		if (list == null || list.isEmpty())
			return (null);
		return (list.get(0).getObject());
	}

	/** Returns the objects */
	public Set<String> getObjects() {
		return (objects.keySet());
	}

	/** Returns the objects with a given subject and relation */
	public Set<String> collectObjects(String subject, String relation) {
		Set<String> result = new TreeSet<>();
		for (Fact f : getFactsWithSubjectAndRelation(subject, relation)) {
			result.add(f.getArg(2));
		}
		return (result);
	}

	/** Returns the relations between this subject and this object */
	public Set<String> collectRelationsBetween(String arg1, String arg2) {
		Set<String> result = new TreeSet<>();
		for (Fact f : getFactsWithRelation(arg1)) {
			if (f.getObject().equals(arg2))
				result.add(f.getRelation());
		}
		return (result);
	}

	/** Returns facts with matching relation */
	public List<Fact> getFactsWithRelation(String relation) {
		if (!relindex.containsKey(relation))
			return (EMPTY);
		return (relindex.get(relation));
	}

	/**
	 * Returns facts with matching relation and second argument. This is very
	 * slow.
	 */
	public List<Fact> seekFactsWithRelationAndObject(String relation,
			String arg2) {
		if (!relindex.containsKey(relation))
			return (EMPTY);
		List<Fact> result = new ArrayList<Fact>();
		for (Fact f : relindex.get(relation)) {
			if (f.getObject().equals(arg2))
				result.add(f);
		}
		return (result);
	}

	/**
	 * Returns subjects with matching relation and second argument. This is very
	 * slow.
	 */
	public Set<String> seekSubjects(String relation, String arg2) {
		Set<String> result = new HashSet<>();
		for (Fact f : seekFactsWithRelationAndObject(relation, arg2)) {
			result.add(f.getArg(1));
		}
		return (result);
	}

	/** Loads from N4 file */
	public FactCollection(File n4File) throws IOException {
		this(n4File, false);
	}

	/** Loads from N4 file. FAST does not check duplicates */
	public FactCollection(File n4File, boolean fast) throws IOException {
		this(FactSource.from(n4File), fast);
	}

	/** Loads from N4 file. FAST does not check duplicates */
	public FactCollection(FactSource n4File) throws IOException {
		this(n4File, false);
	}

	/** Loads from N4 file. FAST does not check duplicates */
	public FactCollection(FactSource n4File, boolean fast) throws IOException {
		if (fast)
			loadFast(n4File);
		else
			load(n4File);
	}

	public FactCollection() {

	}

	/** Add facts */
	public boolean add(Iterable<Fact> facts) {
		boolean change = false;
		for (Fact f : facts)
			change |= add(f);
		return (change);
	}

	/** Removes a fact */
	public boolean remove(Object f) {
		if (!facts.remove(f))
			return (false);
		Fact fact = (Fact) f;
		index.get(fact.getSubject()).get(fact.getRelation()).remove(fact);
		relindex.get(fact.getRelation()).remove(fact);
		if (fact.getId() != null) {
			List<Fact> metaFacts = collectFactsWithSubject(fact.getId());
			for (Fact m : metaFacts) {
				remove(m);
			}
		}
		return (true);
	}

	/** Removes all facts */
	public void clear() {
		facts.clear();
		index.clear();
		relindex.clear();
	}

	/** Loads from N4 file */
	public void load(File n4File) throws IOException {
		load(FactSource.from(n4File));
	}

	/** Loads from N4 file, does not check duplicates */
	public void loadFast(File n4File) throws IOException {
		loadFast(FactSource.from(n4File));
	}

	/** Loads from N4 file, does not check duplicates */
	public void loadFast(FactSource reader) throws IOException {
		Announce.doing("Fast loading", reader);
		for (Fact f : reader) {
			justAdd(f);
		}
		Announce.done();
	}

	/** Loads from N4 file */
	public void load(FactSource reader) throws IOException {
		Announce.doing("Loading", reader);
		for (Fact f : reader) {
			add(f);
		}
		Announce.done();
	}

	@Override
	public Iterator<Fact> iterator() {
		return facts.iterator();
	}

	public int size() {
		return facts.size();
	}

	public String toString() {
		return facts.toString();
	}

	/** Maximal messages for comparison of fact collectioms */
	public static int maxMessages = 4;

	/** Checks if all of my facts are in the other set, prints differences */
	public boolean checkContainedIn(FactCollection goldStandard, String name) {
		boolean matches = true;
		next: for (Fact fact : facts) {
			for (Fact other : goldStandard.getFactsWithSubjectAndRelation(
					fact.getSubject(), fact.getRelation())) {
				if (other.getObject().equals(fact.getObject())) {
					// if (!D.equal(fact.getId(), other.getId()))
					// Announce.message("Different ids:", fact, other);
					continue next;
				}
			}
			Announce.message("Not found in", name, ":", fact);
			matches = false;
			if (--maxMessages <= 0)
				break;
		}
		return (matches);
	}

	/**
	 * Checks for differences, returns TRUE if equal, prints differences
	 * 
	 * @param goldStandardName
	 * @param thisName
	 */
	public boolean checkEqual(FactCollection goldStandard, String thisName,
			String goldStandardName) {
		boolean b = checkContainedIn(goldStandard, goldStandardName)
				& goldStandard.checkContainedIn(this, thisName);
		return (b);
	}

	/** TRUE if this collection contains this fact with any id */
	public boolean contains(String arg1, String rel, String arg2) {
		Map<String, List<Fact>> map = index.get(arg1);
		if (map == null)
			return (false);
		List<Fact> facts = map.get(rel);
		if (facts == null)
			return (false);
		for (Fact f : facts) {
			if (f.getArg(2).equals(arg2))
				return (true);
		}
		return (false);
	}

	/** Returns a map of subject-> object for a relation between strings */
	public Map<String, String> collectSubjectAndObjectAsStrings(String relation) {
		Map<String, String> objects = new HashMap<String, String>();
		for (Fact fact : getFactsWithRelation(relation)) {
			objects.put(fact.getArgJavaString(1), fact.getArgJavaString(2));
		}
		if (objects.isEmpty())
			Announce.warning("No instances of", relation, "found");
		return (objects);
	}

	/** Returns a set of strings for a type */
	public Set<String> seekStringsOfType(String type) {
		Set<String> result = new TreeSet<String>();
		for (Fact fact : seekFactsWithRelationAndObject("rdf:type", type)) {
			result.add(fact.getArgJavaString(1));
		}
		if (result.isEmpty())
			Announce.warning("No instances of", type, "found");
		return (result);
	}

	/** TRUE if the object is an instance of the class */
	public boolean instanceOf(String instance, String clss) {
		Collection<String> classes;
		if (FactComponent.isLiteral(instance)) {
			classes = Arrays.asList(FactComponent.getDatatype(instance));
		} else {
			classes = collectObjects(instance, RDFS.type);
		}
		for (String c : classes) {
			if (isSubClassOf(c, clss))
				return (true);
		}
		return (false);
	}

	/** TRUE if the first class is equal to or a subclass of the second */
	public boolean isSubClassOf(String sub, String supr) {
		if (sub.equals(supr))
			return (true);
		for (String s : collectObjects(sub, RDFS.subclassOf)) {
			if (isSubClassOf(s, supr))
				return (true);
		}
		return (false);
	}

	/** Adds the superclasses of this class */
	public void superClasses(String sub, Set<String> set) {
		set.add(sub);
		for (String s : collectObjects(sub, RDFS.subclassOf)) {
			superClasses(s, set);
		}
	}

	/** Adds the superclasses of this class */
	public Set<String> superClasses(String sub) {
		Set<String> set = new TreeSet<>();
		superClasses(sub, set);
		return (set);
	}

	/**
	 * Creates a map for quickly getting arg1 for a given arg2. Notice that this
	 * might overwrite arg1s that occur multiple times, make sure you know that
	 * they are unique.
	 * 
	 * This is useful for all the YAGO -> Other_KB_ID mappings
	 * 
	 * @param relation
	 *            relation for which to generate the reverse map
	 * @return reverse map
	 */
	public Map<String, String> getReverseMap(String relation) {
		Map<String, String> reverseMap = new HashMap<>();

		for (Fact f : getFactsWithRelation(relation)) {
			reverseMap.put(f.getArg(2), f.getArg(1));
		}

		return reverseMap;
	}

	/**
	 * Caches the preferred meanings map from the objects (as Java Strings) to
	 * the subjects (as entities)
	 */
	protected Map<String, String> preferredMeanings;

	/**
	 * Returns a map from the objects (as Java Strings) to the subjects (as
	 * entities). This map is generated on request and then cached.
	 */
	public synchronized Map<String, String> getPreferredMeanings() {
		if (preferredMeanings != null)
			return (preferredMeanings);
		preferredMeanings = new HashMap<>();
		for (Fact f : getFactsWithRelation("<isPreferredMeaningOf>")) {
			preferredMeanings.put(f.getArgJavaString(2), f.getArg(1));
		}
		return (preferredMeanings);
	}

	/** Returns all existing subjects */
	public Set<String> getSubjects() {
		return index.keySet();
	}

}
