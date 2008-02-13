#include "XidMessage.h"

long XidMessage::xidCounter = 0;

XidMessage::XidMessage()
: xid(-1)
{
}

XidMessage::XidMessage(XidMessage &inReplyTo)
: xid(inReplyTo.xid)
{
	
}

XidMessage::XidMessage(int32_t thexid)
: xid(thexid)
{
	
}

XidMessage::~XidMessage()
{
}


void XidMessage::decode(XdrCoding *in)
{
	in->dec(&xid);
}


void XidMessage::encode(XdrCoding *out)
{
	if (xid == -1)
	{
		throw new ProtocolCodecException("No XID");
	}

	out->enc(xid);
}
