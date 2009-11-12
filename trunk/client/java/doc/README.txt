Avis Client Library
======================================================================

The Avis client library is a Java 5 API for accessing Elvin event
routers. This includes the Avis event router and the Mantara Elvin
product.

If you're not sure where to get started, have a look at the example
code in client/src/examples and the associated documentation in
client/doc/examples.txt. The API documentation is in client/javadoc.

The Avis web site is http://avis.sourceforge.net.


Distribution Layout
======================================================================

server/
  common/                       Avis common packages
  router/                       Avis router sources for testing

client/java                     Avis client library packages
  build.xml                     Ant build targets

  src/main/                     Source for client packages
  src/examples/                 Example code
  src/test/                     Test cases for client packages

  doc/examples.txt              How to use the example code

  doc/javadoc/                  API documentation

  bin/                          The ec, ep and sha1 utilities

  lib/avis-client.jar           The Avis client library
  lib/avis-tools.jar            The Avis tools library (for ec, etc)



Using In Eclipse
======================================================================

To add the Avis client library to your project:
 
 * Right click your project -> choose Properties.

 * Select "Java Build Path".

 * Select the "Libraries" tab at the top.

 * Choose "Add Jars..." and select "client/lib/avis-client.jar" from
   the Avis client distribution. Or copy the JAR file beforehand into
   your project and select that.

 * Expand the new avis-client.jar entry, select "Source attachment" ->
   "Edit..."

 * Select "External folder..." and choose "client/src/main" from the
   Avis distribution.

This will add the JAR file and connect the source code to it for
browsing purposes.


Building Avis
======================================================================

Building (Ant 1.6 or later)
----------------------------------------------------------------------

The Avis client library comes pre-built in the client/lib directory of
this distribution. If you wish to modify the library, you may re-build
it as below:

  > cd avis-client-1.1.0/client
  > ant

To see all build targets:

  > ant -projecthelp


Building (Eclipse 3.2 or later)
----------------------------------------------------------------------

From Eclipse:

  * Select File -> Import...

  * Select "Existing Projects Into Workspace..."

  * Select the root of the Avis client library e.g. avis-client-1.1.0.

  * The projects avis.common, avis.client and avis.server should be
    found: select all three.

You can add the client/build.xml file to the Eclipse Ant view to
access Ant build targets from the IDE.
