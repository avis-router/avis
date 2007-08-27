Name: avis
Summary: Avis event router
URL: http://avis.sourceforge.net/
Version: %{_avis_version}
Release: %{_avis_release}
License: GPL
Packager: Matthew Phillips <avis@mattp.name>
Vendor: (none)
# Requires: java >= 0:1.4
Group: System/Servers
# BuildRequires: java-devel >= 0:1.4, ant, jpackage-utils
BuildArchitectures: noarch
BuildRoot: %{_builddir}/%{name}-root
Source0: avis-src-%{_avis_version}.zip

%description
Avis is an event router service compatible with the commercial Elvin
implementation developed by Mantara Software. Avis provides a
general-purpose, fast and scalable publish/subscribe message bus using
content-based subscriptions.

%prep
%setup -q

%build
unset CLASSPATH
cd server
ant

%install
rm -rf $RPM_BUILD_ROOT
install -Dp -m 0755 -o root -g root \
  server/bin/avisd $RPM_BUILD_ROOT/%{_sbindir}/avisd
install -Dp -m 0644 -o root -g root \
  server/lib/avisd.jar $RPM_BUILD_ROOT/%{_libdir}/avisd.jar
install -Dp -m 0644 -o root -g root \
  server/etc/avisd.config $RPM_BUILD_ROOT/%{_sysconfdir}/avis/avisd.config

# service
sed -e "s|__CONFDIR__|%{_sysconfdir}|g" \
    -e "s|__BINDIR__|%{_sbindir}|g" \
  < server/packaging/fedora/init_script.in > %{_tmppath}/avisd.tmp
install -Dp -m 0755 -o root -g root \
  %{_tmppath}/avisd.tmp $RPM_BUILD_ROOT/etc/init.d/avisd
rm %{_tmppath}/avisd.tmp

%files
%defattr(-,root,root)
%{_sbindir}/avisd
%{_libdir}/avisd.jar
/etc/init.d/avisd
%config %{_sysconfdir}/avis/avisd.config
