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
  platforms/macosx/README.txt \
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

dist_windows     = ${PACKAGE_TARNAME}-win32-${PACKAGE_VERSION}
dist_mac         = ${PACKAGE_TARNAME}-macosx-${PACKAGE_VERSION}
dist_windows_dir = tmp/${dist_windows}
dist_mac_dir     = tmp/${dist_mac}

run-tests: all
	./src/test/tests

api-docs:
	(cd doc/api && make api-docs)

changelog:
	svn2cl -r ${release_rev}:HEAD https://avis.svn.sourceforge.net/svnroot/avis/trunk/c

dist-windows: api-docs
	cmd.exe /c 'call "C:\Program Files\Microsoft Visual Studio 8\VC\vcvarsall.bat" && devenv platforms/windows/vc2005/Avis.sln /Build Release /Project avis'
	mkdir -p ${dist_windows_dir}/include/avis
	cp LICENSE.txt ${dist_windows_dir}
	cp src/include/avis/*.h ${dist_windows_dir}/include/avis 
	cp platforms/windows/vc2005/Release/avis.dll ${dist_windows_dir}
	cp platforms/windows/vc2005/Release/avis.lib ${dist_windows_dir}
	rsync -av doc/api/html/ ${dist_windows_dir}/doc
	rm -f ${dist_windows}.zip
	cd ${dist_windows_dir}/.. && zip -r ../${dist_windows}.zip .
	rm -rf ${dist_windows_dir}

dist-mac: api-docs
	mkdir -p ${dist_mac_dir}
	cp LICENSE.txt ${dist_mac_dir}
	cp platforms/macosx/README.txt ${dist_mac_dir}
	xcodebuild -project platforms/macosx/avis_client.xcodeproj \
	  -target avis -configuration Release
	cp -rp platforms/macosx/build/Release/avis.framework ${dist_mac_dir}
	chmod -R og-w ${dist_mac_dir}/avis.framework
	rsync -av doc/api/html/ ${dist_mac_dir}/doc
	tar jcvf ${dist_mac}.tar.bz2 -C tmp .
	rm -rf ${dist_mac_dir}
