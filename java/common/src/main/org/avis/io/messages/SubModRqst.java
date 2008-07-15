package org.avis.io.messages;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static org.avis.io.XdrCoding.getBool;
import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putBool;
import static org.avis.io.XdrCoding.putString;
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
  
  public SubModRqst (long subscriptionId, String subscriptionExpr,
                     boolean acceptInsecure)
  {
    this (subscriptionId, subscriptionExpr,
          EMPTY_KEYS, EMPTY_KEYS, acceptInsecure);
  }
  
  public SubModRqst (long subscriptionId,
                     Keys addKeys, Keys delKeys,
                     boolean acceptInsecure)
  {
    this (subscriptionId, "", addKeys, delKeys, acceptInsecure);
  }
  
  public SubModRqst (long subscriptionId, String subscriptionExpr,
                     Keys addKeys, Keys delKeys,
                     boolean acceptInsecure)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subscriptionExpr;
    this.subscriptionId = subscriptionId;
    this.acceptInsecure = acceptInsecure;
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
  public void encode (IoBuffer out)
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
  public void decode (IoBuffer in)
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
