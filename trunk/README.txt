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

Avis will run on platforms with a Java 5 Standard Edition runtime. It
has been tested on Mac OS X Tiger (10.4), Windows XP and Windows
Server 2003, Fedora Core (3 & 6) and Debian Sarge (3.1).

NOTE: although Avis is platform-independent, the "avis.sh" script and
example command lines appearing later are for Unix environments.
Windows users can either translate as needed or run under cygwin
(http://www.cygwin.com).


Requirements
----------------------------------------------------------------------

Avis requires a Java 1.5 or later runtime. It will run fine with a
minimal Java Runtime Environment (JRE) but, if you wish to get optimal
performance, it is recommended you install the full Java 5 or Java 6
JDK to gain access to the "server" optimizing VM.

There is no requirement to build from source since platform-
independent binaries are included with the distribution, but if you do
wish to compile Avis you will need a Java Development Kit installed.
If you don't need to build Avis, you can skip to the next section.

Unless you plan to build Avis with Eclipse, you will also need Apache
Ant 1.6.0 or later (http://ant.apache.org).

Optional:

  * Eclipse 3.2 or later. Project files are included with the
    distribution. The version of Ant bundled with Eclipse is
    sufficient to build Avis. http://www.eclipse.org/downloads.

  * JavaCC 4.0 or later. Required if you wish to change the
    subscription parser. http://javacc.dev.java.net.

Very optional -- these only apply if you're working in the development
tree from SVN and need to update the web site:

  * Markdown 1.0.1 or later. Required to build the web
    site. http://daringfireball.net/projects/markdown.

  * rsync. Required to upload web site.

To build the router with Ant, change to the directory where you
extracted Avis and simply run Ant with the default build target:

  > cd avis-1.0
  > ant

This will build the file "lib/avisd.jar", which is the Avis event
router executable.

To see all build targets run:

  > ant -projecthelp


Usage
----------------------------------------------------------------------

To run the Avis event router service using the bash helper script:

  > cd avis-1.0
  > ./bin/avisd.sh

To see command line options:

  > ./bin/avis.sh -h

If you don't have a client for testing the server, you can try one of
the tickertape messaging clients at tickertape.org:

  http://tickertape.org/get_tickertape.html
