package org.avis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.avis.net.security.Keys;

import static org.avis.net.security.Keys.EMPTY_KEYS;

public class Notification implements Map<String, Object>, Cloneable
{
  public Keys keys;
  
  private HashMap<String, Object> fields;
  
  public Notification ()
  {
    this.fields = new HashMap<String, Object> ();
    this.keys = EMPTY_KEYS;
  }

  public void clear ()
  {
    fields.clear ();
  }

  @SuppressWarnings("unchecked")
  public Object clone ()
    throws CloneNotSupportedException
  {
    Notification copy = (Notification)super.clone ();
    
    copy.fields = (HashMap<String, Object>)fields.clone ();
    
    return copy;
  }

  public boolean containsKey (Object key)
  {
    return fields.containsKey (key);
  }

  public boolean containsValue (Object value)
  {
    return fields.containsValue (value);
  }

  public Set<Entry<String, Object>> entrySet ()
  {
    return fields.entrySet ();
  }

  public boolean equals (Object arg0)
  {
    return fields.equals (arg0);
  }

  public Object get (Object key)
  {
    return fields.get (key);
  }

  public int hashCode ()
  {
    return fields.hashCode ();
  }

  public boolean isEmpty ()
  {
    return fields.isEmpty ();
  }

  public Set<String> keySet ()
  {
    return fields.keySet ();
  }

  public Object put (String key, Object value)
  {
    return fields.put (key, value);
  }

  public void putAll (Map<? extends String, ? extends Object> m)
  {
    fields.putAll (m);
  }

  public Object remove (Object key)
  {
    return fields.remove (key);
  }

  public int size ()
  {
    return fields.size ();
  }

  public String toString ()
  {
    return fields.toString ();
  }

  public Collection<Object> values ()
  {
    return fields.values ();
  }
}
