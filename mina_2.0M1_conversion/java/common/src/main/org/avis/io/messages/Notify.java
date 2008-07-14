package org.avis.io.messages;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import static org.avis.io.XdrCoding.getBool;
import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putBool;
import static org.avis.io.XdrCoding.putNameValues;
import static org.avis.security.Keys.EMPTY_KEYS;

/**
 * Base class for notify messages.
 * 
 * @author Matthew Phillips
 */
public abstract class Notify extends Message
{
  public Map<String, Object> attributes;
  public boolean deliverInsecure;
  public Keys keys;
 
  protected Notify ()
  {
    this.attributes = emptyMap ();
    this.deliverInsecure = true;
    this.keys = EMPTY_KEYS;
  }
    
  protected Notify (Object... attributes)
  {
    this (asAttributes (attributes), true, EMPTY_KEYS);
  }
  
  protected Notify (Map<String, Object> attributes,
                    boolean deliverInsecure,
                    Keys keys)
  {
    this.attributes = attributes;
    this.deliverInsecure = deliverInsecure;
    this.keys = keys;
  }
  
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    attributes = getNameValues (in);
    deliverInsecure = getBool (in);
    keys = Keys.decode (in);
  }

  @Override
  public void encode (IoBuffer out)
    throws ProtocolCodecException
  {
    putNameValues (out, attributes);
    putBool (out, deliverInsecure);
    keys.encode (out);
  }
  
  public static Map<String, Object> asAttributes (Object... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put ((String)pairs [i], pairs [i + 1]);

    return unmodifiableMap (map);
  }
}
