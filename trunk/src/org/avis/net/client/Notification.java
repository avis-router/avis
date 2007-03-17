package org.avis.net.client;

import java.util.Map;
import java.util.Map.Entry;

public class Notification
{
  Map<String, Object> attributes;
  
  @Override
  public String toString ()
  {
    StringBuilder str = new StringBuilder ();
    boolean first = true;
    
    for (Entry<String, Object> entry : attributes.entrySet ())
    {
      if (!first)
        str.append ('\n');
      
      first = false;
      
      str.append (entry.getKey ());
      str.append (": ");
      str.append (entry.getValue ());
    }
    
    return str.toString ();
  }
}
