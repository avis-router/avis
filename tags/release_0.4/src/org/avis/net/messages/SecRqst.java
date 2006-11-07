package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

import org.avis.net.security.Keys;

public class SecRqst extends XidMessage
{
  public static final int ID = 54;
  
  public Keys addNtfnKeys;
  public Keys delNtfnKeys;
  public Keys addSubKeys;
  public Keys delSubKeys;
  
  public SecRqst ()
  {
    // zip
  }
  
  public SecRqst (Keys addNtfnKeys, Keys delNtfnKeys,
                  Keys addSubKeys, Keys delSubKeys)
  {
    super (nextXid ());
    
    this.addNtfnKeys = addNtfnKeys;
    this.delNtfnKeys = delNtfnKeys;
    this.addSubKeys = addSubKeys;
    this.delSubKeys = delSubKeys;
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
    
    addNtfnKeys.encode (out);
    delNtfnKeys.encode (out);
    addSubKeys.encode (out);
    delSubKeys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    super.decode (in);
    
    addNtfnKeys = Keys.decode (in);
    delNtfnKeys = Keys.decode (in);
    addSubKeys = Keys.decode (in);
    delSubKeys = Keys.decode (in);
  }
}
