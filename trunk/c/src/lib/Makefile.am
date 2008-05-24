## Process this file with automake to produce Makefile.in

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/lib
AM_CFLAGS   = -Wall -std=c89 -pedantic

public_inc = $(top_srcdir)/src/include/elvin

lib_LTLIBRARIES = libelvin.la

libelvin_la_SOURCES = elvin.c \
                      messages.c \
                      named_values.c \
                      byte_buffer.c \
                      errors.c \
                      log.c
                      
include_HEADERS = $(public_inc)/elvin.h \
	          $(public_inc)/keys.h \
	          $(public_inc)/named_values.h \
	          $(public_inc)/log.h	          

