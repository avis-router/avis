package org.avis.federation.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.Notify;
import org.avis.security.Keys;

import static org.avis.io.IO.getStringArray;
import static org.avis.io.IO.putStringArray;

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
