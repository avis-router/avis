## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/src/lib \
              -I$(top_srcdir)/packages/check/src $(all_includes)

AM_CFLAGS = -Wall -std=gnu89 -pedantic -lm

noinst_PROGRAMS=tests basic_test event_loop_test

tests_SOURCES = \
  tests.c \
  check_messages.c \
  check_security.c \
  check_client.c \
  check_collections.c \
  check_ext.h

tests_LDADD = \
  $(top_srcdir)/src/lib/libavis.la \
  $(top_srcdir)/packages/check/src/libcheck.la

basic_test_SOURCES = basic_test.c

basic_test_LDADD = \
  $(top_srcdir)/src/lib/libavis.la

event_loop_test_SOURCES = event_loop_test.c

event_loop_test_LDADD = \
  $(top_srcdir)/src/lib/libavis.la
