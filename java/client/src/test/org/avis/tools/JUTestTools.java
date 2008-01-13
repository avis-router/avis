package org.avis.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Writer;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.avis.client.Elvin;
import org.avis.client.ElvinOptions;
import org.avis.client.Notification;
import org.avis.client.TestNtfnListener;
import org.avis.router.Router;
import org.avis.router.RouterOptions;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

import static org.junit.Assert.assertEquals;

public class JUTestTools
{
  public static final String ELVIN_URI = "elvin://127.0.0.1:29170";
  public static final String SECURE_ELVIN_URI = "elvin:/secure/127.0.0.1:29171";
  
  protected Writer input;
  protected ByteArrayOutputStream output;
  protected PrintStream oldStdErr;
  protected PrintStream oldStdOut;
  protected InputStream oldStdIn;
  
  @Before
  public void setup () 
    throws IOException
  {
    output = new ByteArrayOutputStream (4096);
    PipedInputStream inputStr = new PipedInputStream ();
    input = new OutputStreamWriter (new PipedOutputStream (inputStr), "UTF-8");
    
    oldStdErr = System.err;
    oldStdOut = System.out;
    oldStdIn = System.in;
    
    System.setErr (new PrintStream (output, true));
    System.setOut (new PrintStream (output, true));
    System.setIn (inputStr);
  }
  
  @After
  public void teardown () 
    throws IOException
  {
    System.setErr (oldStdErr);
    System.setOut (oldStdOut);
    System.setIn (oldStdIn);
    
    output.close ();
    input.close ();
  }
  
  @Test
  public void ec () 
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", ELVIN_URI);
    
    Router router = new Router (options);
    
    Elvin client = new Elvin (ELVIN_URI);
   
    Ec ec = new Ec (new EcOptions ("-e", ELVIN_URI, "require (test)"));
    
    client.send (new Notification ("test", 1));
    client.close ();
    
    waitForOutput 
      ("ec: Connected to server elvin:4.0/tcp,none,xdr/127.0.0.1:29170\n" +
       "$time\n" + 
       "test: 1\n" + 
       "---\n");
    
    ec.close ();
    
    router.close ();
  }
  
  @Test
  public void ecTLS () 
    throws Exception
  {
    URL routerKeystoreUrl = getClass ().getResource ("router.ks");
    URL clientKeystoreUrl = getClass ().getResource ("client.ks");
    
    RouterOptions routerOptions = new RouterOptions ();
    
    routerOptions.set ("Listen", SECURE_ELVIN_URI);
    routerOptions.set ("TLS.Keystore", routerKeystoreUrl);
    routerOptions.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (routerOptions);
    
    ElvinOptions clientOptions = new ElvinOptions ();
    clientOptions.setKeystore (clientKeystoreUrl, "testing");
    
    Elvin client = new Elvin (SECURE_ELVIN_URI, clientOptions);
   
    Ec ec = new Ec (new EcOptions ("-e", SECURE_ELVIN_URI, "-k", 
                                   routerKeystoreUrl.getPath (), 
                                   "testing", "require (test)"));
    
    client.send (new Notification ("test", 1));
    client.close ();
    
    waitForOutput 
      ("ec: Connected to server elvin:4.0/tcp,none,xdr/127.0.0.1:29170\n" +
       "$time\n" + 
       "test: 1\n" + 
       "---\n");
    
    ec.close ();
    
    router.close ();
  }
  
  @Test
  public void ep () 
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", ELVIN_URI);
    
    Router router = new Router (options);
    
    Elvin client = new Elvin (ELVIN_URI);

    TestNtfnListener ntfnListener = 
      new TestNtfnListener (client.subscribe ("require (test)"));
    
    new Thread ()
    {
      @Override
      public void run ()
      {
        try
        {
          new Ep (new EpOptions ("-e", ELVIN_URI));
        } catch (Exception ex)
        {
          ex.printStackTrace (oldStdErr);
        }
      }
    }.start ();
    
    input.append ("test: 1\n---\n");
    input.close ();
    
    ntfnListener.waitForNotification ();
    
    router.close ();
  }

  private void waitForOutput (String expectedOutput) 
    throws Exception
  {
    long endAt = currentTimeMillis () + 10000;
    String actualOutput = "";
    
    do
    {
      System.err.flush ();
      System.out.flush ();
      
      actualOutput = new String (output.toByteArray (), "UTF-8");
      actualOutput = 
        actualOutput.replaceAll (System.getProperty ("line.separator"), "\n");
      actualOutput = actualOutput.replaceAll ("\\$time .+\n", "\\$time\n");
      
      if (actualOutput.equals (expectedOutput))
        return;
      
      sleep (200);
      
    } while (currentTimeMillis () < endAt);
    
    assertEquals (expectedOutput, actualOutput);
  }
}
