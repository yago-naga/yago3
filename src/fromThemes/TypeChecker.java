package fromThemes;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fromOtherSources.HardExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.Extractor.FollowUpExtractor;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;

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
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(checkMe,
				TransitiveTypeExtractor.TRANSITIVETYPE,
				HardExtractor.HARDWIREDFACTS));
	}

	/** Constructor, takes theme to be checked and theme to output */
	public TypeChecker(Theme in, Theme out, Extractor parent) {
		checkMe = in;
		checked = out;
		this.parent = parent;
	}

	public TypeChecker(Theme in, Theme out) {
		this(in, out, null);
	}

	/** Holds the transitive types */
	protected Map<String, Set<String>> types;

	/** Holds the schema */
	protected FactCollection schema;

	/** Type checks a fact. */
	public boolean check(Fact fact) {

		String domain = schema.getArg2(fact.getRelation(), RDFS.domain);
		if (!check(fact.getArg(1), domain)) {
			Announce.debug("Domain check failed", fact);
			return (false);
		}
		String range = schema.getArg2(fact.getRelation(), RDFS.range);
		if (!check(fact.getArg(2), range)) {
			Announce.debug("Range check failed", fact);
			return (false);
		}
		return (true);
	}

	/** Checks whether an entity is of a type. TRUE if the type is NULL */
	public boolean check(String entity, String type) {

		// Check syntax
		String syntaxChecker = FactComponent.asJavaString(schema.getArg2(type,
				"<_hasTypeCheckPattern>"));

		if (syntaxChecker != null && FactComponent.asJavaString(entity) != null
				&& !FactComponent.asJavaString(entity).matches(syntaxChecker)) {
			Announce.debug("Typechecking", entity, "for", entity, type,
					"does not match syntax check", syntaxChecker);
			return false;
		}

		// Check data type
		if (FactComponent.isLiteral(entity)) {
			String parsedDatatype = FactComponent.getDatatype(entity);
			if (parsedDatatype == null)
				parsedDatatype = YAGO.string;
			if (syntaxChecker != null
					&& schema.isSubClassOf(type, parsedDatatype)) {
				// If the syntax check went through, we are fine
				entity = FactComponent.setDataType(entity, type);
			} else {
				// Otherwise, we check if the datatype is OK
				if (!schema.isSubClassOf(parsedDatatype, type)) {
					Announce.debug("Extraction", entity, "for", entity,
							parsedDatatype, "does not match data type check",
							type);
					return false;
				}
			}
		}

		// Check taxonomical type
		if (type == null)
			return (true);
		if (type.equals(RDFS.resource))
			return (true);
		if (type.equals(YAGO.entity)) {
			return (types.containsKey(entity));
		}
		if (type.equals(RDFS.statement)) {
			return (FactComponent.isFactId(entity));
		}
		if (type.equals(RDFS.clss)) {
			return (entity.startsWith("<wordnet_"));
		}
		if (type.equals(YAGO.url)) {
			return (entity.startsWith("<http"));
		}
		// Is it a literal?
		String[] literal = FactComponent.literalAndDatatypeAndLanguage(entity);
		if (literal != null) {
			if (literal[1] == null)
				return (type.equals(YAGO.string) || type
						.equals(YAGO.languageString));
			return (schema.isSubClassOf(literal[1], type));
		}
		Set<String> myTypes = types.get(entity);
		return (myTypes != null && myTypes.contains(type));
	}

	@Override
	public void extract(Map<Theme, FactWriter> output,
			Map<Theme, FactSource> input) throws Exception {
		types = TransitiveTypeExtractor.yagoTaxonomy(input);
		schema = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
		Announce.doing("Type-checking facts of", checkMe);
		FactWriter w = output.get(checked);
		for (Fact f : input.get(checkMe)) {
			if (check(f))
				w.write(f);
		}
		Announce.done();
	}

}
