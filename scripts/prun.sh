#!/bin/bash

# Call it like this: ./prun.sh configuration/my_configuration_file.ini

# Get the absolute dir of this script and navigate there
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

# Run the preflight checks and download all required resources
$DIR/dumps/downloadDumps.py -y "$DIR/../$1"

# Execute the ParallelCaller
export MAVEN_OPTS=-Xmx512G
mvn -U clean verify exec:java -Dexec.args="$DIR/../$1.adapted.ini"

# Remove the dynamically adapted configuration
rm "$DIR/../$1.adapted.ini"