## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/src/lib \
              -I$(top_srcdir)/packages/check/src $(all_includes)

AM_CFLAGS = -Wall -std=c99 -pedantic

bin_PROGRAMS=tests basic_test

tests_SOURCES = tests.c check_ext.c check_messages.c check_client.c

tests_LDADD = \
  $(top_srcdir)/src/lib/libelvin.la \
  $(top_srcdir)/packages/check/src/libcheck.la

basic_test_SOURCES = basic_test.c

basic_test_LDADD = \
  $(top_srcdir)/src/lib/libelvin.la
