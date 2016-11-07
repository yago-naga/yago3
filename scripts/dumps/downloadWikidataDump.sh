#!/bin/bash

# Check if arguements are given and fill in variables
MINPARAMS=2
if [ $# -lt "$MINPARAMS" ]
then
    echo "Usage: $0 <wd_dump_target_dir> <url>..."
    echo "  Parameters:"
    echo "    <wd_dumpd_target_dir>  e.g. /GW/aida/work/data/"
    echo "    <download_url>         e.g. http://tools.wmflabs.org/wikidata-exports/rdf/"
    echo "                                exports/20160801/wikidata-sitelinks.nt.gz"
    exit 1
else
    args=( "$@" )
fi

wd_dump_dir="${args[0]}" 
wd_dump_download_links=("${args[@]:1}")

# Create target directory
# Check if the target directory ends with "/", add it other wise
wikidata_dir="wikidata_dump"
i=$((${#wd_dump_dir}-1))
if [[ "/" == "${wd_dump_dir:$i:1}" ]]
then
    wd_dump_dir="${wd_dump_dir}$wikidata_dir"
else
    wd_dump_dir="$wd_dump_dir/$wikidata_dir"
fi
mkdir -m 775 -p $wd_dump_dir


for wd_dump_download_link in ${wd_dump_download_links[@]};
do
	# Go to the base directory	
	cd $wd_dump_dir

	# Create and go to the inner directory
	date=${wd_dump_download_link:54:8}
	mkdir -m 775 -p $date
	cd $date

	# Check if the file already exist
	if [[ $wd_dump_download_link == *"sitelinks"* ]]
	then
		if [ -e wikidata-sitelinks.nt ]
		then
		    echo "$wd_dump_dir/$date/wikidata-sitelinks.nt already exist."
		else
		    # Download the dump
		    curl -O $wd_dump_download_link

		    # Extract and delete the archieve file
		    gunzip  wikidata-sitelinks.nt.gz   
		fi
	elif [[ $wd_dump_download_link == *"statements"* ]]
	then
		if [ -e wikidata-statements.nt ]
		then
		    echo "$wd_dump_dir/$date/wikidata-statements.nt already exist."
		else
		    # Download the dump
		    curl -O $wd_dump_download_link

		    # Extract and delete the archieve file
		    gunzip  wikidata-statements.nt.gz   
		fi
	else
		echo "Download link is not correct: " + $wd_dump_download_link
	fi

done
