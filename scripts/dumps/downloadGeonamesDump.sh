#!/bin/bash

# Check if arguements are given and fill in variables
PARAMS=2
if [ $# -ne "$PARAMS" ]
then
    echo "Usage: $0 <dump_target_dir> <start_date>"
    echo "  Parameters:"
    echo "    <dump_target_dir>  e.g. /GW/aida/work/data/"
    echo "    <start_date>       e.g. 20171003"
    exit 1
else
    args=( "$@" )
fi

gn_dump_dir="${args[0]}"
startDate="${args[1]}"

# Create target directory
# Check if the target directory ends with "/", add it other wise
gn_dir="geonames"
i=$((${#gn_dump_dir}-1))
if [[ "/" == "${gn_dump_dir:$i:1}" ]]
then
    gn_dump_dir="${gn_dump_dir}$gn_dir"
else
    gn_dump_dir="$gn_dump_dir/$gn_dir"
fi
mkdir -m 775 -p "$gn_dump_dir"


# Go to the base directory
cd "$gn_dump_dir"

# Create the inner directory and name it according to the date
date=$(date "+%Y-%m-%d")
mkdir -m 775 -p $date

# But make sure there's a symlink in case startDate is different from date.
if [[ "$date" != "$startDate" ]]
then
    ln -s $date ${startDate:0:4}-${startDate:4:2}-${startDate:6:2}
fi

# Go to inner directory
cd $date

echo "Downloading geonames dumps to $gn_dump_dir/$date/"

# Check if the file already exist
files="countryInfo.txt hierarchy.zip alternateNames.zip userTags.zip featureCodes_en.txt allCountries.zip"

for file in $files; do
	filename=${file%.*} # remove extension
	if [ -e "$filename.zip" ] || [ -e "$filename.txt" ]
	then
		echo "  $filename.zip or $filename.txt already exist"
	else
		# Download the dump
		curl -O http://download.geonames.org/export/dump/$file

		# Extract zip
		echo "${file##*.}"
		if [ "${file##*.}" == "zip" ]; then
			echo "unzipping"
			unzip "$file"
			rm "$file"
			touch "$filename.txt"
		fi
	fi
done
