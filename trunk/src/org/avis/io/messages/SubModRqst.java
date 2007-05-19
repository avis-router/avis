package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static org.avis.io.IO.getBool;
import static org.avis.io.IO.getString;
import static org.avis.io.IO.putBool;
import static org.avis.io.IO.putString;
import static org.avis.security.Keys.EMPTY_KEYS;

public class SubModRqst extends RequestMessage<SubRply>
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
    this (subscriptionId, subscriptionExpr, EMPTY_KEYS, EMPTY_KEYS);
  }
  
  public SubModRqst (long subscriptionId,
                     Keys addKeys, Keys delKeys)
  {
    this (subscriptionId, "", addKeys, delKeys);
  }
  
  public SubModRqst (long subscriptionId, String subscriptionExpr,
                     Keys addKeys, Keys delKeys)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subscriptionExpr;
    this.subscriptionId = subscriptionId;
    this.acceptInsecure = true;
    this.addKeys = addKeys;
    this.delKeys = delKeys;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public Class<SubRply> replyType ()
  {
    return SubRply.class;
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
