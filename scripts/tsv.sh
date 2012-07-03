#!/bin/sh
if [ `hostname` != "d5blade09" ]; then
  echo We should not run this on contact!
  exit
fi
echo Converting YAGO to TSV.
echo This process runs in the background and logs to tsv.log.
echo Pressing CTRL+C will not stop the process.
nohup /local/java/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:converters2s/bin" converters.TsvConverter yago.ini > tsv.log &
