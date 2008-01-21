#include "ConnRply.h"

ConnRply::ConnRply()
{
}

ConnRply::ConnRply(ConnRqst &inReplyTo, ConnectionOptions theoptions)
: XidMessage(inReplyTo)
{
	options = theoptions;
}

ConnRply::~ConnRply()
{
}

void ConnRply::encode(XdrCoding *out)
{
	XidMessage::encode(out);

	out->putNameValues(&options);
}

void ConnRply::decode(XdrCoding *in)
{
	XidMessage::decode(in);

	in->getNameValues(&options);
}
