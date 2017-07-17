package fromThemes;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    File dateOutputFile = new File(this.input().iterator().next().file().getParentFile(), "regex-dataset-dates.txt");
    File numberOutputFile = new File(this.input().iterator().next().file().getParentFile(), "regex-dataset-numbers.txt");
    PatternList replacements = new PatternList(PatternHardExtractor.INFOBOXREPLACEMENTS, "<_infoboxReplace>");

    DateParser dateParser = new DateParser();
    NumberParser numberParser = new NumberParser();

    Pattern datePattern = Pattern.compile("_result_\\d+-\\d+-\\d+_");
    Pattern numberPattern = Pattern.compile("_result_[^_]*_<[^>%]*>_");
    Pattern numberPatternExclude = Pattern.compile("<(percent|px)>");

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

          String newVal = mark(val, dateParser.patternList, datePattern, null, "date");
          if (newVal != null) {
            datesOut.write("\t" + f.getSubject() + "\t" + f.getRelation() + "\t" + FactComponent.forString(newVal) + "\n");
          }
          newVal = mark(val, numberParser.patternList, numberPattern, numberPatternExclude, "number");
          if (newVal != null) {
            numbersOut.write("\t" + f.getSubject() + "\t" + f.getRelation() + "\t" + FactComponent.forString(newVal) + "\n");
          }
        }
      }
    }
  }

  protected String mark(String val, PatternList patternList, Pattern pattern, Pattern exclude, String tag) {
    List<Integer> startIdx = new ArrayList<>(), endIdx = new ArrayList<>();
    String transformed = patternList.transformWithProvenance(val, startIdx, endIdx);
    Matcher m = pattern.matcher(transformed);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      int lastValPos = 0;
      do {
        if (exclude != null && exclude.matcher(m.group()).find()) {
          continue;
        }
        int start = Collections.min(startIdx.subList(m.start(), m.end()));
        int end = Collections.max(endIdx.subList(m.start(), m.end()));
        sb.append(val.substring(lastValPos, start));
        sb.append("<").append(tag).append(">");
        sb.append(val.substring(start, end));
        sb.append("</").append(tag).append(">");
        lastValPos = end;
      } while (m.find());
      sb.append(val.substring(lastValPos));

      if (lastValPos != 0) {
        return sb.toString();
      }
      //datesOut.write(m.replaceAll("<date>$1</date>") + "\n");
    }
    return null;
  }

  public RegexRepairDatasetBuilder(String lang) {
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      args = new String[] { "/home/tr/tmp/yago3-debug/" };
    }
    RegexRepairDatasetBuilder extractor = new RegexRepairDatasetBuilder("en");
    extractor.extract(new File(args[0]), "mapping infobox attributes into infobox facts");
  }

}
