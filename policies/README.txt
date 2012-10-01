YAGO2s: Infrastructure

This document describes the technical infrastructure of YAGO2s,
meaning how it can be run, what files serve which purpose, and
what tools are available.

--------- Structure of the Project

The following Java projects belong to YAGO2s
* Javatools
  These classes are Java utilities. They are shared with other MPI projects.
* Basics2s
  These classes are used to represent facts, TSV files, etc.
  The files in "data" describe the schema of YAGO.
  This project can be shared with others or be externalized.
* Converters2s
  These classes convert YAGO into the Jena-Format, into TSV, and
  into SVG (graphical browser). 
  They also contain the Flight Planner, the YAGO-a-la-carte HTML generator,
  and an experimental YAGO in-memory database.
  The info-folder contains the FAQ of YAGO, which is also on the Web page.
  The script folder contains a script to load YAGO into Postgres, and also to
  create the SQL tables of the Flight Planner.
  It also contains a readme, which explains how to run the converters.
  This project can be shared and externalized, even though YagoDb does not yet work
  and might have to be excluded, Carte.java is not really necessary externally,
  and the Flight Planner should possibly be excluded before publication of the
  BTW demo paper. It is not clear whether the rest is of use to anybody.
* YAGO2s
  This project contains all main YAGO extractors, together with
  - their data (patterns etc.)
  - policies that describe the code, architecture, and data format 
  - scripts that run YAGO (s.b.)
  - test cases (see policies)

--------- Running the YAGO extractors

The script to run YAGO are in yago2s/scripts. 
The scripts and the ini-files are example files.
They have to be copied to the root folder of the projects,
and paths might have to be adjusted there.

The scripts are (in this order):
0) update.sh
  updates the YAGO code from the SVN, compiles all classes 
1) trun.sh
  Does a test run on the extraction, by running yago_test.ini
  We should first run this to see if the extraction works
2) prun.sh
  Does the full run for YAGO in parallel,
  based on yago.ini
3) tsv.sh
  Converts YAGO to tsv files
4) db.sh
  Loads YAGO into the database
5) flightsb.sh
  Creates the database tables that are necessary for the Flight Planner

All scripts log their output into a file, and print this file continuously
to the screen.
All scripts run in background. That means that you can
(1) interrupt with CTRL+C, but it will only interrupt the printing, not the program
(2) log off, and the program will continue running

--------- Putting YAGO online

The YAGO Web presence consists of the following parts:
* The Web page
  This page is in the shared file system under
    /www/inf-websites/www.mpi-inf.mpg.de/yago-naga/yago
* The FAQ
  The FAQ takes one tab on the Web page. It is also checked in in the
  SVN under converters2s/faq
  --> It should be updated to YAGO2s! (RDF/OWL identifiers, new format, etc.)     
* The data
  YAGO2s shall be available in final themes. These are all themes
  that start with "yago...". Each theme shall be available
  - in the native N4/TTL
  - in TSV
  Following a recommendation from helpdesk, all files should be compressed with 7zip.
* La Carte
  Every final YAGO theme shall be available as a separate download. 
  There is a Java utility "Carte" in cobverters2s that produces the list
  of final themes in HTML, complete with links and previews.
  This HTML document shall become part of the Web page.
* Code tools
  We should make available at least the postgres-script from converters2s/scripts
  The other tools are maybe not necessary any more...
* Browsers
  We have a cool SVG-based browser, which should be online.
  Edwin takes care of this.
  The flight planner should go online too before the BTW deadline,
  so that reviewers can try it out. Edwin is taking care of this.