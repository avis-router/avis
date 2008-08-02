#ifndef CONNECT_REQUEST_MESSAGE_H
#define CONNECT_REQUEST_MESSAGE_H

#include "ConnectionOptions.h"
#include "RequestMessage.h"
#include "Keys.h"

class ConnRqst : public RequestMessage
{
public:

	enum { MSGID = 49 };

	//public static final Map<String, Object> EMPTY_OPTIONS = emptyMap();
	long versionMajor;
	long versionMinor;
	ConnectionOptions *options;
	Keys *notificationKeys;
	Keys *subscriptionKeys;

	ConnRqst();
	ConnRqst(long major, long minor);
	ConnRqst(long major, long minor, ConnectionOptions *theoptions,
		Keys *thenotificationKeys, Keys *thesubscriptionKeys);
	~ConnRqst();

	int32_t typeId() { return MSGID; };

	/*Class<ConnRply> replyType () { return ConnRply.class; }*/

	void encode(XdrCoding *out) throw(ProtocolCodecException);
	void decode(XdrCoding *in) throw(ProtocolCodecException);

	std::string name() { return "ConnRqst"; };
};

#endif //CONNECT_REQUEST_MESSAGE_H
