package org.avis.federation.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.Notify;
import org.avis.security.Keys;

import static org.avis.io.XdrCoding.getStringArray;
import static org.avis.io.XdrCoding.putStringArray;

public class FedNotify extends Notify
{
  public static final int ID = 195;
  
  public String [] routing;
  
  public FedNotify ()
  {
    // zip
  }
 
  public FedNotify (Map<String, Object> attributes,
                    boolean deliverInsecure,
                    Keys keys,
                    String [] routing)
  {
    super (attributes, deliverInsecure, keys);
    
    this.routing = routing;
  }
  
  public FedNotify (Notify original, String [] routing)
  {
    this (original.attributes, original.deliverInsecure, 
          original.keys, routing);
  }

  /**
   * True if the routing list contains a given server domain.
   */
  public boolean routingContains (String serverDomain)
  {
    for (String domain : routing)
    {
      if (domain.equals (serverDomain))
        return true;
    }
    
    return false;
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    routing = getStringArray (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    putStringArray (out, routing);    
  }
}
