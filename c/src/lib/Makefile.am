## Process this file with automake to produce Makefile.in

public_inc = $(top_srcdir)/src/include/elvin

AM_CPPFLAGS = \
  -I$(top_srcdir)/src/include \
  -I$(top_srcdir)/lib \
  -I$(top_srcdir)/packages/hashtable \
  -DAVIS_SHARED_LIB

AM_CFLAGS   = -Wall -std=c89 -pedantic

lib_LTLIBRARIES = libelvin.la

libelvin_la_SOURCES = \
  elvin.c \
  elvin_uri.c \
  messages.c \
  named_values.c \
  byte_buffer.c \
  errors.c \
  values.c \
  keys.c \
  log.c \
  array_list.c

libelvin_la_LIBADD = \
  $(top_srcdir)/packages/hashtable/libhashtable.la

include_HEADERS = \
  $(public_inc)/elvin.h \
  $(public_inc)/elvin_uri.h \
  $(public_inc)/keys.h \
  $(public_inc)/values.h \
  $(public_inc)/named_values.h \
  $(public_inc)/log.h
