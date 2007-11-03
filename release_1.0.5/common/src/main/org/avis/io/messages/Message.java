package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static org.avis.util.Text.className;

/**
 * Base class for all message types.
 * 
 * @author Matthew Phillips
 */
public abstract class Message
{
  /**
   * The message's unique type ID.<p>
   * 
   * Message ID's (from Elvin Client Protocol 4.0 draft):
   * 
   * <pre>
   * UNotify        = 32,
   *   
   * Nack           = 48,   ConnRqst       = 49,
   * ConnRply       = 50,   DisconnRqst    = 51,
   * DisconnRply    = 52,   Disconn        = 53,
   * SecRqst        = 54,   SecRply        = 55,
   * NotifyEmit     = 56,   NotifyDeliver  = 57,
   * SubAddRqst     = 58,   SubModRqst     = 59,
   * SubDelRqst     = 60,   SubRply        = 61,
   * DropWarn       = 62,   TestConn       = 63,
   * ConfConn       = 64,
   *   
   * QnchAddRqst    = 80,   QnchModRqst    = 81,
   * QnchDelRqst    = 82,   QnchRply       = 83,
   * SubAddNotify   = 84,   SubModNotify   = 85,
   * SubDelNotify   = 86
   * </pre>
   */
  public abstract int typeId ();

  public abstract void encode (ByteBuffer out)
    throws ProtocolCodecException;

  public abstract void decode (ByteBuffer in)
    throws ProtocolCodecException;
  
  @Override
  public String toString ()
  {
    return name ();
  }

  public String name ()
  {
    return className (this);
  }
}
