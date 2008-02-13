/* Copyright (c) 2006, Pedro Larroy Tovar 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
#include "XdrCoding.h"
#include <iostream>

//#include "utf8.h"
//using namespace utf8;

using namespace std;

XdrCoding::XdrCoding() : pos(0) {
	OWNED = true;
	buff = new vector<uint8_t>;
}
XdrCoding::XdrCoding(vector<uint8_t>* buff) : pos(0) {
	OWNED = false;
	this->buff = buff;
}
XdrCoding::XdrCoding(vector<uint8_t>* buff, size_t pos) {
	OWNED = false;
	this->buff = buff;
	this->pos = pos;
}

XdrCoding::~XdrCoding() {
	if(OWNED) delete buff;
}

XdrCoding& XdrCoding::resetpos() {
	pos = 0;
	return *this;
}

XdrCoding& XdrCoding::setbuff(vector<uint8_t>*thebuff) {
	if(OWNED) delete buff;
	OWNED = false;
	this->buff = thebuff;
	this->pos = 0;
	return *this;
}

XdrCoding& XdrCoding::setbuff(vector<uint8_t>*buff, size_t pos) {
	if(OWNED) delete buff;
	OWNED = false;
	this->buff = buff;
	this->pos = pos;
	return *this;
}	

vector<uint8_t>* XdrCoding::getbuff() {
	return buff;
}

XdrCoding& XdrCoding::space_for(size_t size) {
	DLOG(cout << "pos: " << pos << " size: " << size <<  " buff->size(): " << buff->size() << " capacity: " << buff->capacity() << endl;)
	if( pos >= buff->size() ) {
		if( (pos + size) < size )
			throw(overflow_error("XdrCoding size_t overflow"));
		DLOG(cout << "resize(" << pos+size << ")" << endl;)
		buff->resize(pos + size);
	} else {
		size_t avail = buff->size() - pos;
		if(avail < size) {
			DLOG(cout << "resize(" << size-avail << ")" << endl;)
			buff->resize(buff->size() + size - avail);
		} else
			return *this;
	}
	return *this;
}

void XdrCoding::ck_space_avl(size_t size) {
	if( pos > buff->size() - 1 || buff->size() - pos < size )
		throw(underflow_error("XdrCoding buffer underflow"));
}

XdrCoding& XdrCoding::enc(int32_t value) {
	DLOG(cout << "enc int32_t: " << value << "  pos: " << pos << endl;)
	space_for(sizeof(int32_t));
	uint32_t tmp;
    if (value < 0) {
        value = 0 - value;
        tmp = 0 - static_cast<uint32_t>(value);
    } else 
		tmp = static_cast<uint32_t>(value);
#ifdef DEBUG	
    (*buff).at(pos)	 = (tmp >> 24) & 0x000000ff;
    (*buff).at(pos+1)= (tmp >> 16) & 0x000000ff;
    (*buff).at(pos+2)= (tmp >>  8) & 0x000000ff;
    (*buff).at(pos+3)= (tmp >>  0) & 0x000000ff;
#else
    (*buff)[pos]	= (tmp >> 24) & 0x000000ff;
    (*buff)[pos+1]	= (tmp >> 16) & 0x000000ff;
    (*buff)[pos+2]	= (tmp >>  8) & 0x000000ff;
    (*buff)[pos+3]	= (tmp >>  0) & 0x000000ff;
#endif
	pos+=sizeof(int32_t);
	return *this;
}

XdrCoding& XdrCoding::enc(uint32_t value) {
	DLOG(cout << "enc uint32_t: " << value << "  pos: " << pos << endl;)
	space_for(sizeof(uint32_t));
    (*buff).at(pos)	 = (value >> 24) & 0x000000ff;
    (*buff).at(pos+1)= (value >> 16) & 0x000000ff;
    (*buff).at(pos+2)= (value >>  8) & 0x000000ff;
    (*buff).at(pos+3)= (value >>  0) & 0x000000ff;
	pos+=sizeof(uint32_t);
	return *this;
}

XdrCoding& XdrCoding::enc(int64_t value) {
	DLOG(cout << "enc int64_t: " << value << "  pos: " << pos << endl;)
	space_for(sizeof(int64_t));
	uint64_t tmp;
    if (value < 0) {
        value = 0 - value;
        tmp = 0 - static_cast<uint64_t>(value);
    } else 
		tmp = static_cast<uint64_t>(value);
    (*buff).at(pos)	 = (tmp >> 56) & 0x000000ff;
    (*buff).at(pos+1)= (tmp >> 48) & 0x000000ff;
    (*buff).at(pos+2)= (tmp >> 40) & 0x000000ff;
    (*buff).at(pos+3)= (tmp >> 32) & 0x000000ff;
    (*buff).at(pos+4)= (tmp >> 24) & 0x000000ff;
    (*buff).at(pos+5)= (tmp >> 16) & 0x000000ff;
    (*buff).at(pos+6)= (tmp >>  8) & 0x000000ff;
    (*buff).at(pos+7)= (tmp >>  0) & 0x000000ff;
	pos+=sizeof(int64_t);
	return *this;
}

XdrCoding& XdrCoding::enc(uint64_t value) {
	DLOG(cout << "enc uint64_t: " << value << "  pos: " << pos << endl;)
	space_for(sizeof(uint64_t));
    (*buff).at(pos)	 = (value >> 56) & 0x000000ff;
    (*buff).at(pos+1)= (value >> 48) & 0x000000ff;
    (*buff).at(pos+2)= (value >> 40) & 0x000000ff;
    (*buff).at(pos+3)= (value >> 32) & 0x000000ff;
    (*buff).at(pos+4)= (value >> 24) & 0x000000ff;
    (*buff).at(pos+5)= (value >> 16) & 0x000000ff;
    (*buff).at(pos+6)= (value >>  8) & 0x000000ff;
    (*buff).at(pos+7)= (value >>  0) & 0x000000ff;
	pos+=sizeof(uint64_t);
	return *this;
}

XdrCoding& XdrCoding::dec(int32_t* value) {
	ck_space_avl(sizeof(int32_t));
    uint32_t tmp;
    if ((*buff)[pos] & 0x80) {
        /* negative number */
#ifdef DEBUG		
        tmp =  (*buff).at(pos);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+1);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+2);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+3);
#else
        tmp =  (*buff)[pos];
        tmp = tmp << 8;
        tmp += (*buff)[pos+1];
        tmp = tmp << 8;
        tmp += (*buff)[pos+2];
        tmp = tmp << 8;
        tmp += (*buff)[pos+3];
#endif		
        tmp = 0 - tmp;
        *value = 0 - (int32_t)tmp;
    }
    else {
        /* positive number */
#ifdef DEBUG		
        *value =  (*buff).at(pos);
        *value = *value << 8;
        *value += (*buff).at(pos+1);
        *value = *value << 8;
        *value += (*buff).at(pos+2);
        *value = *value << 8;
        *value += (*buff).at(pos+3);
#else
        *value =  (*buff)[pos];
        *value = *value << 8;
        *value += (*buff)[pos+1];
        *value = *value << 8;
        *value += (*buff)[pos+2];
        *value = *value << 8;
        *value += (*buff)[pos+3];
#endif		
    }
	pos+=sizeof(int32_t);
	DLOG(cout << "dec: " << *value << "  pos: " << pos << endl;)
	return *this;
}

XdrCoding& XdrCoding::dec(int64_t* value) {
	ck_space_avl(sizeof(int64_t));
    uint64_t tmp;
    if ((*buff)[pos] & 0x80) {
        /* negative number */
        tmp =  (*buff).at(pos);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+1);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+2);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+3);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+4);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+5);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+6);
        tmp = tmp << 8;
        tmp += (*buff).at(pos+7);
        tmp = 0 - tmp;
        *value = 0 - (int64_t)tmp;
    }
    else {
        /* positive number */
        *value =  (*buff).at(pos);
        *value = *value << 8;
        *value += (*buff).at(pos+1);
        *value = *value << 8;
        *value += (*buff).at(pos+2);
        *value = *value << 8;
        *value += (*buff).at(pos+3);
        *value = *value << 8;
        *value += (*buff).at(pos+4);
        *value = *value << 8;
        *value += (*buff).at(pos+5);
        *value = *value << 8;
        *value += (*buff).at(pos+6);
        *value = *value << 8;
        *value += (*buff).at(pos+7);
    }
	pos+=sizeof(int64_t);
	DLOG(cout << "dec int64_t: " << *value << "  pos: " << pos << endl;)
	return *this;
}

XdrCoding& XdrCoding::dec(uint32_t* value) {
	ck_space_avl(sizeof(uint32_t));
    *value =  (*buff).at(pos);
    *value = *value << 8;
    *value += (*buff).at(pos+1);
    *value = *value << 8;
    *value += (*buff).at(pos+2);
    *value = *value << 8;
    *value += (*buff).at(pos+3);
	pos+=sizeof(uint32_t);
	DLOG(cout << "dec uint32_t: " << *value << "  pos: " << pos << endl;)
	return *this;
}

XdrCoding& XdrCoding::dec(uint64_t* value) {
	ck_space_avl(sizeof(uint64_t));
    *value =  (*buff).at(pos);
    *value = *value << 8;
    *value += (*buff).at(pos+1);
    *value = *value << 8;
    *value += (*buff).at(pos+2);
    *value = *value << 8;
    *value += (*buff).at(pos+3);
    *value = *value << 8;
    *value += (*buff).at(pos+4);
    *value = *value << 8;
    *value += (*buff).at(pos+5);
    *value = *value << 8;
    *value += (*buff).at(pos+6);
    *value = *value << 8;
    *value += (*buff).at(pos+7);
	pos+=sizeof(uint64_t);
	DLOG(cout << "dec uint64_t: " << *value << "  pos: " << pos << endl;)
	return *this;
}

void XdrCoding::float2myfloat(myfloat_t* myfloat, float num) {
    size_t i;
    float  tmp;
	DLOG(cout << "f2myf: " << num;)
    /* Handle zero as a special case. */
    if (num == 0.0) {
		myfloat->sign     = 0;
		myfloat->fraction = 0;
		myfloat->exponent = -127;
		return;
	}

    /* Determine the sign of the number. */
    if (num < 0.0) {
		myfloat->sign = 1;
		num = 0.0 - num;
	} else
		myfloat->sign = 0;

    /* Canonify the number before we convert it. */
    myfloat->exponent = 0;
    while (num < 1.0)
	{
		num *= 2.0;
		--myfloat->exponent;
	}

    /* Find the exponent. */
    for (i = 0, tmp = 1.0; i <= 128; ++i, tmp *= 2.0) {
		if (tmp * 2.0 > num)
		    break;
	}
    if (i <= 128) {
		num = num / tmp - 1.0;
		myfloat->exponent += i;
	} else
		throw(overflow_error("XdrCoding float exponent overflow"));
    /* Calculate the fraction part. */
    for (myfloat->fraction = 0, i = 0; i < 23; ++i) {
		myfloat->fraction *= 2;
		if (num >= 1.0 / 2.0) {
			myfloat->fraction += 1;
			num = num * 2.0 - 1.0;
		} else
			num *= 2.0;
	}
	DLOG(cout << " sign: " <<(int) myfloat->sign << " mantissa: " << myfloat->fraction << " exp: " << (int) myfloat->exponent << endl;)
}

void XdrCoding::double2mydouble(mydouble_t* mydouble, double num) {
	size_t i;
    double  tmp;

    /* Handle zero as a special case. */
    if (num == 0.0) {
		mydouble->sign     = 0;
		mydouble->fraction = 0;
		mydouble->exponent = -1023;
		return;
	}

    /* Determine the sign of the number. */
    if (num < 0.0) {
		mydouble->sign = 1;
		num = 0.0 - num;
	} else
		mydouble->sign = 0;

    /* Canonify the number before we convert it. */
    mydouble->exponent = 0;
    while (num < 1.0) {
		num *= 2.0;
		--mydouble->exponent;
	}

    /* Find the exponent. */
    for (i = 0, tmp = 1.0; i <= 1024; ++i, tmp *= 2.0) {
		if (tmp * 2.0 > num)
		    break;
	}
    if (i <= 1024) {
		num = num / tmp - 1.0;
		mydouble->exponent += i;
	} else
		throw(overflow_error("XdrCoding float exponent overflow"));

    /* Calculate the fraction part. */
    for (mydouble->fraction = 0, i = 0; i < 52; ++i) {
		mydouble->fraction *= 2;
		if (num >= 1.0 / 2.0) {
			mydouble->fraction += 1;
			num = num * 2.0 - 1.0;
		} else
			num *= 2.0;
	}
}

XdrCoding& XdrCoding::enc(float in) {
	DLOG(cout << "enc float: " << in << "  pos: " << pos << endl;)
	space_for(4);
	myfloat_t   value = { 0, 0, 0 };
	uint8_t tmp;

	/* Get value and format it into the structure. */
	float2myfloat(&value, in);
	for(size_t i = pos; i < pos+4; ++i)
		(*buff).at(i) = 0;

	if (value.sign == 1)
		(*buff).at(pos) |= 0x80;
	tmp = value.exponent + 127;
	(*buff).at(pos) |= tmp >> 1;
	(*buff).at(pos+1) |= (tmp & 0x01) << 7;
	(*buff).at(pos+1) |= (uint8_t)((value.fraction & 0x7fffff) >> 16);
	(*buff).at(pos+2) |= (uint8_t)((value.fraction & 0x00ffff) >>  8);
	(*buff).at(pos+3) |= (uint8_t)((value.fraction & 0x0000ff) >>  0);
	pos+=4;
	return *this;
}

XdrCoding& XdrCoding::dec(float* value) {
	uint32_t fraction;
	uint8_t  exponent;
	size_t       i;
	char         sign;
	
	ck_space_avl(4);
	*value = 0.0;

	sign		=  ((*buff).at(pos+0) & 0x80) >> 7;
	exponent	=  ((*buff).at(pos+0) & 0x7f) << 1;
	exponent	+= ((*buff).at(pos+1) & 0x80) >> 7;
	fraction	=  ((*buff).at(pos+1) & 0x7fffff) << 16;
	fraction	+= (*buff).at(pos+2) << 8;
	fraction	+= (*buff).at(pos+3);

	if (fraction == 0 && exponent == 0)
		return *this;

	for (i = 23; i > 0; --i) {
		if ((fraction & 0x01) == 1)
			*value += 1.0;
		*value /= 2.0;
		fraction /= 2;
	}
	*value += 1.0;

	if (exponent > 127) {
		for (exponent -= 127; exponent > 0; --exponent)
			*value *= 2.0;
	} else {
		for (exponent = 127 - exponent; exponent > 0; --exponent)
			*value /= 2.0;
	}

	if (sign == 1)
		*value = 0.0 - *value;
	pos+=4;
	return *this;	
}

XdrCoding& XdrCoding::enc(double in) {
	DLOG(cout << "enc double: " << in << "  pos: " << pos << endl;)
	space_for(8);
	mydouble_t   value = { 0, 0, 0 };
	uint16_t tmp;

	/* Get value and format it into the structure. */
	double2mydouble(&value, in);
	for(size_t i = pos; i < pos+8; ++i)
		(*buff).at(i) = 0;

	if (value.sign == 1)
		(*buff).at(pos) |= 0x80;

	tmp = value.exponent + 1023;
	(*buff).at(pos) |= (tmp >> 4) & 0x7f;
	(*buff).at(pos+1) |= (tmp & 0x0f) << 4;

	(*buff).at(pos+1) |= (uint8_t)((value.fraction & 0x0f000000000000LL) >> 48);
	(*buff).at(pos+2) |= (uint8_t)((value.fraction & 0x00ff0000000000LL) >> 40);
	(*buff).at(pos+3) |= (uint8_t)((value.fraction & 0x0000ff00000000LL) >> 32);
	(*buff).at(pos+4) |= (uint8_t)((value.fraction & 0x000000ff000000LL) >> 24);
	(*buff).at(pos+5) |= (uint8_t)((value.fraction & 0x00000000ff0000LL) >> 16);
	(*buff).at(pos+6) |= (uint8_t)((value.fraction & 0x0000000000ff00) >>  8);
	(*buff).at(pos+7) |= (uint8_t)((value.fraction & 0x000000000000ff) >>  0);
	pos+=8;
	return *this;
}

XdrCoding& XdrCoding::dec(double* value) {
	DLOG(cout << "dec double:  pos: " << pos << endl;)
    uint64_t fraction;
    uint16_t exponent;
    size_t       i;
    char         sign;

	ck_space_avl(8);

    *value = 0.0;

    sign = ((*buff).at(pos) & 0x80) >> 7;
    exponent  = ((*buff).at(pos) & 0x7f) << 4;
    exponent += ((*buff).at(pos+1) & 0xf0) >> 4;

    fraction  = (uint64_t)(((*buff).at(pos+1) & 0x0f)) << 48;
    fraction += (uint64_t)((*buff).at(pos+2)) << 40;
    fraction += (uint64_t)((*buff).at(pos+3)) << 32;
    fraction += (uint64_t)((*buff).at(pos+4)) << 24;
    fraction += (uint64_t)((*buff).at(pos+5)) << 16;
    fraction += (uint64_t)((*buff).at(pos+6)) <<  8;
    fraction += (uint64_t)((*buff).at(pos+7)) <<  0;

	pos+=8;

    if (fraction == 0 && exponent == 0)
		return *this;

    for (i = 52; i > 0; --i)
	{
		if ((fraction & 0x01) == 1)
			*value += 1.0;
		*value /= 2.0;
		fraction /= 2;
	}
    *value += 1.0;

    if (exponent > 1023) {
		for (exponent -= 1023; exponent > 0; --exponent)
		    *value *= 2.0;
	}
    else {
		for (exponent = 1023 - exponent; exponent > 0; --exponent)
			*value /= 2.0;
	}
    if (sign == 1)
		*value = 0.0 - *value;
	DLOG(cout << "dec double: " << *value << "  pos: " << pos << endl;)
	return *this;
}

/*XdrCoding& XdrCoding::enc(size_t size) {
	if( size > UINT32_MAX )
		throw(overflow_error("XdrCoding size_t to uint32 overflow"));
	uint32_t size32 = static_cast<uint32_t>(size);	
	enc(size32);
}

XdrCoding& XdrCoding::dec(size_t* size) {
	uint32_t size32;
	dec(&size32);
	*size = static_cast<size_t>(size32);
}*/	

XdrCoding& XdrCoding::enc(const string& str) {
	size_t size = str.size();	
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	space_for(sizeof(uint32_t) + size + pad);
	enc((long)size);
	// xdr mandates padding
	//space_for(size_on_buff);
	memcpy(&(*buff)[pos],str.data(), size);
	memset(&(*buff)[pos+size],0,pad);
	pos+=size_on_buff;
	return *this;
}

XdrCoding& XdrCoding::dec(string& str) {
	str.clear();
	//size_t size;	
	long size;	
	dec(&size);
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	ck_space_avl(size + pad);
	//str.resize(size);
	str.assign((char*)&(*buff)[pos],size);
	pos+=size_on_buff;
	return *this;
}


/*XdrCoding& XdrCoding::enc(const wstring& thestring)
{
	size_t size_on_buff = 0;
	space_for(sizeof(uint32_t));
	
	char *tempbuf = new char[5];

	size_t startpos = pos;

	wstring::const_iterator iter = thestring.begin();
	while (iter != thestring.end())
	{
		int index=0;
		encode_unicode_character(tempbuf, &index, *iter);
		
		size_on_buff += index;

		space_for(size_on_buff);
		memcpy(&(*buff)[pos], tempbuf, index);

		pos+=index;
		
		iter++;
	}
	
	size_t pad = (4 - (pos%4))%4;
	memset(&(*buff)[pos], 0, pad);
	
	size_t finishedpos = pos;
	
	pos = startpos;
	enc((uint32_t)size_on_buff);
	pos = finishedpos;
	
	delete tempbuf;

	return *this;
}

XdrCoding& XdrCoding::dec(wstring& thestring)
{
	uint32_t size;	
	dec(&size);

	for (int i = 0; i < size;)
	{
		char *tempbuf = new char[5];
		char *origtempbuf = tempbuf;
		int index = 0;
		while ((index < 5) && (index+5 < (*buff).size()))
		{
			tempbuf[index] = (*buff)[pos+index];
			index++;
		}

		Uint32 thechar = decode_next_unicode_character((const char **)&tempbuf);

		i+=tempbuf-origtempbuf;

		thestring+=(wchar_t)thechar;

		pos += tempbuf-origtempbuf;

		delete tempbuf;
	}

	size_t pad = (4 - (pos%4))%4;
	ck_space_avl(pos + pad);
	pos += pad;

	return *this;
}*/

XdrCoding& XdrCoding::enc(const void* src, size_t size) {
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	space_for(sizeof(uint32_t) + size + pad);
	enc((long)size);
	memcpy(&(*buff)[pos],src, size);
	memset(&(*buff)[pos+size],0,pad);
	pos+=size_on_buff;
	return *this;
}

XdrCoding& XdrCoding::enc(const vector<uint8_t>& vec) {
	DLOG(cout<< "spec" << endl;)
	size_t size = vec.size();
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	space_for(sizeof(uint32_t) + size + pad);
	enc((long)size);
	memcpy(&(*buff)[pos],&vec[0],size);
	memset(&(*buff)[pos+size],0,pad);
	pos+=size_on_buff;
	return *this;
}

XdrCoding& XdrCoding::dec(vector<uint8_t>& vec) {
	DLOG(cout<< "spec" << endl;)
	vec.clear();
	//size_t size;
	long size;
	dec(&size);
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	ck_space_avl(size + pad);
	vec.resize(size);
	memcpy(&vec[0],&(*buff)[pos],size);
	pos+=size_on_buff;
	return *this;
}


void XdrCoding::putNameValues(ConnectionOptions *options)
{
	if (options->size() == 0)
	{
		enc((int32_t)0);
	}
	else
	{
		throw std::logic_error("XdrCoding::putNameValues is not implemented");
	}
}

void XdrCoding::getNameValues(ConnectionOptions *options)
{
	int32_t size;
	dec(&size);
	if (size != 0)
	{
		throw std::logic_error("XdrCoding::getNameValues is not implemented");
	}
}

XdrCoding& XdrCoding::appendbytes(char *bytes, size_t bytecount)
{
	space_for(bytecount);

	memcpy(&(*buff)[pos], bytes, bytecount);

	pos+=bytecount;

	/*int i = 0;
	while (i < bytecount)
	{
		printf("GOT: |%x|\n", (*buff)[i]);
		i++;
	}*/

	return *this;
}

unsigned char *XdrCoding::getbytes()
{
	unsigned char *themessage = new unsigned char[buff->size()];
	std::vector<uint8_t>::const_iterator iter = buff->begin();
	int i = 0;
	while (iter != buff->end())
	{
		themessage[i] = *iter;
		i++; iter++;
	}
	return themessage;
}
	
size_t XdrCoding::size()
{
	return buff->size();
}
