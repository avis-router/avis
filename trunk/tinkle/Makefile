svn2cl=../support/svn2cl-0.12/svn2cl.sh

BUILD_CONFIG=Release

SOURCES=src/main/*.m src/main/*.h

PROJECT_FILE=Tinkle.xcodeproj/project.pbxproj
APP=build/$(BUILD_CONFIG)/Tinkle.app/Contents/MacOS/Tinkle
DISK_IMAGE=build/$(BUILD_CONFIG)/Tinkle.dmg

all: $(DISK_IMAGE)

$(APP): $(SOURCES) $(PROJECT_FILE)
	xcodebuild -project Tinkle.xcodeproj -configuration $(BUILD_CONFIG) -target Tinkle

$(DISK_IMAGE): $(APP)
	xcodebuild -project Tinkle.xcodeproj -configuration $(BUILD_CONFIG) -target "Disk Image"

changelog:
	  $(svn2cl) -r {2010-01-05}:HEAD \
	    https://avis.svn.sourceforge.net/svnroot/avis/trunk/tinkle
