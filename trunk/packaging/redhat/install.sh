#!/bin/sh

home=$(dirname $0)
prefix=/

OPTS=`getopt -o p: --long prefix: -n '$0' -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi

eval set -- "$OPTS"

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix) prefix=$2 ; shift 2 ;;
    --) shift ; break ;;
    *) echo "!error" ; shift 1 ;;
  esac
done

chown -R root:root $home/root && \
cp -rp $home/root/* $prefix
