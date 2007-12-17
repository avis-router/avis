package org.avis.router;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import org.avis.federation.Connector;
import org.avis.io.messages.NotifyEmit;
import org.avis.logging.Log;
import org.avis.util.IllegalConfigOptionException;

import static org.avis.federation.FederationManager.managerFor;
import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.util.Streams.close;

/**
 * Full integration test between three routers. Starts each router as
 * if started from command line and federates in a V: router1 ->
 * router2 and router1 -> router3.
 * 
 * @author Matthew Phillips
 */
public class JUTestMain
{
  @Test
  public void main ()
    throws Exception
  {
    File config1 = configFile
    (
      "Listen=elvin://127.0.0.1:29170\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Connect[Test]=" +
        "ewaf://127.0.0.1:29181 ewaf://127.0.0.1:29191\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Request-Timeout=2"
    );
    
    File config2 = configFile
    (
      "Listen=elvin://127.0.0.1:29180\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Listen=ewaf://127.0.0.1:29181\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Apply-Class[Test]=@127.0.0.1"
    );
    
    File config3 = configFile
    (
      "Listen=elvin://127.0.0.1:29190\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Listen=ewaf://127.0.0.1:29191\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Apply-Class[Test]=@127.0.0.1"
    );

    Log.enableLogging (Log.INFO, false);
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);

    Router router2 = startRouter (config2);
    Router router3 = startRouter (config3);
    Router router1 = startRouter (config1);

    for (Connector connector : managerFor (router1).connectors ())
      waitForConnect (connector);
    
    SimpleClient client1 = new SimpleClient ("127.0.0.1", 29180);
    SimpleClient client2 = new SimpleClient ("127.0.0.1", 29190);
    
    client1.connect ();
    client1.subscribe ("test == 1");
    
    client2.connect ();
    client2.subscribe ("test == 2");
    
    client1.send (new NotifyEmit ("test", 2));
    
    client2.receive ();
    
    client2.send (new NotifyEmit ("test", 1));
    
    client1.receive ();
    
    client1.close ();
    client2.close ();
    
    router1.close ();
    router2.close ();
    router3.close ();
  }

  private static Router startRouter (File config) 
    throws IllegalConfigOptionException, IOException
  {
    return Main.start ("-c", config.getAbsolutePath ());
  }

  private static File configFile (String contents)
    throws IOException
  {
    File configFile = File.createTempFile ("avis", ".conf");
    
    configFile.deleteOnExit ();
    
    PrintWriter configStream = new PrintWriter (configFile);
    
    try
    {
      configStream.append (contents);
    } finally
    {
      close (configStream);
    }
    
    return configFile;
  }
}