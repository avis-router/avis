package org.avis.management.web;import org.avis.federation.FederationManager;import org.avis.federation.FederationOptionSet;import org.avis.logging.Log;import org.avis.router.Router;import org.avis.router.RouterOptionSet;import org.avis.router.RouterOptions;public class RunWebManager{  public static void main (String [] args) throws Exception  {    Log.enableLogging (Log.DIAGNOSTIC, true);    Log.info ("Starting Avis with web manager", RunWebManager.class);        RouterOptionSet routerOptionSet = new RouterOptionSet ();        routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);    routerOptionSet.inheritFrom (WebManagementOptionSet.OPTION_SET);        RouterOptions config = new RouterOptions (routerOptionSet);        config.set ("Port", 29170);        config.set ("Federation.Connect[Public]", "ewaf://hex");    config.set ("Federation.Provide[Public]", "TRUE");    config.set ("Federation.Subscribe[Public]", "require (Test)");        config.set ("Management.Listen", "http://localhost:8017");    config.set ("Management.Admin-Name", "admin");    config.set ("Management.Admin-Password", "foo2");//    config.set ("TLS.Keystore", "avis-router.keystore");//    config.set ("TLS.Keystore-Passphrase", "avis-router");//    config.setRelativeDirectory ("tmp");        Router router = new Router (config);        new FederationManager (router, config);    new WebManagementManager (router, config);  }}