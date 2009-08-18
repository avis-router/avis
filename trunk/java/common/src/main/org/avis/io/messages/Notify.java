package org.avis.io.messages;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
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
  
  /**
   * The raw serialised attributes for this notification. See
   * {@link #decode(IoBuffer)}.
   */
  public transient IoBuffer rawAttributes;
 
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
    /* Store a view on the raw attributes. This allows potential optimisation 
     * if this is used as the basis of a NotifyDeliver: we can just write the
     * raw attribtes back out rather than re-serialising them. */
    int start = in.position ();
    
    rawAttributes = in.slice ();
    
    attributes = getNameValues (in);

    rawAttributes.limit (in.position () - start);
    
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
