## Process this file with automake to produce Makefile.in

public_inc = $(top_srcdir)/src/include/avis

AM_CPPFLAGS = \
  -I$(top_srcdir)/src/include \
  -I$(top_srcdir)/lib \
  -I$(top_srcdir)/packages/hashtable \
  -DAVIS_SHARED_LIB

AM_CFLAGS = @AVIS_CFLAGS@ @AVIS_STD_C89@

AM_LDFLAGS = @AVIS_LDFLAGS@

lib_LTLIBRARIES = libavis.la

libavis_la_SOURCES = \
  elvin.c \
  elvin_uri.c \
  errors_private.h \
  messages.c \
  sha1.c \
  attributes.c \
  byte_buffer.c \
  errors.c \
  values.c \
  keys.c \
  listeners.c \
  listeners.h \
  log.c \
  arrays.c \
  arrays_private.h \
  attributes_private.h \
  byte_buffer.h \
  keys_private.h \
  log.h \
  avis_endian.h \
  messages.h \
  values_private.h

libavis_la_LIBADD = \
  $(top_srcdir)/packages/hashtable/libhashtable.la

includedir=${prefix}/include/avis

include_HEADERS = \
  $(public_inc)/arrays.h \
  $(public_inc)/attributes.h \
  $(public_inc)/defs.h \
  $(public_inc)/elvin_uri.h \
  $(public_inc)/elvin.h \
  $(public_inc)/errors.h \
  $(public_inc)/keys.h \
  $(public_inc)/stdtypes.h \
  $(public_inc)/values.h
