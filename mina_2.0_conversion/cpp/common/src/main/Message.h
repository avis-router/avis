#ifndef MESSAGE_H
#define MESSAGE_H

#include "XdrCoding.h"
#include "ProtocolCodecException.h"

class Message
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
public:

  virtual int32_t typeId() = 0;
  virtual void encode(XdrCoding *out) throw(ProtocolCodecException) = 0;
  virtual void decode(XdrCoding *in) throw(ProtocolCodecException) = 0;  
  virtual std::string name() = 0;
  virtual std::string toString() { return name(); };  
};

#endif //MESSAGE_H
