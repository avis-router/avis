#include "ConnRqst.h"


ConnRqst::ConnRqst()
{
}

ConnRqst::ConnRqst(long major, long minor)
{
	//ConnRqst(major, minor, EMPTY_OPTIONS, EMPTY_KEYS, EMPTY_KEYS);
}

ConnRqst::ConnRqst(long major, long minor, ConnectionOptions *theoptions,
					Keys *thenotificationKeys, Keys *thesubscriptionKeys)
: RequestMessage(nextXid())
{
	versionMajor = major;
	versionMinor = minor;
	options = theoptions;
	notificationKeys = thenotificationKeys;
	subscriptionKeys = thesubscriptionKeys;
}

ConnRqst::~ConnRqst()
{
}

void ConnRqst::encode(XdrCoding *out)
{
	RequestMessage::encode(out);

	out->enc(versionMajor);
	out->enc(versionMinor);

	out->putNameValues(options);

	notificationKeys->encode(out);
	subscriptionKeys->encode(out);
}


void ConnRqst::decode(XdrCoding *in)
{
	RequestMessage::decode(in);

	in->dec(&versionMajor);
	in->dec(&versionMinor);

	in->getNameValues(options);

	notificationKeys = Keys::decode(in);
	subscriptionKeys = Keys::decode(in);
}
