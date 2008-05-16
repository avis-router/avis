bin_PROGRAMS=check_messages

check_messages_SOURCES=check_messages.c \
		       check_ext.c

check_messages_LDADD=$(top_srcdir)/src/lib/libelvin.la \
                     $(top_srcdir)/packages/check/src/libcheck.la

AM_CPPFLAGS = -I$(top_srcdir)/src/include -I$(top_srcdir)/src/lib \
              -I$(top_srcdir)/packages/check/src $(all_includes) \
              -Wall -Werror

AM_CFLAGS = -std=c99