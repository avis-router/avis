This is an interim install package until an RPM is available. This
package is just an image of the Avis install structure, with the
"root" directory as its base. You can either install the files in root
using:

  > sudo install.sh

Which will copy the files in "root" to "/" and set the correct
permissions and ownerships.

Or, using GNU stow:

  > sudo install.sh --prefix=/usr/local/stow/avis-0.7
  > sudo stow /usr/local/stow/avis-0.7
