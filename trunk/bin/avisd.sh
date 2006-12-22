#!/bin/sh

avisd_jar=`dirname $0`/../lib/avisd.jar

if [ ! -e $avisd_jar ]; then
  echo "Cannot find avisd.jar"
  exit 1
fi

java -server -jar $avisd_jar $*
