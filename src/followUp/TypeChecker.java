package followUp;

import java.util.HashSet;
import java.util.Set;

import utils.FactCollection;
import utils.Theme;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.HardExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * YAGO2s - TypeChecker
 * 
 * Does a type check
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TypeChecker extends FollowUpExtractor {

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(HardExtractor.HARDWIREDFACTS,
				TransitiveTypeExtractor.TRANSITIVETYPE);
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(checkMe,
				TransitiveTypeExtractor.TRANSITIVETYPE,
				HardExtractor.HARDWIREDFACTS);
	}

	/** Constructor, takes theme to be checked and theme to output */
	public TypeChecker(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
	}

	public TypeChecker(Theme in, Theme out) {
		this(in, out, null);
	}

	/** Holds the transitive types */
	protected FactCollection types;

	/** Holds Relations without domain/range */
	protected Set<String> untypedRelations = new HashSet<>();

	/** Holds the schema */
	protected FactCollection schema;

	/** Type checks a fact. */
	public boolean check(Fact fact) {

		String domain = schema.getObject(fact.getRelation(), RDFS.domain);
		if (domain == null) {
			Announce.debug("No domain found for", fact.getRelation());
			untypedRelations.add(fact.getRelation());
			return (true);
		}
		if (!check(fact.getArg(1), domain)) {
			Announce.debug("Domain check failed", fact);
			return (false);
		}
		String range = schema.getObject(fact.getRelation(), RDFS.range);
		if (range == null) {
			Announce.debug("No range found for", fact.getRelation());
			untypedRelations.add(fact.getRelation());
			return (true);
		}
		if (!check(fact.getArg(2), range)) {
			Announce.debug("Range check failed", fact);
			return (false);
		}
		return (true);
	}

	/** Checks whether an entity is of a type */
	public boolean check(String entity, String type) {

		/*
		 * // This is the old code, replaced by the new code below.
		 * 
		 * // Check syntax String syntaxChecker =
		 * FactComponent.asJavaString(schema.getObject( type,
		 * "<_hasTypeCheckPattern>"));
		 * 
		 * if (syntaxChecker != null && FactComponent.asJavaString(entity) !=
		 * null && !FactComponent.asJavaString(entity).matches(syntaxChecker)) {
		 * Announce.debug("Typechecking", entity, "for", entity, type,
		 * "does not match syntax check", syntaxChecker); return false; }
		 * 
		 * // Check data type if (FactComponent.isLiteral(entity)) { String
		 * parsedDatatype = FactComponent.getDatatype(entity); if
		 * (parsedDatatype == null) parsedDatatype = YAGO.string; if
		 * (syntaxChecker != null && schema.isSubClassOf(type, parsedDatatype))
		 * { // If the // syntax // check // went // through, // we are // fine
		 * entity = FactComponent.setDataType(entity, type); } else { //
		 * Otherwise, we check if the datatype is OK if
		 * (!schema.isSubClassOf(parsedDatatype, type)) {
		 * Announce.debug("Extraction", entity, "for", entity, parsedDatatype,
		 * "does not match data type check", type); return false; } } }
		 */
		// Is it a literal?
		String[] literal = FactComponent.literalAndDatatypeAndLanguage(entity);
		if (literal != null) {
			// Literals without data types are strings
			if (literal[1] == null) {
				if (type.equals(YAGO.languageString) && literal[2] == null) {
					Announce.debug("Kicked out", entity,
							"because it should have a language tag to be a",
							type);
					return (false);
				}
				literal[1] = YAGO.string;
			}
			if (schema.isSubClassOf(literal[1], type))
				return (true);
			// Now try retro-typing: The parsed type xsd:integer
			// can fulfill xsd:nonNegativeInteger, if it matches the syntax
			// check.
			// For this, the parsed type has to be a superclass of the expected
			// type.
			if (!schema.isSubClassOf(type, literal[1])) {
				Announce.debug("Kicked out", entity,
						"because its cannot be retro-typed to", type);
				return (false);
			}
			String syntaxChecker = FactComponent.asJavaString(schema.getObject(
					type, "<_hasTypeCheckPattern>"));
			if (syntaxChecker == null)
				return (false);
			if (FactComponent.asJavaString(entity).matches(syntaxChecker))
				return (true);
			Announce.debug("Kicked out", entity,
					"because its does not match the syntaxcheck",
					syntaxChecker, "of", type);
			return (false);
		}

		// Check taxonomical type
		switch (type) {
		case RDFS.resource:
			return (true);
		case YAGO.entity:
			return (types.containsSubject(entity));
		case RDFS.statement:
			return (FactComponent.isFactId(entity));
		case RDFS.clss:
			return (FactComponent.isClass(entity));
		case YAGO.url:
			return (entity.startsWith("<http"));
		}

		Set<String> myTypes = types.collectObjects(entity, RDFS.type);
		return (myTypes != null && myTypes.contains(type));
	}

	@Override
	public void extract() throws Exception {
		types = TransitiveTypeExtractor.TRANSITIVETYPE.factCollection();
		schema = HardExtractor.HARDWIREDFACTS.factCollection();
		Announce.doing("Type-checking facts of", checkMe);
		for (Fact f : checkMe) {
			if (check(f))
				checked.write(f);
		}
		if (!untypedRelations.isEmpty()) {
			Announce.warning("Untypes relations:", untypedRelations);
		}
		schema = null;
		types = null;
		Announce.done();
	}

}
