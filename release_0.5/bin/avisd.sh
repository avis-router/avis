#!/bin/sh

if [ -e build/avisd.jar ]; then
  AVISD=build/avisd.jar
elif [ -e avisd.jar ]; then
  AVISD=avisd.jar
else
  AVISD=../build/avisd.jar
fi

java -server -jar $AVISD $*
