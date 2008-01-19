Name:         avis
Summary:      Event Router
Vendor:       Matthew Phillips
Packager:     Avis Project <avis@mattp.name>
Distribution: Sourceforge
Group:        System/Servers
License:      GPL
Version:      %{_avis_version}
Release:      %{_avis_release}
URL:          http://avis.sourceforge.net/

#   build information
Prefix:       %{_prefix}
BuildRoot:    %{_topdir}
AutoReq:      no
AutoReqProv:  no
BuildArchitectures: noarch
# Requires:     java >= 0:1.5
# Requires:     jdk >= 0:1.5

%description
    Avis is a multicast event bus. It provides a fast, publish/subscribe
    event routing service compatible with the commercial Elvin
    implementation developed by Mantara Software. Elvin routers can be
    federated together to form wide-area event notification networks.
    Clients can exchange events with other clients anywhere on the bus,
    subscribing to messages using pattern-matching expressions that
    select messages based on their content.

%install
    # create installation hierarchy
    mkdir -p -m 755 \
        $RPM_BUILD_ROOT%{_prefix}/bin \
        $RPM_BUILD_ROOT%{_prefix}/libexec/avis \
        $RPM_BUILD_ROOT%{_prefix}/var/avis \
        $RPM_BUILD_ROOT/etc/avis \
        $RPM_BUILD_ROOT/etc/init.d

    # install avis and ec/ep libs
   install -c -m 644 \
        %{_avis_server}/lib/avis-router.jar \
        %{_avis_client}/lib/avis-client.jar \
        %{_avis_client}/lib/avis-tools.jar \
        $RPM_BUILD_ROOT%{_prefix}/libexec/avis/

    # install default server configuration
    install -c -m 644 \
        %{_avis_server}/etc/avisd.config \
        %{_avis_server}/etc/avis-router.keystore \
        $RPM_BUILD_ROOT/etc/avis/

    # install ec/ep
    ( echo "#!/bin/sh"
      echo "exec %{_prefix}/bin/java -Xverify:none \\%{nil}"
      echo "    -cp %{_prefix}/libexec/avis/avis-tools.jar:%{_prefix}/libexec/avis/avis-client.jar \\%{nil}"
      echo "    org.avis.tools.Ec \${1+\"\$@\"}"
    ) > %{_tmppath}/ec

    ( echo "#!/bin/sh"
      echo "exec %{_prefix}/bin/java -Xverify:none \\%{nil}"
      echo "    -cp %{_prefix}/libexec/avis/avis-tools.jar:%{_prefix}/libexec/avis/avis-client.jar \\%{nil}"
      echo "    org.avis.tools.Ep \${1+\"\$@\"}"
    ) > %{_tmppath}/ep

    install -c -m 755 \
      %{_tmppath}/ec %{_tmppath}/ep $RPM_BUILD_ROOT%{_prefix}/bin/

    install -Dp -m 0755 \
      %{_avis_server}/bin/avisd \
      $RPM_BUILD_ROOT%{_prefix}/sbin/avisd
 
    # init script
    sed -e "s|__PREFIX__|%{_prefix}|g" \
      < %{_avis_server}/packaging/fedora/rc_init_script.in > %{_tmppath}/avisd
    
    install -Dp -m 0755 \
      %{_tmppath}/avisd $RPM_BUILD_ROOT/etc/init.d/

%files
     %defattr(-,root,root)
     %{_prefix}/sbin/avisd
     %{_prefix}/bin/ec
     %{_prefix}/bin/ep
     %{_prefix}/libexec/avis/avis-router.jar
     %{_prefix}/libexec/avis/avis-tools.jar
     %{_prefix}/libexec/avis/avis-client.jar
     /etc/init.d/avisd
     %config /etc/avis/avisd.config
     %config /etc/avis/avis-router.keystore
