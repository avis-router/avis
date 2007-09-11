Avis
======================================================================

This package contains the Avis publish/subscribe message router. For
more information on the Avis project please see:

  http://avis.sourceforge.net/


Installation
----------------------------------------------------------------------

Simply unzip the distribution.


Supported Platforms
----------------------------------------------------------------------

Avis will run on any platform with a Java 5 Standard Edition
runtime. It has been tested on Mac OS X Tiger (10.4), Windows XP and
Windows Server 2003, Fedora Core 6 and Debian Sarge (3.1).


Requirements
----------------------------------------------------------------------

Avis requires Java 1.5.0 or later. There is no requirement to build
Avis since binaries are included with the distribution, but if you do
wish to compile Avis you will need a Java Development Kit
installed. If you don't need to build Avis, you can skip to the next
section.

Unless you plan to build Avis with Eclipse, you will also need Apache
Ant 1.6.0 or later (http://ant.apache.org).

Optional:

  * Eclipse 3.2 or later. Project files are included with the
    distribution. The version of Ant bundled with Eclipse is
    sufficient to build Avis.

  * JavaCC 4.0 or later. Required if you wish to change the
    subscription parser. http://javacc.dev.java.net/

Very optional - these only apply if you're working in the development
tree from SVN:

  * Markdown 1.0.1 or later. Required to build the web
    site. http://daringfireball.net/projects/markdown/

  * Rsync. Required to upload web site.

To build the router with Ant, change to the directory where you
extracted Avis and simply run Ant with the default build target:

  > cd avis-0.5
  > ant

This will build the file "build/avisd.jar", which is the Avis event
router executable.

To see all build targets run:

  > ant -projecthelp


Usage
----------------------------------------------------------------------

To run the Avis event router service using the bash helper script:

  > cd avis-0.5
  > ./bin/avisd.sh

To see command line options:

  > ./bin/avis.sh -h

You can also just use:

  > java -server -jar build/avisd.jar

The "-server" switch is optional: it enables the HotSpot Server VM
which performs more advanced optimizations targeted at long-running
servers.

If you don't have a client for testing the server, you can try one of
the tickertape messaging clients at tickertape.org:

  http://tickertape.org/get_tickertape.html
