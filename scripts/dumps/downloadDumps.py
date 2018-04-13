#!/usr/bin/env python
# encoding: utf-8

"""
Downloads Wikipedia, Wikidata and commonswiki dumps for the specified languages unless they are explicitly specified in the YAGO configuration file via the properties named "wikipedias", "wikidata" or "commons_wiki". For all dumps, the most recent version is downloaded unless an explicit date is set.

Usage:
  downloadDumps.py -y YAGO_CONFIGURATION_FILE [(--date=DATE ...)] [--wikidata-date=WIKIDATA_DATE] [--commonswiki-date=COMMONSWIKI_DATE] [-s START_DATE]

Options:
  -y YAGO_CONFIGURATION_FILE --yago-configuration-file=YAGO_CONFIGURATION_FILE      the YAGO3 ini file that holds the configuration to be used
  --date=DATE                                         Date of the Wikipedia dump
  --wikidata-date=WIKIDATA_DATE                       Date of the Wikidata dump
  --commonswiki-date=COMMONSWIKI_DATE                 Date of the CommonsWiki dump
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

# Constants
DOWNLOAD_WIKIPEDIA_DUMP_SCRIPT = 'downloadWikipediaDump.sh'
DOWNLOAD_WIKIDATA_DUMP_SCRIPT =  'downloadWikidataDump.sh'
DOWNLOAD_COMMONSWIKI_DUMP_SCRIPT =  'downloadCommonsWikiDump.sh'
DOWNLOAD_GEONAMES_DUMP_SCRIPT =  'downloadGeonamesDump.sh'
WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS = 365

YAGO3_ADAPTED_CONFIGURATION_EXTENSION = '.adapted.ini'

YAGO3_DUMPSFOLDER_PROPERTY = 'dumpsFolder'
YAGO3_LANGUAGES_PROPERTY = 'languages'
YAGO3_WIKIPEDIAS_PROPERTY = 'wikipedias'
YAGO3_WIKIDATA_PROPERTY = 'wikidata'
YAGO3_COMMONSWIKI_PROPERTY = "commons_wiki"
YAGO3_GEONAMES_PROPERTY = "geonames"

WIKIPEDIA_DUMPS_PAGE = 'https://dumps.wikimedia.org/'
WIKIDATA_DUMPS_PAGE = 'https://dumps.wikimedia.org/wikidatawiki/entities/'
COMMONSWIKI_DUMP_PAGE = 'https://dumps.wikimedia.org/commonswiki/'
WIKIDATA_DIR = 'wikidatawiki'
COMMONSWIKI_DIR = 'commonswiki'
GEONAMES_DIR = 'geonames'

# Initialize variables
dumpsFolder = None
languages = None
wikipedias = None
wikipediaIds = None
wikidata = None
wikidataUrl = None
commons_wiki = None
commonsWikiUrl = None


class Usage(Exception):
  def __init__(self, msg):
    self.msg = msg

def execute(cmd, customEnv=None):
  process = subprocess.Popen(cmd, stdout=PIPE, stderr=STDOUT, universal_newlines=True, env=customEnv)

  for line in iter(process.stdout.readline, ""):
    print(line)

  process.stdout.close()
  return_code = process.wait()

  if return_code:
    raise subprocess.CalledProcessError(return_code, cmd)

def main(argv=None):
  global dumpsFolder, languages, wikipedias, wikidata, wikipediaIds

  print("Loading YAGO configuration...")
  loadYagoConfiguration()

  if wikipedias == None:
    print("Downloading Wikipedia dump(s)...")
    wikipedias = downloadWikipediaDumps(languages)
  else:
    print("Wikipedia dump(s) already present.")
    wikipediaIds = getWikipediaIdsFromFile(wikipedias)

  if (wikidata == None):
    print("Downloading Wikidata dump(s)...")
    downloadWikidataDumps()
  else:
    print("Wikidata dump(s) already present.")

  if commons_wiki == None:
    print("Downloading CommonsWiki dump...")
    downloadCommonsWikiDump()
  else:
    print("CommonsWiki dump already present.")

  downloadGeonames()

  print("Adapting the YAGO3 configuration...")
  adaptYagoConfiguration()

  print("Wikipedia, Wikidata and Commonswiki dumps are ready.")


"""
Loads the YAGO configuration file.
"""
def loadYagoConfiguration():
  global dumpsFolder, languages, wikipedias, wikidata, commons_wiki, geonames

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
    elif re.match('^' + YAGO3_COMMONSWIKI_PROPERTY + '\s*=', line):
      commons_wiki = re.sub(r'\s', '', line).split("=")[1]
    elif re.match('^' + YAGO3_WIKIDATA_PROPERTY + '\s*=', line):
      wikidata = re.sub(r'\s', '', line).split("=")[1]
    elif re.match('^' + YAGO3_GEONAMES_PROPERTY + '\s*=', line):
      geonames = re.sub(r'\s', '', line).split("=")[1]


  if languages == None:
    print("ERROR: 'languages' is a mandatory property and must be set in the configuration file.")
    sys.exit(1)


  if (wikipedias == None or wikidata == None or commons_wiki == None) and dumpsFolder == None:
    print("ERROR: Some resources require downloading dumps before YAGO can be run. You must set the 'dumpsFolder' property in the configuration file.")
    sys.exit(1)


"""
Invokes the external shell script for downloading and extracting the Wikipedia dumps.
"""
def downloadWikipediaDumps(languages):
  global dumpsFolder
  global wikipediaIds

  # Determine the most recent Wikipedia dump versions.
  urls = getWikipediaDumpUrls(languages)

  execute(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_WIKIPEDIA_DUMP_SCRIPT), dumpsFolder, ' '.join(urls)])


  wikipediaIds = getWikipediaIds(urls)

  return getWikipedias(urls)


"""
Duplicates the YAGO3 template ini file and adapts the properties as necessary
"""
def adaptYagoConfiguration():
  global wikipedias
  global wikipediaIds
  wikipediasDone = False
  wikidataDone = False
  commonsWikiDone = False
  geonamesDone = False

  yagoAdaptedConfigurationFile = os.path.join(
    os.path.dirname(yagoConfigurationFile), os.path.basename(yagoConfigurationFile) + YAGO3_ADAPTED_CONFIGURATION_EXTENSION)

  shutil.copy(yagoConfigurationFile, yagoAdaptedConfigurationFile)

  for line in fileinput.input(yagoAdaptedConfigurationFile, inplace=1):
    if re.match('^' + YAGO3_WIKIPEDIAS_PROPERTY + '\s*=', line):
      wikipediasDone = True
    elif re.match('^' + YAGO3_WIKIDATA_PROPERTY + '\s*=', line):
      wikidataDone = True
    elif re.match('^' + YAGO3_COMMONSWIKI_PROPERTY + '\s*=', line):
      commonsWikiDone = True
    elif re.match('^' + YAGO3_GEONAMES_PROPERTY + '\s*=', line):
      geonamesDone = True

    # Write the (possibly modified) line back to the configuration file
    sys.stdout.write(line)

  # If the values couldn't be replaced because the property wasn't in the configuration yet, add it.
  with open(yagoAdaptedConfigurationFile, "a") as configFile:
    # Make sure to start a new line first
    configFile.write('\n')

    if wikipediasDone == False:
      configFile.write(YAGO3_WIKIPEDIAS_PROPERTY + ' = ' + ','.join(wikipedias) + '\n')
    if wikidataDone == False:
      configFile.write(YAGO3_WIKIDATA_PROPERTY + ' = ' + getWikidata() + '\n')
    if commonsWikiDone == False:
      configFile.write(YAGO3_COMMONSWIKI_PROPERTY + ' = ' + getCommonsWiki() + '\n')
    if geonamesDone == False:
      configFile.write(YAGO3_GEONAMES_PROPERTY + ' = ' + getGeonames() + '\n')


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
Convenience method for getting languageId + date identifiers from Wikipedia dump file paths.
"""
def getWikipediaIdsFromFile(files):
  wpIds = []
  for file in files:
    wpIds.append(getLanguageFromFile(file) + getFormattedDateFromFile(file))
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
Convenience method for getting the ISO iso 639-1 language code out of a Wikipedia dump file path.
"""
def getLanguageFromFile(filePath):
  return filePath[len(filePath)-34:len(filePath)-32]


"""
Convenience method for getting the date string out of a Wikipedia dump file path.
"""
def getFormattedDateFromFile(filePath):
  return filePath[len(filePath)-27:len(filePath)-19]

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
        print("Latest Wikipedia dump for " + language + ": " + formattedDumpDate)
        urls.append(url)
        break
      else:
        if len(dates) > i and dates[i]:
          url = url + '/' + language + 'wiki-' + formattedDumpDate + '-pages-articles.xml.bz2'

          if os.path.isfile(getWikipedias([url])[0]):
            urls.append(url)
            break
          else:
            print("ERROR: No Wikipedia dump found (neither remotely nor in local cache) for language " + language + " and date " + formattedDumpDate + ".")
            sys.exit(1)
        elif (startDate - dumpDate).days <= WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS:
          dumpDate -= timedelta(days=1)
        else:
          print("ERROR: No Wikipedia dump found (neither remotely nor in local cache) for language " + language + " (oldest dump date tried was " + formattedDumpDate + ").")
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
def getWikidataUrl():
  resultUrl = None

  # Use the given date if it is available.
  if wikidataDate:
    dumpDate = datetime.strptime(wikidataDate, "%Y%m%d")
    formattedDumpDate = dumpDate.strftime("%Y%m%d")
    url= WIKIDATA_DUMPS_PAGE + formattedDumpDate + '/wikidata-' + formattedDumpDate + '-all-BETA.ttl.bz2'
    r = requests.head(url)

    if (r.status_code == requests.codes.ok):
      print("Wikidata dump is available for the given date: " + formattedDumpDate)
      resultUrl = url
    elif os.path.isfile(os.path.join(dumpsFolder, WIKIDATA_DIR, formattedDumpDate, 'wikidata-' + formattedDumpDate + '-all-BETA.ttl')):
      print("Wikidata dump exist: " + formattedDumpDate)
      resultUrl = url
    else:
      print("ERROR: No Wikidata dump file found (neither remotely nor in local cache) for date: " + formattedDumpDate)
      sys.exit(1)

  else:
    dumpDate = startDate
    while True:
      formattedDumpDate = dumpDate.strftime("%Y%m%d")
      url= WIKIDATA_DUMPS_PAGE + formattedDumpDate + '/wikidata-' + formattedDumpDate + '-all-BETA.ttl.bz2'
      r = requests.head(url)

      if (r.status_code == requests.codes.ok):
        print("Latest Wikidata dump: " + formattedDumpDate)
        resultUrl = url
        break
      elif os.path.isfile(os.path.join(dumpsFolder, WIKIDATA_DIR, formattedDumpDate, 'wikidata-' + formattedDumpDate + '-all-BETA.ttl')):
        print("Wikidata dump exist: " + formattedDumpDate)
        resultUrl = url
        break
      elif (startDate - dumpDate).days <= WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS:
        dumpDate -= timedelta(days = 1)
      else:
        print("ERROR: No Wikidata dump file found (neither remotely nor in local cache), oldest dump date tried was: " + formattedDumpDate)
        sys.exit(1)

  return resultUrl


"""
Invokes the external shell script for downloading and extracting the Wikidata dump
"""
def downloadWikidataDumps():
  global wikidataUrl

  # Determine the most recent Wikidata dump version.
  wikidataUrl = getWikidataUrl()

  execute(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_WIKIDATA_DUMP_SCRIPT),
    dumpsFolder, wikidataUrl])


"""
Invokes the external shell script for downloading and extracting the Commonswiki dump
"""
def downloadCommonsWikiDump():
  global commonsWikiUrl

  # Determine the most recent CommonsWiki dump version
  commonsWikiUrl = getCommonsWikiUrl()

  subprocess.call(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_COMMONSWIKI_DUMP_SCRIPT),
    dumpsFolder, commonsWikiUrl])


"""
Invokes the external shell script for downloading and extracting Geonames dump
"""
def downloadGeonames():
  subprocess.call(
    [os.path.join(os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))), DOWNLOAD_GEONAMES_DUMP_SCRIPT),
    dumpsFolder,
    startDate.strftime("%Y%m%d")])


"""
Gets the url that point to the most recent Commonswiki dump version
"""
def getCommonsWikiUrl():
  resultUrl = None

  # Use the given date if it is available.
  if commonswikiDate:
    dumpDate = datetime.strptime(commonswikiDate, "%Y%m%d")
    formattedDumpDate = dumpDate.strftime("%Y%m%d")
    url= COMMONSWIKI_DUMP_PAGE + formattedDumpDate + '/commonswiki-' + formattedDumpDate + '-pages-articles.xml.bz2'
    r = requests.head(url)

    if (r.status_code == requests.codes.ok):
      print("Commonswiki dump is available for the given date: " + formattedDumpDate)
      resultUrl = url
    elif os.path.isfile(os.path.join(dumpsFolder, COMMONSWIKI_DIR, formattedDumpDate, 'commonswiki-' + formattedDumpDate + '-pages-articles.xml')):
      print("Commonswiki dump exist: " + formattedDumpDate)
      resultUrl = url
    else:
      print("ERROR: No Commonswiki dump file found (neither remotely nor in local cache) for date: " + formattedDumpDate)
      sys.exit(1)

  else:
    dumpDate = startDate
    while True:
      formattedDumpDate = dumpDate.strftime("%Y%m%d")
      url= COMMONSWIKI_DUMP_PAGE + formattedDumpDate + '/commonswiki-' + formattedDumpDate + '-pages-articles.xml.bz2'
      r = requests.head(url)

      if (r.status_code == requests.codes.ok):
        print("Latest Commonswiki dump: " + formattedDumpDate)
        resultUrl = url
        break
      elif os.path.isfile(os.path.join(dumpsFolder, COMMONSWIKI_DIR, formattedDumpDate, 'commonswiki-' + formattedDumpDate + '-pages-articles.xml')):
        print("Commonswiki dump exist: " + formattedDumpDate)
        resultUrl = url
        break
      elif (startDate - dumpDate).days <= WIKIPEDIA_DUMP_MAX_AGE_IN_DAYS:
        dumpDate -= timedelta(days = 1)
      else:
        print("ERROR: No Commonswiki dump file found (neither remotely nor in local cache), oldest dump date tried was: " + formattedDumpDate)
        sys.exit(1)

  return resultUrl


"""
Gets the path to wikidata dump
"""
def getWikidata():
  global wikidataUrls

  date = wikidataUrl[50:58]
  return os.path.join(dumpsFolder, WIKIDATA_DIR, date, 'wikidata-' + date + '-all-BETA.ttl' )


"""
Gets the path to wikidata dump
"""
def getCommonsWiki():
  global commonsWikiUrl

  date = commonsWikiUrl[40:48]
  return os.path.join(dumpsFolder, COMMONSWIKI_DIR, date, 'commonswiki-' + date + '-pages-articles.xml')

"""
Gets the path to wikidata dump
"""
def getGeonames():
  # There will always be a folder which is named according to startDate. Either it's a symlink or a real dir,
  # but this doesn't matter here.
  return os.path.join(dumpsFolder, GEONAMES_DIR, startDate.strftime("%Y-%m-%d"))


if __name__ == "__main__":
  # parse options
  options = docopt(__doc__)

  dates = options['--date']
  wikidataDate = options['--wikidata-date']
  commonswikiDate = options['--commonswiki-date']
  yagoConfigurationFile = options['--yago-configuration-file']

  # Read optional arguments with dynamic defaults
  if options['--start-date']:
    startDate = datetime.strptime(options['--start-date'], '%Y%m%d')
  else:
    startDate = datetime.today()

  sys.exit(main())
