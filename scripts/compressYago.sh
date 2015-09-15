#!/bin/sh
yagoFolder="/san/ekuzey/publishYagoDownloads/yago3"
webpage="/san/ekuzey/publishYagoDownloads/www"
echo $yagoFolder

echo Creating YAGO La Carte archives.
echo This process runs in the background and logs to publishYago.log.
echo Pressing CTRL+C will not stop the process.

for file in $yagoFolder/yago*; do 7za a -t7z $webpage/${file/\/*\//}.7z $file -mmt & done
sleep 20s

echo Creating one piece yago downloads.
7za a -t7z yago3_entire_tsv.7z $yagoFolder/yago*.tsv -mmt & done

7za a -t7z yago3_entire_ttl.7z $yagoFolder/yago*.ttl -mmt & done
