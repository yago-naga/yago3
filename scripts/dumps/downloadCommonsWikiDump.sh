#!/bin/bash

# Check if arguements are given and fill in variables
PARAMS=2
if [ $# -ne "$PARAMS" ]
then
    echo "Usage: $0 <dump_target_dir> <url>"
    echo "  Parameters:"
    echo "    <dump_targer_dir>  e.g. /GW/aida/work/data/"
    echo "    <url>              e.g. https://dumps.wikimedia.org/commonswiki/20170101/"
    echo "                            commonswiki-20170101-pages-articles.xml.bz2"
    exit 1
else
    args=( "$@" )
fi

cw_dump_dir="${args[0]}"
cw_download_url="${args[1]}"

echo "Processing $cw_download_url"

# Create targer directory
# Check if the target directory ends with "/", add it other wise
cw_dir="commonswiki"
i=$((${#cw_dump_dir}-1))
if [[ "/" == "${cw_dump_dir:$i:1}" ]]
then
    cw_dump_dir="${cw_dump_dir}$cw_dir"
else
    cw_dump_dir="$cw_dump_dir/$cw_dir"
fi
mkdir -m 775 -p $cw_dump_dir


# Go to the base directory
cd $cw_dump_dir

# Create and go to the inner directory
date=${cw_download_url:40:8}
mkdir -m 775 -p $date
cd $date

# Check if the file already exist
cw_file_name="commonswiki-${date}-pages-articles.xml"
if [ -e $cw_file_name ]
then
    echo "$cw_dump_dir/$date/$cw_file_name already exist."
else
    # Download the dump
    curl -O $cw_download_url

    # Extract and delete the archieve file
    bzip2 -d "$cw_file_name.bz2"
fi