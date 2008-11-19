Mac OS X Avis Client Library
----------------------------------------------------------------------

This package contains the Avis client library, precompiled as a
Universal binary and packaged as an embeddable Mac OS X framework.

To use it, first copy "avis.framework" into the project directory:

  > mkdir MyProject/Frameworks
  > cp -rp avis.framework MyProject/Frameworks

In XCode, add "avis.framework" to the project. At this point you can
use the Avis client API and compile the project, but to actually run
your app you need to copy avis.framework to the build area:

  > cp -rp Frameworks/avis.frameworks build

If you forget to do this, you will see messages like "dyld: Library
not loaded: ... Reason: image not found".

A better way to do this is to set up a "Copy Files" target in XCode to
automatically do this for you when you run a build.


Non-Embedded Use
----------------------------------------------------------------------

To make this framework embeddable, the framework library's path is set
to be relative to the current executable, i.e.
"@executable_path/../Frameworks".

If you want to install the framework into one of the shared framework
areas for general use, you will need to rebuild from the source
distribution, after changing the "Installation Directory" setting on
avis.framework: e.g. to "/Library/Frameworks" or
"$(HOME)/Library/Frameworks".
