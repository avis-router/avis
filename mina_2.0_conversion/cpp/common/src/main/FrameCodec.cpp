#include "FrameCodec.h"
#include "ConnRqst.h"
#include "ConnRply.h"

FrameCodec::FrameCodec()
{
}

FrameCodec::~FrameCodec()
{
}

void FrameCodec::encode(SOCKET sd, Message *msg)
{
	XdrCoding *marshall = new XdrCoding();
	
	marshall->space_for(4);
	marshall->setpos(4);
	marshall->enc(msg->typeId());
	msg->encode(marshall);
	int32_t endpos = marshall->getpos();
	marshall->setpos(0);
	marshall->enc(endpos-4);

	unsigned char *thebytes = marshall->getbytes();
	if (send(sd, (char *)thebytes, marshall->size(), 0) == SOCKET_ERROR) 
	{
		printf("send() failed\n");
	}

	delete [] thebytes;
}

void FrameCodec::ReadFrame(SOCKET sd, XdrCoding *marshall)
{
	char recvmessage[1024];
	do 
	{
		int recvbytes = recv(sd, recvmessage, 1024, 0);
		marshall->appendbytes(recvmessage, recvbytes);
	}
	while(marshall->getpos() < 4); 

	size_t currentpos = marshall->getpos();
	marshall->setpos(0);

	int32_t framesize;
	marshall->dec(&framesize);

	marshall->setpos(currentpos);

	while(marshall->getpos() < framesize)
	{
		int recvbytes = recv(sd, recvmessage, 1024, 0);
		marshall->enc(recvmessage, recvbytes);
	}

	marshall->setpos(0);
}

Message *FrameCodec::CreateMsg(int32_t msgtype)
{
	switch(msgtype)
	{
	case ConnRply::MSGID:
		return new ConnRply();
		break;
	case ConnRqst::MSGID:
		return new ConnRqst();
		break;
	default:
		throw ProtocolCodecException("Unknown Message ID");
		break;
	}
}

Message *FrameCodec::decode(SOCKET sd)
{
	XdrCoding *marshall = new XdrCoding();

	ReadFrame(sd, marshall);

	//assume we got a whole valid message
	int32_t framesize;
	marshall->dec(&framesize);
	int32_t msgtype;
	marshall->dec(&msgtype);

	Message *themsg = CreateMsg(msgtype);
	
	themsg->decode(marshall);

	delete marshall;

	return themsg;
}
