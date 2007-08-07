This is an interim Avis install package for Redhat Fedora (Core 3 and
later) until an RPM is available.

Avis's main dependency is a Java 5 (or later) runtime. See
http://java.sun.com/javase/downloads for an RPM installer.


Installation
----------------------------------------------------------------------

You can either install the files directly to /usr/local, and the
service in /etc/init.d using:

  > sudo ./install.sh --prefix=/usr/local --service

Or, using GNU stow (http://www.gnu.org/software/stow):

  > AVIS_HOME=/usr/local/stow/avis-0.7
  > sudo mkdir $AVIS_HOME
  > sudo ./install.sh --prefix=$AVIS_HOME
  > (cd $AVIS_HOME/.. && sudo stow $(basename $AVIS_HOME))
  > sudo ./install.sh --prefix=/usr/local --service

This way is recommended, since it allows safe, easy uninstall of Avis
with:

  > sudo chkconfig avisd --del
  > sudo rm /etc/init.d/avisd
  > (cd $AVIS_HOME/.. && sudo stow --delete $(basename $AVIS_HOME))

To start the service:

  > sudo service avisd start


Manual Uninstallation
----------------------------------------------------------------------

An uninstall target is not provided in the script since the author
doesn't want to risk damaging your system. If you don't use stow to
manage installation/uninstallation, the files installed can be found
by prepending the install prefix (default is /usr/local) to each line
of the output of the the following command in the directory you
untarred the installation package:

  > (cd root && find)

The service init script is installed to:

  /etc/init.d/avisd
