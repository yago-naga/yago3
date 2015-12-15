package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.Theme.ThemeGroup;
import extractors.Extractor.ImplementationNote;

/**
 * Class MultilingualTheme
 * 
 * This code is part of the YAGO project at the Max Planck Institute for
 * Informatics and the Telecom ParisTech University. It is licensed under a
 * Creative Commons Attribution License by the YAGO team:
 * https://creativecommons.org/licenses/by/3.0/
 * 
 * This class represents a theme that can be instantiated in several languages.
 * 
 * @author Fabian M. Suchanek
 * 
 */

public class MultilingualTheme {

	/** Types of my theme */
	public final ThemeGroup themeGroup;

	/** Name of the theme */
	public final String name;

	/** Description of the theme */
	public final String description;

	public MultilingualTheme(String name, String description) {
		this(name, description, name.startsWith("yago") ? ThemeGroup.OTHER
				: ThemeGroup.INTERNAL);
	}

	public MultilingualTheme(String name, String description, ThemeGroup group) {
		this.name = name;
		this.description = description;
		themeGroup = group;
	}

	/** Maps languages to themes */
	protected Map<String, Theme> language2theme = new HashMap<String, Theme>();

	/** Returns the theme in the given languages, in that order */
	@ImplementationNote("The order is important, because in case of fact conflict, the first language prevails")
	public List<Theme> inLanguages(List<String> languages) {
		List<Theme> result = new ArrayList<>();
		for (String language : languages)
			result.add(inLanguage(language));
		return (result);
	}

	/** Returns the theme in English */
	public Theme inEnglish() {
		Theme r = language2theme.get("en");
		if (r != null)
			return (r);
		r = language2theme.get("eng");
		if (r != null)
			return (r);
		return (inLanguage("en"));
	}

	/** Returns this theme in a language */
	public Theme inLanguage(String lang) {
		Theme result = language2theme.get(lang);
		if (result == null) {
			result = new Theme(name, lang, description, themeGroup);
			language2theme.put(lang, result);
		}
		return (result);
	}

}
