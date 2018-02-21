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

package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import basics.Fact;
import utils.Theme.ThemeGroup;

/**
 * This class represents a theme that can be instantiated in several languages.
*/

public class MultilingualTheme {

  /** Types of my theme */
  public final ThemeGroup themeGroup;

  /** Name of the theme */
  public final String name;

  /** Description of the theme */
  public final String description;

  public MultilingualTheme(String name, String description) {
    this(name, description, name.startsWith("yago") ? ThemeGroup.OTHER : ThemeGroup.INTERNAL);
  }

  public MultilingualTheme(String name, String description, ThemeGroup group) {
    this.name = name;
    this.description = description;
    themeGroup = group;
  }

  /** Maps languages to themes */
  protected Map<String, Theme> language2theme = new HashMap<String, Theme>();

  /** Returns the theme in the given languages, in that order */
  @Fact.ImplementationNote("The order is important, because in case of fact conflict, the first language prevails")
  public List<Theme> inLanguages(List<String> languages) {
    List<Theme> result = new ArrayList<>();
    for (String language : languages)
      result.add(inLanguage(language));
    return (result);
  }

  /** Returns the theme in English */
  public Theme inEnglish() {
    Theme r = language2theme.get("en");
    if (r != null) return (r);
    r = language2theme.get("eng");
    if (r != null) return (r);
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
