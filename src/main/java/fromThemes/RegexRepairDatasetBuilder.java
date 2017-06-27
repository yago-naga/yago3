package fromThemes;

import java.io.File;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import basics.Fact;
import basics.FactComponent;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.InfoboxExtractor;
import javatools.parsers.Char17;
import utils.PatternList;
import utils.Theme;
import utils.termParsers.DateParser;
import utils.termParsers.NumberParser;

/**
 * Extracts the terms from the Infobox templates, builds the dataset that we use
 * for the Regex repairing.
 * 
 * This class is copyright 2016 Fabian M. Suchanek.
 */
public class RegexRepairDatasetBuilder extends InfoboxTermExtractor {

    @Override
    public Set<FollowUpExtractor> followUp() {
        return (new HashSet<>());
    }

    @Override
    public Set<Theme> output() {
        return (new HashSet<>());
    }

    @Override
    public void extract() throws Exception {
        File dateOutputFile = new File(this.input().iterator().next().file().getParentFile(),
                "regex-dataset-dates.txt");
        File numberOutputFile = new File(this.input().iterator().next().file().getParentFile(),
                "regex-dataset-numbers.txt");
        PatternList replacements = new PatternList(PatternHardExtractor.INFOBOXREPLACEMENTS, "<_infoboxReplace>");

        DateParser dateParser = new DateParser();
        NumberParser numberParser = new NumberParser();

        try (Writer datesOut = java.nio.file.Files.newBufferedWriter(dateOutputFile.toPath())) {
            try (Writer numbersOut = java.nio.file.Files.newBufferedWriter(numberOutputFile.toPath())) {
                for (Fact f : InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage(this.language)) {
                    String val = f.getObjectAsJavaString();
                    val = Char17.decodeAmpersand(val);
                    // Sometimes we get empty values here
                    if (val == null || val.isEmpty()) continue;
                    val = replacements.transform(val);
                    val = val.replace("$0", FactComponent.stripBrackets(f.getSubject()));
                    val = val.trim();
                    if (val.length() == 0) continue;
                    String transformed = dateParser.patternList.transform(val);
                    Matcher m = DateParser.resultPattern.matcher(transformed);
                    if (m.find()) datesOut.write(m.replaceAll("<date>$1</date>") + "\n");
                    transformed = numberParser.patternList.transform(val);
                    m = NumberParser.resultPattern.matcher(transformed);
                    if (m.find()) numbersOut.write(m.replaceAll("<number>$1</number>") + "\n");
                }
            }
        }
    }

    public RegexRepairDatasetBuilder(String lang) {
        super(lang);
    }

    public static void main(String[] args) throws Exception {
        RegexRepairDatasetBuilder extractor = new RegexRepairDatasetBuilder("en");
        extractor.extract(new File("c:/fabian/data/yago3"), "mapping infobox attributes into infobox facts");
    }

}
