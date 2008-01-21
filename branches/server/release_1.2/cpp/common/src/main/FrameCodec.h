#ifndef FRAME_CODEC_H
#define FRAME_CODEC_H

#include <winsock2.h>
#include "Message.h"

class FrameCodec
{
public:
	FrameCodec();
	~FrameCodec();

	void encode(SOCKET sd, Message *msg);
	Message *decode(SOCKET sd);

private:
	void ReadFrame(SOCKET sd, XdrCoding *marshall);
	Message *CreateMsg(int32_t msgtype);

};

#endif //FRAME_CODEC_H
