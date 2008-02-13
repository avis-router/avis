#ifndef CONNECT_REPLY_MESSAGE_H
#define CONNECT_REPLY_MESSAGE_H

#include "XidMessage.h"
#include "ConnRqst.h"

class ConnRply : public XidMessage
{
public:
	enum { MSGID = 50 };
	
	/** Options requested by client that are supported. */
	ConnectionOptions options;

	ConnRply();
	ConnRply(ConnRqst &inReplyTo, ConnectionOptions theoptions);
	~ConnRply();

	int32_t typeId() { return MSGID; };
	void encode(XdrCoding *out) throw(ProtocolCodecException);
	void decode(XdrCoding *in) throw(ProtocolCodecException);

	std::string name() { return "ConnRply"; };
};

#endif //CONNECT_REPLY_MESSAGE_H
