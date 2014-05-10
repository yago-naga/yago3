package fromWikipedia;

/**
 * MultilingualComparator - YAGO2s
 * 
 * Finds the confirming facts, new facts, and missing facts, for Types in each
 * language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class MultilingualComparator {

	/*
	 * public static final BaseTheme STATISTICS = new
	 * BaseTheme("multilingualStatistics",
	 * "Statistics on how many facts the multilingual extraction added"); public
	 * static final BaseTheme CONFIRMING_FACTS= new BaseTheme("confirmingFacts",
	 * "English facts that got confirmed"); public static final BaseTheme
	 * MISSING_FACTS= new BaseTheme("multilingualStatistics",
	 * "Statistics on how many facts the multilingual extraction added"); public
	 * static final BaseTheme STATISTICS = new
	 * BaseTheme("multilingualStatistics",
	 * "Statistics on how many facts the multilingual extraction added"); public
	 * static final BaseTheme STATISTICS = new
	 * BaseTheme("multilingualStatistics",
	 * "Statistics on how many facts the multilingual extraction added"); public
	 * static final BaseTheme STATISTICS = new
	 * BaseTheme("multilingualStatistics",
	 * "Statistics on how many facts the multilingual extraction added");
	 * 
	 * public Set<Theme> input() { return new
	 * FinalSet<Theme>(TypeCoherenceChecker.YAGOTYPES,
	 * CategoryTypeExtractor.CATEGORYTYPES.inLanguage(language),
	 * InfoboxTypeExtractor.INFOBOXTYPES.inLanguage(language),
	 * CategoryTypeExtractor.CATEGORYTYPES.inLanguage(language)); }
	 * 
	 * @Override public Set<Theme> output() { return new
	 * FinalSet<Theme>(STATISTICS.inLanguage(language)); }
	 * 
	 * @Override public void extract() throws Exception { FactSource yagoTypes =
	 * input.get(TypeCoherenceChecker.YAGOTYPES); ExtendedFactCollection
	 * baseLangFactCollection = new ExtendedFactCollection();
	 * loadFacts(input.get
	 * (CategoryTypeExtractor.CATEGORYTYPES.inLanguage(baseLang)),
	 * baseLangFactCollection);
	 * loadFacts(input.get(InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(baseLang)),
	 * baseLangFactCollection); Set<String> baseLangEntities =
	 * baseLangFactCollection.getSubjects();
	 * 
	 * ExtendedFactCollection secondLangFactCollection = new
	 * ExtendedFactCollection();
	 * loadFacts(input.get(CategoryTypeExtractor.CATEGORYTYPES
	 * .inLanguage(language)), secondLangFactCollection);
	 * loadFacts(input.get(InfoboxTypeExtractor.INFOBOXTYPES_MAP.get(language)),
	 * secondLangFactCollection); Set<String> secondLangEntities =
	 * baseLangFactCollection.getSubjects();
	 * 
	 * int missingFacts = 0; int missingEntities = 0; int newFacts = 0; int
	 * newEntities = 0; int confirmingFacts = 0; int confirmingEntities = 0;
	 * 
	 * for (Fact f : yagoTypes) { if
	 * (secondLangFactCollection.contains(f.getArg(1), f.getRelation(),
	 * f.getArg(2))) { if (baseLangFactCollection.contains(f.getArg(1),
	 * f.getRelation(), f.getArg(2))) { confirmingFacts++;
	 * output.get(CONFIRMINGFACTS_MAP.get(language)).write(f); } else {
	 * newFacts++; output.get(NEWFACTS_MAP.get(language)).write(f); } } else if
	 * (baseLangFactCollection.contains(f.getArg(1), f.getRelation(),
	 * f.getArg(2))) { missingFacts++;
	 * output.get(MISSINGFACTS_MAP.get(language)).write(f); }
	 * 
	 * if (secondLangEntities.contains(f.getArg(1))) { if
	 * (baseLangEntities.contains(f.getArg(1))) { confirmingEntities++;
	 * output.get(CONFIRMINGENTITIES_MAP.get(language)).write(new
	 * Fact(f.getArg(1), "", "")); } else { newEntities++;
	 * output.get(NEWENTITIES_MAP.get(language)).write(new Fact(f.getArg(1), "",
	 * "")); } } else if (baseLangEntities.contains(f.getArg(1))) {
	 * missingEntities++;
	 * output.get(MISSINGENTITIES_MAP.get(language)).write(new Fact(f.getArg(1),
	 * "", "")); }
	 * 
	 * } output.get(MISSINGFACTS_MAP.get(language)).write(new
	 * Fact("Missing_facts", "hasNumberOf", missingFacts + ""));
	 * output.get(MISSINGENTITIES_MAP.get(language)).write(new
	 * Fact("Missing_entities", "hasNumberOf", missingEntities + ""));
	 * output.get(CONFIRMINGFACTS_MAP.get(language)).write(new
	 * Fact("Confirming_facts", "hasNumberOf", confirmingFacts + ""));
	 * output.get(CONFIRMINGENTITIES_MAP.get(language)).write(new
	 * Fact("Confirming_entities", "hasNumberOf", confirmingEntities + ""));
	 * output.get(NEWFACTS_MAP.get(language)).write(new Fact("New_facts",
	 * "hasNumberOf", newFacts + ""));
	 * output.get(NEWENTITIES_MAP.get(language)).write(new Fact("New_entities",
	 * "hasNumberOf", newEntities + ""));
	 * 
	 * }
	 * 
	 * public MultilingualComparator(String baseLanguage, String language) {
	 * this.baseLang = baseLanguage; this.language = language; }
	 * 
	 * public static void main(String[] args) throws Exception { new
	 * MultilingualComparator("en", "de").extract(new File("D:/data3/yago2s/"),
	 * "nnn"); }
	 */
}
