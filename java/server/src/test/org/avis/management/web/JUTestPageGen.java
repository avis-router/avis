package org.avis.management.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JUTestPageGen
{
  @Test
  public void webPageGen ()
    throws Exception
  {
    HTML html = new HTML ();
    
    html.append ("<table class='client-list' border='1' cellspacing='0'>");
    
    html.indent ();
    html.append ("<tr><th>Client</th> <th>Connected</th> <th>Host</th>\n" + 
                 "  <th>Notifications</th> <th>Subscriptions</th></tr>");
    
    html.append ("<tr><td rowspan='2'>${1}</td><td>${2}</td>\n" + 
                 "  <td>${3}</td><td>${4}</td><td>${5}</td></tr>", 
                 12, "26/6/2009 10:43 AM (12 mins)", "hex.dsto.defence.gov.au", 
                 "2,123", "4");
    
    html.append ("</td></tr>");
    
    html.outdent ();
    
    html.append ("</table>");
    
    assertEquals
    ("<table class=\"client-list\" border=\"1\" cellspacing=\"0\"><tr><th>Client</th> <th>Connected</th> <th>Host</th>\n" + 
     "    <th>Notifications</th> <th>Subscriptions</th></tr><tr><td rowspan=\"2\">12</td><td>26/6/2009 10:43 AM (12 mins)</td>\n" + 
     "    <td>hex.dsto.defence.gov.au</td><td>2,123</td><td>4</td></tr></td></tr></table>", html.asText ());
    
    //System.out.println (html.asText ());
    
    // check that ' -> " translation is not done for vars
    html = new HTML ();
    
    html.append ("test '${1}'", "'hello'");
    
    assertEquals ("test \"'hello'\"", html.asText ());
  }
  
  @Test
  public void escaping ()
    throws Exception
  {
    HTML html = new HTML ();

    // check ' -> " conversion
    html.append ("<table class='client-list' border='1' cellspacing='0'>");
    
    assertEquals ("<table class=\"client-list\" border=\"1\" cellspacing=\"0\">", 
                  html.asText ());
    
    // check special character conversion
    
    html = new HTML ();
    html.append ("<input name='${1}'>", "<\"quoted'>, this is an Ž character");
    
    assertEquals ("<input name=\"&lt;&quot;quoted'&gt;, this is an &#233; " +
    		  "character\">", html.asText ());
    
    // System.out.println (html.asText ());
  }

  @Test
  public void errors ()
    throws Exception
  {
    // undef symbol
    HTML html = new HTML ();
    
    try
    {
      html.append ("${10}", "1");
      
      fail ();
    } catch (IllegalArgumentException ex)
    {
      // ok
    }
  
    // missing start {
    html = new HTML ();
    
    try
    {
      html.append ("$10", "1");
      
      fail ();
    } catch (IllegalArgumentException ex)
    {
      // ok
    }

    // missing close }
    
    html = new HTML ();
    try
    {
      html.append ("${10", "1");
      
      fail ();
    } catch (IllegalArgumentException ex)
    {
      // ok
    }
  }
}
