/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

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

package followUp;

import java.util.*;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.AllEntitiesTypesExtractorFromWikidata;
import fromOtherSources.HardExtractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.EntityType;
import utils.FactCollection;
import utils.Theme;

/**
 * Does a type check
 * 
*/

public class TypeChecker extends FollowUpExtractor {

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
  }

  @Override
  public Set<Theme> input() {
    Set<Theme> res = new TreeSet<>();
    res.add(checkMe);
    res.add(HardExtractor.HARDWIREDFACTS);
    res.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    if (Extractor.includeConcepts) {
      res.add(AllEntitiesTypesExtractorFromWikidata.ALLENTITIES_WIKIDATA);//TODO Change this if the splitting theme is changed
    }

    return res;
  }

  /** Constructor, takes theme to be checked and theme to output */
  public TypeChecker(Theme in, Theme out, Extractor parent) {
    super(in, out, parent);
  }

  public TypeChecker(Theme in, Theme out) {
    this(in, out, null);
  }

  /** Holds the transitive types in a map from subjects to types */
  protected Map<String, Set<String>> types = new HashMap<>();

  /** Holds the kind of the entity id (entity or concept) */
  protected Map<String, EntityType> entities = new HashMap<>();

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
          Announce.debug("Kicked out", entity, "because it should have a language tag to be a", type);
          return (false);
        }
        literal[1] = YAGO.string;
      }
      if (schema.isSubClassOf(literal[1], type)) return (true);
      // Now try retro-typing: The parsed type xsd:integer
      // can fulfill xsd:nonNegativeInteger, if it matches the syntax
      // check.
      // For this, the parsed type has to be a superclass of the expected
      // type.
      if (!schema.isSubClassOf(type, literal[1])) {
        Announce.debug("Kicked out", entity, "because its cannot be retro-typed to", type);
        return (false);
      }
      String syntaxChecker = FactComponent.asJavaString(schema.getObject(type, "<_hasTypeCheckPattern>"));
      if (syntaxChecker == null) return (false);
      if (FactComponent.asJavaString(entity).matches(syntaxChecker)) return (true);
      Announce.debug("Kicked out", entity, "because its does not match the syntaxcheck", syntaxChecker, "of", type);
      return (false);
    }

    // Check taxonomical type
    switch (type) {
      case RDFS.resource:
        return (true);
      case YAGO.entity:
        if (Extractor.includeConcepts && entities.containsKey(entity) && entities.get(entity) != EntityType.NAMED_ENTITY) {
          return (entities.containsKey(entity));
        }
        else {
          return (types.containsKey(entity));
        }
      case RDFS.statement:
        return (FactComponent.isFactId(entity));
      case RDFS.clss:
        return (FactComponent.isClass(entity));
      case YAGO.url:
        return (entity.startsWith("<http"));
    }

    if (Extractor.includeConcepts && entities.containsKey(entity) && entities.get(entity) != EntityType.NAMED_ENTITY) {
      return true;
    }
    Set<String> myTypes = types.get(entity);
    return (myTypes != null && myTypes.contains(type));
  }

  @Override
  public void extract() throws Exception {
    if (Extractor.includeConcepts) {
      entities = AllEntitiesTypesExtractorFromWikidata.getAllEntitiesToSplitType();
    }
    types = TransitiveTypeExtractor.getSubjectToTypes();

    schema = HardExtractor.HARDWIREDFACTS.factCollection();
    Announce.doing("Type-checking facts of", checkMe);
    for (Fact f : checkMe) {
      if (check(f)) checked.write(f);
    }
    if (!untypedRelations.isEmpty()) {
      Announce.warning("Untypes relations:", untypedRelations);
    }
    schema = null;
    types = null;
    Announce.done();
  }
}
