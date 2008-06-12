## Process this file with automake to produce Makefile.in

public_inc = $(top_srcdir)/src/include/avis

AM_CPPFLAGS = \
  -I$(top_srcdir)/src/include \
  -I$(top_srcdir)/lib \
  -I$(top_srcdir)/packages/hashtable \
  -DAVIS_SHARED_LIB

AM_CFLAGS = -Wall -std=gnu89 -pedantic

lib_LTLIBRARIES = libelvin.la

libelvin_la_SOURCES = \
  elvin.c \
  elvin_uri.c \
  messages.c \
  attributes.c \
  byte_buffer.c \
  errors.c \
  values.c \
  keys.c \
  log.c \
  array_list.c \
  array_list_private.h \
  attributes_private.h \
  byte_buffer.h \
  avis_endian.h \
  messages.h \
  values_private.h

libelvin_la_LIBADD = \
  $(top_srcdir)/packages/hashtable/libhashtable.la

include_HEADERS = \
  $(public_inc)/array_list.h \
  $(public_inc)/attributes.h \
  $(public_inc)/elvin_uri.h \
  $(public_inc)/elvin.h \
  $(public_inc)/errors.h \
  $(public_inc)/keys.h \
  $(public_inc)/log.h \
  $(public_inc)/stdtypes.h \
  $(public_inc)/values.h
