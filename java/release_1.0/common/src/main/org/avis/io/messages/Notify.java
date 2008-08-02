package org.avis.io.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static java.util.Collections.emptyMap;

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
  
  protected Notify (Map<String, Object> attributes,
                    boolean deliverInsecure,
                    Keys keys)
  {
    this.attributes = attributes;
    this.deliverInsecure = deliverInsecure;
    this.keys = keys;
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    attributes = getNameValues (in);
    deliverInsecure = getBool (in);
    keys = Keys.decode (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    putNameValues (out, attributes);
    putBool (out, deliverInsecure);
    keys.encode (out);
  }
}
