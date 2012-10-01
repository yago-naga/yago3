#!/bin/sh
if [ `hostname` != "d5blade09" ]; then
  echo We should not run this on contact!
  exit
fi
echo Creating flight database
echo This process runs in the background and logs to db.log.
echo Pressing CTRL+C will not stop the process.
echo *:*:yago2s:*:yago2itnyago > ~/.pgpass
chmod 0600 ~/.pgpass
cd /local/suchanek/yago2s
nohup psql -a -d yago2s -h postgres0 -U yago -f ~/workspace/converters2s/scripts/flightplanner_postgres.sql > ~/workspace/flightdb.log &
