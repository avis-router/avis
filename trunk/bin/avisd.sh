#!/bin/sh

set -e

daemon=0
avisd_opts=""
logfile="/dev/null" 
avisd_jar=`dirname $0`/../lib/avisd.jar

if [ ! -e $avisd_jar ]; then
  echo "Cannot find avisd.jar"
  exit 1
fi

while [ $# -gt 0 ]; do
  case $1 in
    -pidfile) pidfile=$2; shift 2;;
    -daemon)  daemon=1; shift;;
    -logfile) logfile=$2; shift 2;;
    *)        avisd_opts="$avisd_opts $1"; shift;;
  esac
done

command="java -server -jar $avisd_jar $avisd_opts"

if [ $daemon == 1 ]; then
  (
    exec $command < /dev/null 2>&1 > $logfile
  ) &
  
  if [ "x$pidfile" != "x" ]; then
    echo $! > "$pidfile"
  fi
else
  if [ "x$pidfile" != "x" ]; then
    echo $$ > "$pidfile"
  fi
  
  exec $command
fi
