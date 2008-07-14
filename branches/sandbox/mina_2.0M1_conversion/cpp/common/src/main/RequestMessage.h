#ifndef REQUEST_MESSAGE_H
#define REQUEST_MESSAGE_H

#include "XidMessage.h"

class RequestMessage /*<R extends XidMessage>*/ : public XidMessage
{
public:
	RequestMessage() : XidMessage() {};
	RequestMessage(int32_t xid) : XidMessage(xid) {};

  //TODO need to work out how to do this in C++
  /**
   * The type of a successful reply.
   */
  //public abstract Class<R> replyType ();
};

#endif //REQUEST_MESSAGE_H
