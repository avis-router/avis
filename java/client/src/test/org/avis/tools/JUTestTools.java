package org.avis.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.router.Router;
import org.avis.router.RouterOptions;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

import static org.junit.Assert.assertEquals;

public class JUTestTools
{
  public static final String ELVIN_URI = "elvin://127.0.0.1:29170";
  
  private ByteArrayOutputStream output;
  private PrintStream oldStdErr;
  private PrintStream oldStdOut;
  
  @Before
  public void setup ()
  {
    output = new ByteArrayOutputStream (4096);

    oldStdErr = System.err;
    oldStdOut = System.out;
    
    System.setErr (new PrintStream (output, true));
    System.setOut (new PrintStream (output, true));
  }
  
  @After
  public void teardown () 
    throws IOException
  {
    System.setErr (oldStdErr);
    System.setOut (oldStdOut);
    
    output.close ();
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
    
    waitForOutput 
      ("ec: Connected to server elvin:4.0/tcp,none,xdr/127.0.0.1:29170\n" +
       "$time\n" + 
       "test: 1\n" + 
       "---\n");
    
    ec.close ();
    
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
