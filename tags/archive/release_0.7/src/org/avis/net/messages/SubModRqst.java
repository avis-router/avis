package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.net.security.Keys;

import static org.avis.net.IO.getBool;
import static org.avis.net.IO.getString;
import static org.avis.net.IO.putBool;
import static org.avis.net.IO.putString;
import static org.avis.net.security.Keys.EMPTY_KEYS;

public class SubModRqst extends XidMessage
{
  public static final int ID = 59;
  
  public long subscriptionId;
  public String subscriptionExpr;
  public boolean acceptInsecure;
  public Keys addKeys;
  public Keys delKeys;

  public SubModRqst ()
  {
    // zip
  }
  
  public SubModRqst (long subscriptionId, String subscriptionExpr)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subscriptionExpr;
    this.subscriptionId = subscriptionId;
    this.acceptInsecure = true;
    this.addKeys = EMPTY_KEYS;
    this.delKeys = EMPTY_KEYS;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    out.putLong (subscriptionId);
    putString (out, subscriptionExpr);
    putBool (out, acceptInsecure);
    addKeys.encode (out);
    delKeys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    subscriptionId = in.getLong ();
    subscriptionExpr = getString (in);
    acceptInsecure = getBool (in);
    addKeys = Keys.decode (in);
    delKeys = Keys.decode (in);
  }
}
