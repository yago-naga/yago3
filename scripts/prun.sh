#!/bin/sh
if [ `hostname` != "d5blade09" ]; then
  echo We should not run this on contact!
  exit
fi
echo Starting YAGO in parallel, output written to yago.log
nohup /local/java/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:yago2s/bin" main.ParallelCaller yago.ini > yago.log &

sleep 5s
tail -f yago.log
