#!/bin/bash

# Check if arguments are given
if [ $# -eq 0 ] 
then
	echo "Usage: $0 <wp_dumps_target_dir> <url>..."
	echo "  Parameters:"
	echo "    <wp_dumps_target_dir>  e.g. /GW/aida/work/wikipedia_dumps/"
	echo "    <download_url>         e.g. http://dumps.wikimedia.org/eswiki/20151202/"
	echo "                                eswiki-20151202-pages-articles.xml.bz2"
	exit 1
else
	args=( "$@" )
fi

# Fill in the variables
wp_dumps_dir="${args[0]}" 
wp_dump_download_links=("${args[@]:1}")

# Get the Wikipedia dumps, process one dump after the other.
for wp_dump_download_link in ${wp_dump_download_links[@]};
do
	echo "Processing $wp_dump_download_link"

	# Go to the base dir of the dumps
	mkdir -m 775 -p $wp_dumps_dir
	cd $wp_dumps_dir
	
	# Switch to the language folder
	language=${wp_dump_download_link:28:2}
	mkdir -m 775 -p $language
	cd $language
	
	# Create a folder for the date of the Wikipedia dump
	date=${wp_dump_download_link:35:8}

	if [ -d "$date" ]; then
		echo "$wp_dumps_dir/$language/$date already exists. Skipping."
	else
		mkdir -m 775 $date
		cd $date

		# Download the dump
		curl -O $wp_dump_download_link

		# Extract, then delete the archive
		filename=${wp_dump_download_link:44}
		bzip2 -d $filename
	fi		

done
