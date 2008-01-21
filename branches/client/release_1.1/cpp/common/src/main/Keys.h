#ifndef KEYS_H
#define KEYS_H

#include "XdrCoding.h"

class Keys
{
public:
	Keys();
	~Keys();

	void encode(XdrCoding *buf);
	static Keys *decode(XdrCoding *buf);

	bool isEmpty();

};

#endif //KEYS_H
