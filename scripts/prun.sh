#!/bin/sh
if [ `hostname` != "d5blade05" ]; then
  echo We should not run this on contact!
  exit
fi
echo Starting YAGO in parallel, output written to yago.log
/local/java/jdk1.7.0/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:yago2s/bin" main.ParallelCaller yago.ini > yago.log &
disown -h %1
sleep 5s
tail -f yago.log
