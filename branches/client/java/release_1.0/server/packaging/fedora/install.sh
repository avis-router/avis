#!/bin/sh

# Avis install script for Redhat/Fedora.
#
# Run with -h option for usage

home=$(dirname $0)
root=$home/root
prefix=/usr/local
install_service=0

usage ()
{
  local NL=$'\x0a'
  local help="\
  Usage: $0 [-h|--help] [--prefix dir] [--service]$NL\

     -h|--help      : This text$NL\
     --prefix dir   : Set install dir prefix (default is \"/usr/local\")$NL\
     --service      : Install as service in etc/rc.d"

  echo "$help" >&2
}

OPTS=`getopt -o h --long prefix:,service,help -n '$0' -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi

eval set -- "$OPTS"

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix) prefix=$2 ; shift 2 ;;
    --service) install_service=1 ; shift 1 ;;
    -h|--help) usage ; exit 0 ;;
    --) shift ; break ;;
    *) echo "!error" ; shift 1 ;;
  esac
done

# install if no --service OR if avisd not already installed
if [ $install_service == 0 ] || [ ! -e $prefix/sbin/avisd ]; then
  install -DCp -m 0755 -o root -g root $root/sbin/avisd $prefix/sbin/avisd && \
  install -DCp -m 0644 -o root -g root $root/lib/avisd.jar $prefix/lib/avisd.jar && \
  install -DCp -m 0644 -o root -g root $root/etc/avis/avisd.config $prefix/etc/avis/avisd.config
fi

if [ $install_service == 1 ]; then
  sed -e "s|__CONFDIR__|$prefix/etc|g" \
      -e "s|__BINDIR__|$prefix/sbin|g" \
    < init_script.in > avisd.tmp && \
  install -DCp -m 0755 -o root -g root avisd.tmp /etc/init.d/avisd && \
  rm avisd.tmp && \
  chkconfig --add avisd && \
  chkconfig avisd on
fi

