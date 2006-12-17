#!/bin/sh

if [ -e ../lib/avisd.jar ]; then
  AVISD=../lib/avisd.jar
elif [ -e lib/avisd.jar ]; then
  AVISD=lib/avisd.jar
else
  echo "Cannot find avisd.jar"
  exit 1
fi

java -server -jar $AVISD $*
