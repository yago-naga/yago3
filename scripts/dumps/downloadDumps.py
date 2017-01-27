#!/usr/bin/env python
# encoding: utf-8

"""
Downloads Wikipedia and Wikidata dumps for the specified languages unless they are explicitly specified in the YAGO configuration file via the properties named "wikipedias", "wikidata_sitelinks", or "wikidata_statements". For all dumps, the most recent version is downloaded unless an explicit date is set. 

Usage:
  downloadDumps.py -y YAGO_CONFIGURATION_FILE [(--date=DATE ...)] [--wikidata-date=WIKIDATA_DATE] [-s START_DATE]
  
Options:
  -d TARGET_DIR --target-dir=TARGET_DIR               directory to store the Wikipedia dumps
  -y YAGO_CONFIGURATION_FILE --yago-configuration-file=YAGO_CONFIGURATION_FILE      the YAGO3 ini file that holds the configuration to be used
  --date=DATE                                         Date of the Wikipedia dump
  --wikidata-date=WIKIDATA_DATE                       Date of the Wikidata dump
  -s START_DATE --start-date=START_DATE               Date from where the search for dumps starts backwards in time (default: today())
"""
from datetime import datetime
import getopt
import os
import re
import sys
import time
import requests
import shutil
import fileinput
import subprocess
from subprocess import PIPE, STDOUT
import inspect
from datetime import date, timedelta
from docopt import docopt
from BeautifulSoup import BeautifulSoup

# Constants
DOWNLOAD_WIKIPEDIA_DUMP_SCRIPT = 'downloadWikipediaDump.sh'
DOWNLOAD_WIKIDATA_DUMP_SCRIPT =  'downloadWikidataDump.sh'
WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS = 365

YAGO3_ADAPTED_CONFIGURATION_EXTENSION = '.adapted.ini'

YAGO3_LANGUAGES_PROPERTY = 'languages'
YAGO3_WIKIPEDIAS_PROPERTY = 'wikipedias'
YAGO3_DUMPSFOLDER_PROPERTY = 'dumpsFolder'
YAGO3_WIKIDATA_SITELINKS_PROPERTY = 'wikidata_sitelinks'
YAGO3_WIKIDATA_STATEMENTS_PROPERTY = 'wikidata_statements'

WIKIPEDIA_DUMPS_PAGE = 'https://dumps.wikimedia.org/'
WIKIDATA_DUMPS_PAGE = 'http://tools.wmflabs.org/wikidata-exports/rdf/'
WIKIDATA_DIR = 'wikidata_dump'
WIKIDATA_SITELINKS_FILE = 'wikidata-sitelinks.nt'
WIKIDATA_STATEMENTS_FILE = 'wikidata-statements.nt'

# Initialize variables
dumpsFolder = None
languages = None
wikipedias = None
wikidata_sitelinks = None
wikidata_statements = None
wikidataUrls = None


class Usage(Exception):
  def __init__(self, msg):
    self.msg = msg

def execute(cmd):
  proc = subprocess.Popen(cmd, stdout=PIPE, stderr=STDOUT, universal_newlines=True)
  print proc.communicate()[0]

def main(argv=None):
  global dumpsFolder, languages, wikipedias, wikidata_sitelinks, wikidata_statements
  
  print "Loading YAGO configuration..."
  loadYagoConfiguration()
  
  if wikipedias == None:
    print "Downloading Wikipedia dump(s)..."
    wikipedias = downloadWikipediaDumps(languages)
  else:
    print "Wikipedia dump(s) already present."
 
  if wikidata_sitelinks == None:
    print "Downloading Wikidata dump(s)..."
    downloadWikidataDumps()
  else:
    print "Wikidata dump(s) already present."
  
  print "Adapting the YAGO3 configuration..."
  adaptYagoConfiguration()
    
  print "Wikipedia and Wikidata dumps are ready."

  
"""
Loads the YAGO configuration file.
"""  
def loadYagoConfiguration():  
  global dumpsFolder, languages, wikipedias, wikidata_sitelinks, wikidata_statements
  
  for line in fileinput.input(yagoConfigurationFile):
    if re.match('^' + YAGO3_DUMPSFOLDER_PROPERTY + '\s*=', line):
      dumpsFolder = re.sub(r'\s', '', line).split("=")[1]
      
      # Make sure the folder exists
      if not os.path.exists(dumpsFolder):
        os.makedirs(dumpsFolder)  
            
    elif re.match('^' + YAGO3_LANGUAGES_PROPERTY + '\s*=', line):
      languages = re.sub(r'\s', '', line).split("=")[1].split(",")
    elif re.match('^' + YAGO3_WIKIPEDIAS_PROPERTY + '\s*=', line):
      wikipedias = re.sub(r'\s', '', line).split("=")[1].split(",")
    elif re.match('^' + YAGO3_WIKIDATA_SITELINKS_PROPERTY + '\s*=', line):
      wikidata_sitelinks = re.sub(r'\s', '', line).split("=")[1]
    elif re.match('^' + YAGO3_WIKIDATA_STATEMENTS_PROPERTY + '\s*=', line):  
      wikidata_statements = re.sub(r'\s', '', line).split("=")[1]
     
  if languages == None: 
    print "ERROR: 'languages' is a mandatory property and must be set in the configuration file."
    sys.exit(1)
    
  if (wikidata_sitelinks == None and wikidata_statements != None) or (wikidata_sitelinks != None and wikidata_statements == None):
    print "ERROR: 'wikidata_sitelinks' and 'wikidata_statements' must be set or unset as a pair."
    sys.exit(1)
    
  if (wikipedias == None or wikidata_sitelinks == None) and dumpsFolder == None: 
    print "ERROR: Some resources require downloading dumps before YAGO can be run. You must set the 'dumpsFolder' property in the configuration file."
    sys.exit(1)
  
  
"""
Invokes the external shell script for downloading and extracting the Wikipedia dumps.
"""
def downloadWikipediaDumps(languages):
  global dumpsFolder
  
  # Determine the most recent Wikipedia dump versions.
  urls = getWikipediaDumpUrls(languages)
  
  execute(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_WIKIPEDIA_DUMP_SCRIPT), dumpsFolder, ' '.join(urls)])
    
  return getWikipedias(urls)


"""
Duplicates the YAGO3 template ini file and adapts the properties as necessary
"""
def adaptYagoConfiguration():  
  global wikipedias
  wikipediasDone = False
  wikidataSitelinksDone = False
  wikidataStatementsDone = False
  
  yagoAdaptedConfigurationFile = os.path.join(
    os.path.dirname(yagoConfigurationFile), os.path.basename(yagoConfigurationFile) + YAGO3_ADAPTED_CONFIGURATION_EXTENSION)
  
  shutil.copy(yagoConfigurationFile, yagoAdaptedConfigurationFile)
  
  for line in fileinput.input(yagoAdaptedConfigurationFile, inplace=1):
    if re.match('^' + YAGO3_WIKIPEDIAS_PROPERTY + '\s*=', line):
      wikipediasDone = True
    elif re.match('^' + YAGO3_WIKIDATA_SITELINKS_PROPERTY + '\s*=', line):
      wikidataSitelinksDone = True
    elif re.match('^' + YAGO3_WIKIDATA_STATEMENTS_PROPERTY + '\s*=', line):
      wikidataStatementsDone = True
    
    # Write the (possibly modified) line back to the configuration file
    sys.stdout.write(line)
    
  # If the values couldn't be replaced because the property wasn't in the configuration yet, add it.
  with open(yagoAdaptedConfigurationFile, "a") as configFile:
    # Make sure to start a new line first
    configFile.write('\n')
    
    if wikipediasDone == False:
      configFile.write(YAGO3_WIKIPEDIAS_PROPERTY + ' = ' + ','.join(wikipedias) + '\n')  
    if wikidataSitelinksDone == False:
      configFile.write(YAGO3_WIKIDATA_SITELINKS_PROPERTY + ' = ' + getWikidata(WIKIDATA_SITELINKS_FILE) + '\n')  
    if wikidataStatementsDone == False:
      configFile.write(YAGO3_WIKIDATA_STATEMENTS_PROPERTY + ' = ' + getWikidata(WIKIDATA_STATEMENTS_FILE) + '\n')


"""
Converts from ISO 639-1 into ISO 639-2 format. For creating the mapping, we referred to this website:
https://www.loc.gov/standards/iso639-2/php/code_list.php
"""
def getThreeLetterLanguageCode(twoLetterCode):
  codeTable = {
    'ar': "ara",
    'de': "deu",
    'en': "eng",
    'fr': "fra",
    'it': "ita",
    'jp': "jpn",
    'es': "spa",
    'pt': "por",
    'ru': "rus",
    'zh': "zho"
  }
  return codeTable.get(twoLetterCode, "xx")


"""
Convenience method for getting languageId + date identifiers from Wikipedia dump URLs.
"""
def getWikipediaIds(urls):
  wpIds = []
  for url in urls:
    wpIds.append(getLanguage(url) + getFormattedDate(url))
  return wpIds


"""
Constructs the database ID from a set of Wikipedia URLs.
"""
def getDatabaseId(urls):
  languages = []
  dates = []
  for url in urls:
    languages.append(getLanguage(url))
    dates.append(getFormattedDate(url))

  return max(dates) + '_' + '_'.join(sorted(languages))


"""
Convenience method for getting the ISO iso 639-1 language code out of a Wikipedia dump URL.
"""
def getLanguage(url):
  return url[28:30]


"""
Convenience method for getting the date string out of a Wikipedia dump URL.
"""  
def getFormattedDate(url):
  return url[35:43]


"""
Convenience method for getting the ISO iso 639-1 language code out of a Wikipedia dump URL.
"""
def getExtractedFilename(url):
  return url[44:-4]
  
  
"""
Gets a list of URLs that point to the most recent Wikipedia dump versions for the specified list of languages
"""  
def getWikipediaDumpUrls(languages):
  urls = []
  
  for i in range(0, len(languages)):
    language = languages[i]
    
    # If a fixed data is set, use exactly this one.
    if len(dates) > i and dates[i]:
      dumpDate = datetime.strptime(dates[i], '%Y%m%d')
    else:
      dumpDate = startDate
    
    while True:    
      formattedDumpDate = dumpDate.strftime("%Y%m%d")
      url = WIKIPEDIA_DUMPS_PAGE + language + 'wiki/' + formattedDumpDate + '/' + language + 'wiki-' + formattedDumpDate + '-pages-articles.xml.bz2'
      
      r = requests.head(url)
      
      if (r.status_code == 200):
        print "Latest Wikipedia dump for " + language + ": " + formattedDumpDate
        urls.append(url)
        break
      else:
        if len(dates) > i and dates[i]:
          url = url + '/' + language + 'wiki-' + formattedDumpDate + '-pages-articles.xml.bz2'

          if os.path.isfile(getWikipedias([url])[0]):          
            urls.append(url)
            break
          else:
            print "ERROR: No Wikipedia dump found (neither remotely nor in local cache) for language " + language + " and date " + formattedDumpDate + "."
            sys.exit(1)
        elif (startDate - dumpDate).days <= WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS:
          dumpDate -= timedelta(days=1)
        else:
          print "ERROR: No Wikipedia dump found (neither remotely nor in local cache) for language " + language + " (oldest dump date tried was " + formattedDumpDate + ")."
          sys.exit(1)
          
  return urls


"""
Gets a list of Wikipedia dump filenames (*-pages-articles.xml) in the specified YAGO target folder
"""
def getWikipedias(urls):
  wps = []
  for url in urls:
    wps.append(os.path.join(dumpsFolder, getLanguage(url), getFormattedDate(url), getExtractedFilename(url)))
  return wps
  
  
"""
Get the URL to the most recent wikidata dump
"""
def getWikidataUrls(): 
  urls = {}
    
  for dumpFile in [WIKIDATA_SITELINKS_FILE, WIKIDATA_STATEMENTS_FILE]:
    # Use the given date if it is available
    if wikidataDate:
      dumpDate = datetime.strptime(wikidataDate, "%Y%m%d")
      formattedDumpDate = dumpDate.strftime("%Y%m%d")
      url= WIKIDATA_DUMPS_PAGE + 'exports/' + formattedDumpDate + '/' + dumpFile + '.gz'
      r = requests.head(url)

      if r.status_code == requests.codes.ok and checkStatus(formattedDumpDate):
        print dumpFile[:-3] + " dump is available: " + formattedDumpDate
        urls[dumpFile] = url
      elif os.path.isfile(os.path.join(dumpsFolder, WIKIDATA_DIR, formattedDumpDate, dumpFile)):
        print dumpFile[:-3] + " dump exist: " + formattedDumpDate
        urls[dumpFile] = url
      else:
        print "ERROR: No " + dumpFile[:-3] + " dump file found (neither remotely nor in local cache) for date: " + wikidataDate
        sys.exit(1)
  
    else:
      dumpDate = startDate
      while True:
        formattedDumpDate = dumpDate.strftime("%Y%m%d")
        url = WIKIDATA_DUMPS_PAGE + 'exports/' + formattedDumpDate + '/' + dumpFile + '.gz'
        r = requests.head(url)

        if r.status_code == requests.codes.ok and checkStatus(formattedDumpDate):
          print "Latest " + dumpFile[:-3] + " dump: " + formattedDumpDate
          urls[dumpFile] = url
          break
        elif os.path.isfile(os.path.join(dumpsFolder, WIKIDATA_DIR, formattedDumpDate, dumpFile)):
          print "Wikidata dump exist: " + formattedDumpDate
          urls[dumpFile] = url
          break
        elif (startDate - dumpDate).days <= WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS:
          dumpDate -= timedelta(days = 1)
        else:
          print "ERROR: No " + dumpFile[:-3] + " dump file found (neither remotely nor in local cache), oldest dump date tried was: " + formattedDumpDate
          sys.exit(1)
    
  return urls


"""
Checks if dumps for given date are available or not yet(in progress)
"""        
def checkStatus(formattedDate):
  r = requests.get(WIKIDATA_DUMPS_PAGE + 'exports.html')
  html_parsed = BeautifulSoup(r.content)
  table = html_parsed.find("table", attrs ={"class":"exports"})
  tags = table.findAll("a", href=True)
  href = "exports/" + formattedDate + "/dump_download.html"
  for tag in tags:
    if tag['href'] == href:
      status = tag.findNext("a")
      if status.get('class') == 'green-text' and status.contents[0] == 'available':
        return True
      elif status.get('class') == 'yellow-text' and status.contents[0] == 'in progress':
        return False
  return False


"""
Invokes the external shell script for downloading and extracting the Wikidata dump
"""
def downloadWikidataDumps():
  global wikidataUrls
  
  # Determine the most recent Wikidata dump versions.
  wikidataUrls = getWikidataUrls()
  
  execute(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_WIKIDATA_DUMP_SCRIPT),
    dumpsFolder, ' '.join(wikidataUrls.values())])


"""
Gets the path to wikidata dump
"""  
def getWikidata(dumpFile):
  global wikidataUrls
  
  date = wikidataUrls[dumpFile][54:62]
  return os.path.join(dumpsFolder, WIKIDATA_DIR, date, dumpFile)
  
  
if __name__ == "__main__":
  # parse options
  options = docopt(__doc__)
  
  dates = options['--date']
  wikidataDate = options['--wikidata-date']
  yagoConfigurationFile = options['--yago-configuration-file']
  
  # Read optional arguments with dynamic defaults
  if options['--start-date']:
    startDate = datetime.strptime(options['--start-date'], '%Y%m%d')
  else:
    startDate = datetime.today()
  
  sys.exit(main())
