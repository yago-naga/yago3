#!/bin/sh

# Call it like this: ./prun.sh configuration/my_configuration_file.ini

# Get the absolute dir of this script and navigate there
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

#if [ `hostname` != "d5blade09" ]; then
#  echo We should not run this on contact!
#  exit
#fi

echo Starting YAGO in parallel, output written to yago.log
#nohup /local/java/bin/java -Xmx44G -cp "../javatools/bin:../basics3/bin:bin" main.ParallelCaller yago.ini > yago.log &

export MAVEN_OPTS=-Xmx44G
mvn exec:java -Dexec.args="$1"

sleep 5s
tail -f yago.log
