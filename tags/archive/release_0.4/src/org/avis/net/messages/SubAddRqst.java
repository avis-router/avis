package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

import org.avis.net.security.Keys;

import static org.avis.net.IO.getBool;
import static org.avis.net.IO.getString;
import static org.avis.net.IO.putBool;
import static org.avis.net.IO.putString;
import static org.avis.net.security.Keys.EMPTY_KEYS;

public class SubAddRqst extends XidMessage
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
    this (subExpr, EMPTY_KEYS);
  }
  
  public SubAddRqst (String subExpr, Keys keys)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subExpr;
    this.acceptInsecure = true;
    this.keys = keys;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolViolationException
  {
    super.encode (out);
    
    putString (out, subscriptionExpr);
    putBool (out, acceptInsecure);
    keys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    super.decode (in);
    
    subscriptionExpr = getString (in);
    acceptInsecure = getBool (in);
    keys = Keys.decode (in);
  }
}