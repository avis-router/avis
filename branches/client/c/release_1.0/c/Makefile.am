ACLOCAL_AMFLAGS = -I automake-macros

SUBDIRS = packages . src doc

EXTRA_DIST = \
  .cproject \
  .project \
  .settings \
  avis-client.properties \
  README.txt \
  LICENSE.txt \
  platforms/gnu_automake/readme.txt \
  platforms/macosx/avis_client.xcodeproj/project.pbxproj \
  platforms/macosx/avis_client_config.h \
  platforms/macosx/avis-Info.plist \
  platforms/windows/vc2005/avis_client_config.h \
  platforms/windows/vc2005/Avis.sln \
  platforms/windows/vc2005/avislib.vcproj \
  platforms/windows/vc2005/avistests.vcproj \
  platforms/windows/vc2005/basic_test.vcproj \
  platforms/windows/vc2005/check.sln \
  platforms/windows/vc2005/event_loop_test.vcproj \
  platforms/windows/vc2005/hashtable.sln \
  platforms/windows/vc2005/libcheck.vcproj \
  platforms/windows/vc2005/libcheck/config.h \
  platforms/windows/vc2005/libhashtable.vcproj

run-tests: all
	./src/test/tests

api-docs:
	(cd doc/api && make api-docs)

changelog:
	svn2cl -r ${release_rev}:HEAD https://avis.svn.sourceforge.net/svnroot/avis/trunk/c
