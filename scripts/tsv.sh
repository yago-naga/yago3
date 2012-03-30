#!/bin/sh
if [ `hostname` != "d5blade05" ]; then
  echo We should not run this on contact!
  exit
fi
echo Converting YAGO to TSV
/local/java/jdk1.7.0/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:converters2s/bin" converters.TsvConverter yago.ini > tsv.log &
disown -h %1
sleep 5s
tail -f tsv.log
echo yago2itnyago | psql -a -d yago2s -h postgres0 -U yago -f converters2s/scripts/postgres.sql
