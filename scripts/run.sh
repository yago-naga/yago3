#!/bin/sh
if [ `hostname` == "contact" ]; then
  echo We should not run this on contact!
  exit
fi
/local/java/jdk1.7.0/bin/java -Xmx44G -cp "basics2s/bin:javatools/bin:yago2s/bin" main.Caller yago.ini log &
disown -h %1
