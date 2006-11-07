package org.avis.net.messages;

import static org.avis.net.IO.getObjects;
import static org.avis.net.IO.getString;
import static org.avis.net.IO.putObjects;
import static org.avis.net.IO.putString;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

public class Nack extends XidMessage
{
  public static final int ID = 48;

  public static final int PROT_ERROR = 1001;
  public static final int PARSE_ERROR = 2101;
  public static final int NO_SUCH_SUB = 1002;
 
  private static final Object [] EMPTY_ARGS = new Object [0];
  
  public int error;
  public String message;
  public Object [] args;
  
  @Override
  public int typeId ()
  {
    return ID;
  }

  public Nack ()
  {
    // zip
  }
  
  public Nack (XidMessage inReplyTo, int error, String message)
  {
    this (inReplyTo, error, message, EMPTY_ARGS);
  }
  
  public Nack (XidMessage inReplyTo, int error, String message, Object ...args)
  {
    super (inReplyTo);
    
    this.error = error;
    this.message = message;
    this.args = args;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolViolationException
  {
    super.encode (out);
    
    out.putInt (error);
    putString (out, message);
    putObjects (out, args);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    super.decode (in);
    
    error = in.getInt ();
    message = getString (in);
    args = getObjects (in);
  }
}
