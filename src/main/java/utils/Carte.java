package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactSource;
import basics.N4Writer;
import basics.TsvReader;
import basics.YAGO;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.filehandlers.FileSet;
import utils.Theme.ThemeGroup;

/**
 * Produces a Web page snippet for YAGO3 a la carte
 * 
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
public class Carte {

  /** Number of lines in the preview file*/
  public static final int previewLines = 100;

  /** Colors to use in the list of themes*/
  public static final String[] colors = { "Lavender", "SandyBrown", "PaleGreen", "LightBlue", "LightPink", "Khaki", "WhiteSmoke" };

  /** Creates the HTML carte for YAGO. First argument: YAGO folder. Second argument: Folder where the carte and previews should go.*/
  public static void main(String[] args) throws Exception {
    //args = new String[] { "c:/fabian/data/yago3", "c:/fabian/data/yago3" };
    if (args.length != 2) Announce.help("Carte <YAGO folder> <Web folder>", "", "Creates carte.html and preview files for all YAGO themes");
    Announce.doing("Creating Web page 'YAGO a la Carte'");
    File yagoFolder = new File(args[0]);
    File targetFolder = new File(args[1]);
    Announce.message("Yago folder:", yagoFolder);
    Announce.message("Web folder:", targetFolder);
    if (yagoFolder.listFiles() == null) Announce.error("Yago folder does not exist");
    if (targetFolder.listFiles() == null) Announce.error("Web folder does not exist");

    Map<ThemeGroup, Set<File>> groups = new HashMap<>();
    Map<File, String> descriptions = new HashMap<>();

    Announce.doing("Loading files");
    for (File f : yagoFolder.listFiles()) {
      if (!f.getName().startsWith("yago") || !f.getName().endsWith(".ttl")) continue;
      Announce.doing("Treating", f.getName());
      try (N4Writer preview = new N4Writer(new File(targetFolder, FileSet.newExtension(f.getName(), "txt")), null)) {
        int counter = 0;
        for (Fact fact : FactSource.from(f)) {
          if (descriptions.get(f) == null && fact.getRelation().equals(YAGO.hasGloss)) {
            String[] glossAndGroup = TsvReader.glossAndGroup(fact.getArgJavaString(2));
            descriptions.put(f, glossAndGroup[0]);
            ThemeGroup group = ThemeGroup.of(glossAndGroup[1]);
            if (group == null) group = ThemeGroup.OTHER;
            D.addKeyValue(groups, group, f, HashSet.class);
          }
          preview.write(fact);
          if (counter++ > previewLines) break;
        }
      }
      Announce.done();
    }
    Announce.done();

    Announce.doing("Writing carte");
    try (Writer w = new FileWriter(new File(targetFolder, "carte.html"))) {
      w.write("<HTML><HEAD><STYLE>td {padding:10}</STYLE></HEAD><BODY><TABLE style='border-collapse:collapse;' >\n");
      // Run through the groups in the order specified in ThemeGroup (!)
      for (ThemeGroup group : ThemeGroup.values()) {
        if (groups.get(group) == null || groups.get(group).size() == 0) {
          Announce.warning("No themes in group", group);
          continue;
        }
        boolean wroteGroup = false;
        for (File f : groups.get(group)) {
          String cellstyle = " style='background-color:" + colors[group.ordinal() % colors.length] + "'";
          w.write("<tr><td" + cellstyle + ">");
          if (!wroteGroup) w.write(group.toString());
          wroteGroup = true;
          w.write("\n  <td" + cellstyle + "><b>" + FileSet.newExtension(f.getName(), null) + "</b>\n");
          w.write("      <br>" + descriptions.get(f) + "\n");
          w.write("  <td" + cellstyle + "><a href='" + FileSet.newExtension(f.getName(), "txt") + "'>Preview</a>\n");
          w.write("  <td" + cellstyle + "><a href='" + FileSet.newExtension(f.getName(), "ttl.7z") + "'>Download&nbsp;TTL</a>\n");
          w.write("  <td" + cellstyle + "><a href='" + FileSet.newExtension(f.getName(), "tsv.7z") + "'>Download&nbsp;TSV</a>\n");
        }
      }
      w.write("</BODY></HTML>");
    }
    Announce.done();
    Announce.done();
  }
}
