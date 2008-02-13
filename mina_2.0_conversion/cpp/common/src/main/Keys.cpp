#include "Keys.h"
#include <stdexcept>

Keys::Keys()
{
}

Keys::~Keys()
{
}

bool Keys::isEmpty()
{
	//TODO: Keys should be able to hold things
	return true;
}

void Keys::encode(XdrCoding *buf)
{
	if (isEmpty())
	{
		buf->enc((int32_t)0);
		return;
	}
	//TODO finish off
	throw std::logic_error("Keys::Encode doesn't do anything");
}

Keys *Keys::decode(XdrCoding *buf)
{
	int32_t keycount;
	buf->dec(&keycount);

	if (keycount == 0)
	{
		return new Keys(); //EMPTY_KEYS;
	}
	
	//TODO finish off
	throw std::logic_error("Keys::Decode doesn't do anything");
}