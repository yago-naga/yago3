package followUp;

import java.util.Map;

import extractors.Extractor;
import fromOtherSources.DictionaryExtractor;
import utils.Theme;

/**
 * InfoboxTemplateTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English
 * language. Objects are infobox templates.
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Farzaneh Mahdisoltani, with contributions from Fabian M. Suchanek.

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

public class InfoboxTemplateTranslator extends EntityTranslator {

  @Override
  protected String translateObject(String object, Map<String, String> dictionary) {
    return dictionary.get(object);
  }

  public InfoboxTemplateTranslator(Theme in, Theme out, Extractor parent) {
    super(in, out, parent);
    objectDictionaryTheme = DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY.inLanguage(language);
  }
}
