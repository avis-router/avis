#!/bin/sh

home=$(dirname $0)
prefix=/

usage ()
{
  local NL=$'\x0a'
  local help="\
  Usage: $0 [-h|--help] [-p|--prefix dir] $NL\

     -h|--help      : This text$NL\
     -p--prefix dir : Set install dir prefix (default is \"/\")"
 
  echo "$help" >&2
}

OPTS=`getopt -a -o p:h --long prefix:,help -n '$0' -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi

eval set -- "$OPTS"

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix|-p) prefix=$2 ; shift 2 ;;
    -h|--help) usage ; exit 0 ;;
    --) shift ; break ;;
    *) echo "!error" ; shift 1 ;;
  esac
done

chmod -R 0644 $home && \
chmod 0755 $home/usr/local/sbin/avisd && \
chmod 0755 $home/etc/init.d/avisd && \
chown -R root:root $home/root && \
cp -rpv $home/root/* $prefix
