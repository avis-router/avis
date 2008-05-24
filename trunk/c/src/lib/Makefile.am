## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/lib \
              -I$(top_srcdir)/packages/hashtable
AM_CFLAGS   = -Wall -std=c89 -pedantic

public_inc = $(top_srcdir)/src/include/elvin

lib_LTLIBRARIES = libelvin.la

libelvin_la_SOURCES = \
  elvin.c \
  messages.c \
  named_values.c \
  byte_buffer.c \
  errors.c \
  values.c \
  log.c
  
libelvin_la_LIBADD = $(top_srcdir)/packages/hashtable/libhashtable.a
                      
include_HEADERS = \
  $(public_inc)/elvin.h \
  $(public_inc)/keys.h \
  $(public_inc)/named_values.h \
  $(public_inc)/log.h

