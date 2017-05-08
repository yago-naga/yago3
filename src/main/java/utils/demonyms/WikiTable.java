package utils.demonyms;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Extract Wikipedia table as list of rows (which is a list of strings representing the cells in that row).
 * Headers are extracted to the headers list. This class takes rowspan and colspan into account, but removes all other attributes.
 *
 * @author Thomas Rebele
 *
 */
public class WikiTable {

  private static Pattern wikitable = Pattern
      .compile("\\{\\|\\s*class=\"[^\"]*wikitable[^\"]*\"[^\\|]*(?<content>(((?!\\|\\}|\\{\\|\\s*class).)*\\R?)*)\\|\\}");

  public List<List<String>> headers = new ArrayList<>();

  public List<List<String>> rows = new ArrayList<>();

  private static class Helper {

    String remaining = "";
  }

  /**
   * Transform the content of a table (within {| ... |} marks) to a WikiTable
   * @param content
   * @return
   */
  public static WikiTable parse(String content) {
    WikiTable result = new WikiTable();

    StringReader reader = new StringReader(content);
    // split rows
    List<String> rawRows = Arrays.asList(content.split("\\r?\\n(\\|-|</tr>)\\r?\\n"));
    for (String rawRow : rawRows) {
      // split cols
      List<String> cols = Arrays.asList(rawRow.trim().split("\\s*([\\|!]{2}|\\R[\\|!])\\s*"));
      if (cols.size() == 0) continue;

      // determine whether header or normal row
      List<List<String>> dest = rawRow.contains("!") ? result.headers : result.rows;
      cols = cols.stream().map(str -> {
        // remove | and ! at start of line
        if (str.startsWith("|") || str.startsWith("!")) {
          return str.replaceAll("^[\\|!]\\s*", "");
        }
        return str;
      }).collect(Collectors.toList());

      // Q&D fix for description at the end of the table
      if (cols.size() == 1 && cols.get(0).startsWith("colspan=")) continue;
      dest.add(cols);
    }

    // deal with rowspan and colspan
    applyAttributes(result.headers);
    applyAttributes(result.rows);
    return result;
  }

  static Pattern cell = Pattern.compile("(?<attribute>[^\\[\\{\\}\\]]*\\|)?(?<content>.*)");

  static Map<String, Pattern> attribPattern = new HashMap<>();

  /**
   * Read an attribute of the form name="value" or name=number
   * @param name of the attribute
   * @param attrib containing the input string, and the remaining after removing the attribute
   * @return value of the attribute
   */
  private static String getAttrib(String name, Helper attrib) {
    if (attrib.remaining == null) return null;
    Pattern p = attribPattern.computeIfAbsent(name, n -> Pattern.compile("\\b" + Pattern.quote(name) + "=(?<arg>\"[^\"]*\"|\\d*)"));

    Matcher m = p.matcher(attrib.remaining);
    if (m.find()) {
      attrib.remaining = m.replaceFirst("");
      String content = m.group("arg");
      if (content.startsWith("\"") && content.endsWith("\"")) {
        content = content.substring(1, content.length() - 1);
      }
      return content;
    }
    return null;
  }

  /**
   * Apply rowspan and colspan attributes. Remove attributes afterwards.
   * @param rows
   */
  private static void applyAttributes(List<List<String>> rows) {
    for (int i = 0; i < rows.size(); i++) {

      List<String> row = rows.get(i), newRow = new ArrayList<>();
      for (int j = 0; j < row.size(); j++) {
        Matcher m = cell.matcher(row.get(j));
        if (m.matches()) {
          String attrib = m.group("attribute");
          String content = m.group("content");
          if (attrib != null) {
            Helper h = new Helper();
            h.remaining = attrib;

            // deal with rowspan
            String rawRowspan = getAttrib("rowspan", h);
            if (rawRowspan != null) {
              int rowspan = Integer.parseInt(rawRowspan);
              String newcell = h.remaining + "|" + content;
              for (int spani = 1; spani < rowspan; spani++) {
                List<String> tmpRow = rows.get(i + spani);
                tmpRow.add(j, newcell);
              }
            }

            // deal with colspan
            String rawColspan = getAttrib("colspan", h);
            if (rawColspan != null) {
              int colspan = Integer.parseInt(rawColspan);
              String newcell = h.remaining + "|" + content;
              for (int spani = 1; spani < colspan; spani++) {
                row.add(j + 1, newcell);
              }
            }
          }
          newRow.add(content);
        }
      }
      rows.set(i, newRow);
    }
  }

  /**
   * Get all tables of a page. Cannot deal with nested tables.
   * @param page code of Wikipedia page (wiki code)
   * @return
   */
  public static List<WikiTable> getTables(String page) {
    List<WikiTable> result = new ArrayList<>();
    Matcher m = wikitable.matcher(page);

    while (m.find()) {
      result.add(parse(m.group("content")));
    }
    return result;
  }
}
