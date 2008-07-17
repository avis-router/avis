# Process this file with autoconf to produce a configure script.

# read in the Avis release numbers

define([version_major], 
       esyscmd([. avis-client.properties && printf "%s" ${avis_version_major}]))

define([version_minor], 
       esyscmd([. avis-client.properties && printf "%s" ${avis_version_minor}]))

define([version_patch], 
       esyscmd([. avis-client.properties && printf "%s" ${avis_version_patch}]))

# Prelude.
AC_PREREQ([2.61])
AC_INIT([Avis Client For C++], version_major.version_minor.version_patch, 
        [phillipsm@sourceforge.net])

# place to put some extra build scripts installed
AC_CONFIG_AUX_DIR([automake])

# Call check's ./configure script recursively.
AC_CONFIG_SUBDIRS([packages/zthread])

# Checks for programs.
AC_PROG_CC
AC_PROG_LIBTOOL

AC_PATH_PROGS(AR, ar, no, $PATH:/usr/ccs/bin:/usr/local/bin)

if test x$AR = xno ; then
  AC_MSG_ERROR([Avis requires ar, but cannot find it.])
fi

# fairly severe build strictness
# change foreign to gnu or gnits to comply with gnu standards
AM_INIT_AUTOMAKE([-Wall foreign 1.9.5])

# Checks for libraries.

# This macro is defined in check.m4 and tests if check.h and
# libcheck.a are installed in your system. It sets CHECK_CFLAGS and
# CHECK_LIBS accordingly.  
#  AM_PATH_CHECK([MINIMUM-VERSION,
#                [ACTION-IF-FOUND [, ACTION-IF-NOT-FOUND]]])
#AM_PATH_CHECK()

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS([stdlib.h])
AC_CHECK_HEADERS([stdint.h])
AC_CHECK_HEADERS([stdbool.h])
AC_CHECK_HEADERS([endian.h])

# Checks for library functions.
AC_FUNC_MALLOC

# gcc-specific stuff
if `echo "$CC" | grep gcc >/dev/null 2>&1`; then
  AVIS_CFLAGS="$AVIS_CFLAGS -Wall -pedantic"
  AVIS_STD_C89="-std=gnu89"
fi

# use gnu99 on gcc 4 and later
if `"$CC" --version | grep "gcc-4" >/dev/null 2>&1`; then
  AVIS_STD_C99="-std=gnu99"
fi

# linked libraries needed by various platforms
case $host in 
  *-*-linux*)
    AVIS_LDFLAGS="$AVIS_LDFLAGS -lm"
    ;;
  sparc-sun-solaris*)
    AVIS_LDFLAGS="$AVIS_LDFLAGS -lm -lsocket"
    ;;
esac

AC_SUBST(AVIS_STD_C89)
AC_SUBST(AVIS_STD_C99)
AC_SUBST(AVIS_CFLAGS)
AC_SUBST(AVIS_LDFLAGS)

# Output files
AC_CONFIG_HEADERS([platforms/gnu_automake/avis_client_config.h])

AC_CONFIG_FILES([
   Makefile ])

SUBDIRS=packages/zthread

AC_OUTPUT
