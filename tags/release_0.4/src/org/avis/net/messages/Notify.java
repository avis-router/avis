package org.avis.net.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

import org.avis.net.security.Keys;

import static java.util.Collections.emptyMap;

import static org.avis.net.IO.getBool;
import static org.avis.net.IO.getNameValues;
import static org.avis.net.IO.putBool;
import static org.avis.net.IO.putNameValues;
import static org.avis.net.security.Keys.EMPTY_KEYS;

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
    throws ProtocolViolationException
  {
    attributes = getNameValues (in);
    deliverInsecure = getBool (in);
    keys = Keys.decode (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolViolationException
  {
    putNameValues (out, attributes);
    putBool (out, deliverInsecure);
    keys.encode (out);
  }
}
