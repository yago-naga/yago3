#!/bin/sh
if [ `hostname` != "d5blade05" ]; then
  echo We should not run this on contact!
  exit
fi
echo Loading YAGO into the database
echo This process runs in the background and logs to db.log.
echo Pressing CTRL+C will not stop the process.
cd /local/users/yago
echo Enter the password: yago2itnyago
psql -a -d yago2s -h postgres0 -U yago -f ~/workspace/converters2s/scripts/postgres.sql > ~/workspace/db.log &
disown -h %1
sleep 5s
tail -f ~/workspace/db.log

