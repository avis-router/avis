## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include $(all_includes)

AM_CFLAGS = @AVIS_CFLAGS@

AM_LDFLAGS = @AVIS_LDFLAGS@

noinst_PROGRAMS=hello_world secure_sender secure_receiver

hello_world_SOURCES = hello_world.c
hello_world_LDADD = $(top_srcdir)/src/lib/libavis.la

secure_sender_SOURCES = secure_sender.c
secure_sender_LDADD = $(top_srcdir)/src/lib/libavis.la

secure_receiver_SOURCES = secure_receiver.c
secure_receiver_LDADD = $(top_srcdir)/src/lib/libavis.la
