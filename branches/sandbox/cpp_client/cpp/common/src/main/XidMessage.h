#ifndef XIDMESSAGE_H
#define XIDMESSAGE_H

#include "Message.h"

class XidMessage : public Message
{
public:
	int32_t xid;

	XidMessage();
	XidMessage(XidMessage &inReplyTo);
	XidMessage(int32_t thexid);
	~XidMessage();

	//virtual int typeId() = 0;

	void decode(XdrCoding *in) throw(ProtocolCodecException);
	void encode(XdrCoding *out) throw(ProtocolCodecException);

protected:
	static long nextXid() { return ++xidCounter; };

private:

	//TODO make this threadsafe
	static long xidCounter;

	//TODO check if we need this in the client
	/**
	* The request message that triggered this reply. This is for the
	* convenience of message processing, not part of the serialized
	* format: you need to add a {@link RequestTrackingFilter} to the
	* filter chain if you want this automatically filled in.
	*/
	//transient RequestMessage<?> request;
};

#endif //XIDMESSAGE_H
