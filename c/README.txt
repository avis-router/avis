Avis C Client Library
======================================================================

This is the source distribution of the Avis client library for C. The
Avis client library provides a C API for accessing Elvin event
routers. This includes the Avis event router and the Mantara Elvin
product.

If you're not sure where to get started, have a look at the API
documentation in doc/api/html.

The Avis web site is http://avis.sourceforge.net.


Supported Platforms
======================================================================

The client library is written in portable C and is actively tested on
Mac OS X, Linux, Solaris (5.9 and later), and Microsoft Windows XP. It
can be built from the command line using the GNU automake-based build
scripts, or using the projects provided or the Apple Xcode and
Microsoft Visual Studio 2005 IDE's.


Distribution Layout
======================================================================

configure                     GNU automake configure script.

doc/
  api/html/                   API documentation.

src/

  include/avis/               Public header files for the library.
  lib/                        Source for the client library.
  test/                       Unit tests.

packages/

  check/                      Check unit testing framework.
                              (modified from 0.9.5 release)
  hashtable/                  C hash table implementation.

platforms/

  gnu_automake/               For building under GNU automake.
  macosx/                     For building on Mac OS X Xcode.
  windows/
    vc2005/                   For building on Windows under Visual
                              Studio 2005.


Building The Client Library
======================================================================

GNU Automake
----------------------------------------------------------------------

  > ./configure
  > make
  > sudo make install

Optionally, if you have an accessible Elvin router, you can run the
unit tests:

  > ./src/test/tests

Set the ELVIN environment variable to change the default Elvin URI for
the router. e.g.

  > export ELVIN="elvin://public.elvin.org"
 
 
Mac OS X
----------------------------------------------------------------------

The client library will build from the command line using the steps
above, or you can build in Xcode (3.0 or later) using the project in
"platforms/macosx".

Windows
----------------------------------------------------------------------

Open the "platforms/windows/vc2005/Avis.sln" file under Visual Studio
2005 or later.


Using In Eclipse C Development Tools (CDT)
======================================================================

To import the Avis client library to your workspace (you need the
Eclipse CDT plugin version 4.0.x or later):
 
 * Choose File -> Import...

 * Select "Existing project into workspace"


License
======================================================================

The Avis client library is distributed under the terms of the GNU
Lesser General Public license version 3 (LGPL v3). 
