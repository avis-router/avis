## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include $(all_includes)

AM_CFLAGS = @AVIS_CFLAGS@

AM_LDFLAGS = @AVIS_LDFLAGS@

noinst_PROGRAMS=hello_world

hello_world_SOURCES = hello_world.c

hello_world_LDADD = \
  $(top_srcdir)/src/lib/libavis.la
