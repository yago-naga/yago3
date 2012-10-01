#!/bin/sh
if [ `hostname` != "d5blade09" ]; then
  echo We should not run this on contact!
  exit
fi

/usr/lib/jvm/java-7-sun/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:yago2s/bin" main.ParallelCaller yago_test.ini > test.log &
sleep 5s
tail -f test.log
