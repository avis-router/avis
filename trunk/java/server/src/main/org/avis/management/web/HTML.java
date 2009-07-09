package org.avis.management.web;

import java.util.Date;

import java.net.InetAddress;

import java.text.SimpleDateFormat;

/**
 * Basic HTML page generation template system.
 *  
 * @author Matthew Phillips
 */
public class HTML
{
  private StringBuilder content;
  private int indent;
  private boolean atLineStart;

  public HTML ()
  {
    this.content = new StringBuilder (4096);
    this.indent = 0;
    this.atLineStart = true;
  }
  
  public HTML appendXHTMLHeader (String title)
  {
    append 
      ("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" + 
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + 
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" + 
       "<head>\n" + 
       "  <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" + 
       "  <title>${1}</title>\n" + 
       "  <link href=\"main.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />\n" + 
      "</head>\n", title);
    
    return this;
  }

  public HTML append (String text)
  {
    appendTemplateString (text);
    
    return this;
  }

  public HTML append (String text, Object... args)
  {
    int index = 0;      // current index within str
    int symStart = -1;  // start of next symbol (>= index)
    int symEnd = -1;    // end of next symbol (> symStart)
    int nextArgIndex = 1;
    
    for (;;)
    {
      symStart = text.indexOf ('$', index);
      
      if (symStart != -1)
      {      
        // append text between index and start of symbol
        appendTemplateString (text.substring (index, symStart));

        // handle delimeter
        symStart++;
        
        if (symStart < text.length () &&
            text.charAt (symStart) == '{')
        {
          symStart++;
          
          // find end of symbol
          symEnd = text.indexOf ('}', symStart);
          
        } else
        {
          throw new IllegalArgumentException 
            ("Malformed sym starting at " + text.substring (index));
        }
        
        if (symEnd == -1)
        {
          throw new IllegalArgumentException 
            ("No closing } for sym starting at " + text.substring (index));
        }

        int argIndex;
        
        if (symEnd == symStart)
          argIndex = nextArgIndex++;
        else
          argIndex = Integer.valueOf (text.substring (symStart, symEnd));

        try
        {
          appendEscapedString (args [argIndex - 1].toString ());

          // advance to end of symbol
          index = symEnd + 1;
        } catch (IndexOutOfBoundsException ex)
        {
          throw new IllegalArgumentException 
            ("Arg index ${" + argIndex + "} not defined");
        }
      } else
      {
        // no symbol start found, add remaining characters and exit
        appendTemplateString (text.substring (index));
        
        break;
      }
    }
    
    return this;
  }

  public void appendImage (String image, String description)
  {
    appendString ("<img src=\"");
    appendEscapedString (image);
    appendString ("\" title=\"");
    appendEscapedString (description);
    appendString ("\" />");
  }
    
  private void appendEscapedString (String text)
  {
    for (int i = 0; i < text.length (); i++)
    {
      char c = text.charAt (i);
      
      if (c == '<')
        appendString ("&lt;");
      else if (c == '>')
        appendString ("&gt;");
      else if (c == '&')
        appendString ("&amp;");
      else if (c == '"')
        appendString ("&quot;");
      else if (c < 32)
        appendString ("&#" + (int)c + ";");
      else
        appendChar (c);
    }
  }

  /**
   * Append a string part of a the HTML template, with translation of
   * ' -> "
   */
  private void appendTemplateString (String text)
  {
    for (int i = 0; i < text.length (); i++)
    {      
      char c = text.charAt (i);
      
      if (c == '\'')
        c = '"';

      appendChar (c);
    }
  }
 
  private void appendString (String text)
  {
    for (int i = 0; i < text.length (); i++)
      appendChar (text.charAt (i));
  }

  private void appendChar (char c)
  {
    if (atLineStart && c != '\n')
    {
      for (int i = 0; i < indent; i++)
        content.append ("  ");
    }
       
    content.append (c);
    
    atLineStart = (c == '\n');
  }

  public String asText ()
  {
    return content.toString ();
  }
  
  @Override
  public String toString ()
  {
    return asText ();
  }

  public HTML indent ()
  {
    indent++;
    
    return this;
  }

  public HTML outdent ()
  {
    if (indent > 0)
      indent--;
    else
      throw new IllegalStateException ("Cannot outdent");
    
    return this;
  }

  public HTML appendBody ()
  {
    appendString ("<body>\n");
    
    return this;
  }

  public HTML appendClosingTags ()
  {
    appendString ("\n</body>\n</html>\n");
    
    return this;
  }

  public static String formatTime (long time)
  {
    return new SimpleDateFormat 
      ("yyyy-MM-dd HH:mm:ss.SSSZ").format (new Date (time));
  }

  /**
   * Format a number e.g. "1331" -> "1,331"
   */
  public static String num (int value)
  {
    return String.format ("%,d", value);
  }
  
  public static String host (InetAddress address)
  {
    return address.getCanonicalHostName () + " / " + address.getHostAddress (); 
  }
}
