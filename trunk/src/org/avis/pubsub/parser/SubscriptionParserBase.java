package org.avis.pubsub.parser;

import org.avis.util.Format;

public abstract class SubscriptionParserBase
{
  /**
   * Strip backslashed codes from a string.
   */
  protected static String stripBackslashes (String text)
  {
    return Format.stripBackslashes (text);
  }
  
  /**
   * Expand backslash codes such as \n \x90 etc into their literal values.
   */
  protected static String expandBackslashes (String text)
  {
    if (text.indexOf ('\\') != -1)
    {
      StringBuilder buff = new StringBuilder (text.length ());
      
      for (int i = 0; i < text.length (); i++)
      {
        char c = text.charAt (i);
        
        if (c == '\\')
        {
          c = text.charAt (++i);
          
          switch (c)
          {
            case 'n':
              c = '\n'; break;
            case 't':
              c = '\t'; break;
            case 'b':
              c = '\b'; break;
            case 'r':
              c = '\r'; break;
            case 'f':
              c = '\f'; break;
            case 'a':
              c = 7; break;
            case 'v':
              c = 11; break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
              int value = c - '0';
              int end = Math.min (text.length (), i + 3);
              
              while (i + 1 < end && octDigit (text.charAt (i + 1)))
              {
                c = text.charAt (++i);
                value = value * 8 + (c - '0');                
              }
              
              c = (char)value;
              break;
            case 'x':
              value = 0;
              end = Math.min (text.length (), i + 3);
              
              do
              {
                c = text.charAt (++i);
                value = value * 16 + hexValue (c);
              } while (i + 1 < end && hexDigit (text.charAt (i + 1)));
              
              c = (char)value;
              break;
          }
        }

        buff.append (c);
      }
      
      text = buff.toString ();
    }
    
    return text;
  }
  
  private static boolean octDigit (char c)
  {
    return c >= '0' && c <= '7';
  }
  
  private static boolean hexDigit (char c)
  {
    return (c >= '0' && c <= '9') ||
           (c >= 'a' && c <= 'f') ||
           (c >= 'A' && c <= 'F');
  }

  private static int hexValue (char c)
  {
    if (c >= '0' && c <= '9')
      return c - '0';
    else if (c >= 'a' && c <= 'f')
      return c - 'a' + 10;
    else
      return c - 'A' + 10;
  }

  protected static String className (Class type)
  {
    String name = type.getName ();
    
    return name.substring (name.lastIndexOf ('.') + 1);
  }
}
