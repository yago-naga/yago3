#!/bin/bash

# Check if arguements are given and fill in variables
PARAMS=2
if [ $# -ne "$PARAMS" ]
then
    echo "Usage: $0 <dump_target_dir> <url>"
    echo "  Parameters:"
    echo "    <dump_target_dir>  e.g. /GW/aida/work/data/"
    echo "    <url>              e.g. https://dumps.wikimedia.org/wikidatawiki/entities/20170227/"
    echo "                            wikidata-20170227-all-BETA.ttl.bz2"
    exit 1
else
    args=( "$@" )
fi

wd_dump_dir="${args[0]}"
wd_download_url="${args[1]}"

echo "Processing $wd_download_url"

# Create target directory
# Check if the target directory ends with "/", add it other wise
wd_dir="wikidatawiki"
i=$((${#wd_dump_dir}-1))
if [[ "/" == "${wd_dump_dir:$i:1}" ]]
then
    wd_dump_dir="${wd_dump_dir}$wd_dir"
else
    wd_dump_dir="$wd_dump_dir/$wd_dir"
fi
mkdir -m 775 -p $wd_dump_dir


# Go to the base directory
cd $wd_dump_dir

# Create and go to the inner directory
date=${wd_download_url:50:8}
mkdir -m 775 -p $date
cd $date

# Check if the file already exist
wd_file_name="wikidata-${date}-all-BETA.ttl"
if [ -e $wd_file_name ]
then
    echo "$wd_dump_dir/$date/$wd_file_name already exist."
else
    # Download the dump
    curl -O $wd_download_url

    # Extract and delete the archieve file
    bzip2 -d "$wd_file_name.bz2"
fi
