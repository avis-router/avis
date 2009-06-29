package org.avis.management.web;

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
    appendString (text);
    
    return this;
  }

  public HTML append (String text, Object... args)
  {
    int index = 0;      // current index within str
    int symStart = -1;  // start of next symbol (>= index)
    int symEnd = -1;    // end of next symbol (> symStart)

    for (;;)
    {
      symStart = text.indexOf ('$', index);
      
      if (symStart != -1)
      {      
        // append text between index and start of symbol
        appendString (text.substring (index, symStart));

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

        String symbol = text.substring (symStart, symEnd);
        int argIndex = Integer.valueOf (symbol);

        try
        {
          appendStringEscaped (args [argIndex - 1].toString ());

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
        appendString (text.substring (index));
        
        break;
      }
    }
    
    return this;
  }

  private void appendStringEscaped (String text)
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
      else if (c < 32 || c > 126)
        appendString ("&#" + (int)c + ";");
      else
        appendChar (c);
    }
  }

  private void appendString (String text)
  {
    for (int i = 0; i < text.length (); i++)
    {      
      char c = text.charAt (i);
      
      // allow ' in place of "
      if (c == '\'')
        c = '"';

      appendChar (c);
    }
  }

  private void appendChar (char c)
  {
    maybeAppendIndent ();
   
    atLineStart = (c == '\n');
    
    content.append (c);
  }

  private void maybeAppendIndent ()
  {
    if (atLineStart)
    {
      for (int i = 0; i < indent; i++)
        content.append ("  ");
    }
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

  public void indent ()
  {
    indent++;
  }

  public void outdent ()
  {
    if (indent > 0)
      indent--;
    else
      throw new IllegalStateException ("Cannot outdent");
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
}
