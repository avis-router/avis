public_inc = $(top_srcdir)/src/include/elvin

lib_LTLIBRARIES = libelvin.la

libelvin_la_SOURCES = elvin.c messages.c named_values.c
include_HEADERS = $(public_inc)/elvin.h $(public_inc)/keys.h $(public_inc)/named_values.h

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/lib 
