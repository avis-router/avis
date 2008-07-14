package org.avis.io.messages;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static org.avis.io.XdrCoding.getBool;
import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putBool;
import static org.avis.io.XdrCoding.putString;
import static org.avis.security.Keys.EMPTY_KEYS;

public class SubAddRqst extends RequestMessage<SubRply>
{
  public static final int ID = 58;
  
  public String subscriptionExpr;
  public boolean acceptInsecure;
  public Keys keys;

  public SubAddRqst ()
  {
    // zip
  }
  
  public SubAddRqst (String subExpr)
  {
    this (subExpr, EMPTY_KEYS, true);
  }
  
  public SubAddRqst (String subExpr, Keys keys, boolean acceptInsecure)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subExpr;
    this.acceptInsecure = acceptInsecure;
    this.keys = keys;
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
    
    putString (out, subscriptionExpr);
    putBool (out, acceptInsecure);
    keys.encode (out);
  }
  
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    subscriptionExpr = getString (in);
    acceptInsecure = getBool (in);
    keys = Keys.decode (in);
  }
}
