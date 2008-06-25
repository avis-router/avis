/* Copyright (c) 2006, Pedro Larroy Tovar 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
#ifndef XdrCoding_hh
#define XdrCoding_hh
//#include <stdint.h>
#include <vector>
#include <stdexcept>

#include "ConnectionOptions.h"

typedef signed char 			int8_t;
typedef unsigned char 			uint8_t;
typedef signed int 				int16_t;
typedef unsigned int 			uint16_t;
typedef signed long int 		int32_t;
typedef unsigned long int 		uint32_t;
typedef signed long long int 	int64_t;
typedef unsigned long long int 	uint64_t;

//#define VERBOSELOG

#ifdef VERBOSELOG
#define DLOG(x) x
#else
#define DLOG(x)
#endif

#define DEBUG

using namespace std;

class XdrCoding {
	public:
	
		XdrCoding();
		/**
		 * Marshall data into buff starting at 0
		 */
		XdrCoding(vector<uint8_t>*buff);
		/**
		 * Marshall data into buff starting at pos
		 */
		XdrCoding(vector<uint8_t>*buff, size_t pos);
		virtual ~XdrCoding();

		XdrCoding& resetpos();
		XdrCoding& clear() { buff->clear(); return *this; };
		size_t getpos() { return pos; };
		void setpos(size_t thepos) { space_for(thepos); pos = thepos; };
		XdrCoding& setbuff(vector<uint8_t>*thebuff);
		XdrCoding& setbuff(vector<uint8_t>*buff, size_t pos);
		vector<uint8_t>* getbuff();

		unsigned char *getbytes();
		size_t size();

		/**
		 * Make space for size_t after pos
		 */
		XdrCoding& space_for(size_t);
		/**
		 * Check if there's space for size_t after pos
		 */
		void ck_space_avl(size_t);

		XdrCoding& appendbytes(char *bytes, size_t bytecount);

		XdrCoding& enc(int32_t);
		XdrCoding& dec(int32_t*);

		XdrCoding& enc(uint32_t);
		XdrCoding& dec(uint32_t*);

		XdrCoding& enc(int64_t);
		XdrCoding& dec(int64_t*);

		XdrCoding& enc(uint64_t);
		XdrCoding& dec(uint64_t*);
		/**
		 * IEEE754 Float
		 * http://steve.hollasch.net/cgindex/coding/ieeefloat.html
		 */
		XdrCoding& enc(float);
		XdrCoding& dec(float*);

		XdrCoding& enc(double);
		XdrCoding& dec(double*);

		XdrCoding& enc(const string&);
		XdrCoding& dec(string&);

		/*XdrCoding& enc(const wstring&);
		XdrCoding& dec(wstring&);*/

		template<typename T> XdrCoding& enc(const vector<T>& vec);
		template<typename T> XdrCoding& dec(vector<T>& vec);

		XdrCoding& enc(const vector<uint8_t>& vec);
		XdrCoding& dec(vector<uint8_t>& vec); 

		XdrCoding& enc(const void* src, size_t size); 

		XdrCoding& operator<<(int32_t v) { return enc(v); };
		XdrCoding& operator>>(int32_t& v) { return dec(&v); };

		XdrCoding& operator<<(int64_t v) { return enc(v); };
		XdrCoding& operator>>(int64_t& v) { return dec(&v); };

		XdrCoding& operator<<(uint32_t v) { return enc(v); };
		XdrCoding& operator>>(uint32_t& v) { return dec(&v); };

		XdrCoding& operator<<(uint64_t v) { return enc(v); };
		XdrCoding& operator>>(uint64_t& v) { return dec(&v); };

		XdrCoding& operator<<(float v) { return enc(v); };
		XdrCoding& operator>>(float& v) { return dec(&v); };

		XdrCoding& operator<<(double v) { return enc(v); };
		XdrCoding& operator>>(double& v) { return dec(&v); };

		XdrCoding& operator<<(const string& v) { return enc(v); };
		XdrCoding& operator>>(string& v) { return dec(v); };

		template<typename T> XdrCoding& operator<<(const vector<T>& v) { return enc(v); };
		template<typename T> XdrCoding& operator>>(vector<T>& v) { return dec(v); };

		typedef struct {
			uint8_t  sign;		/* :1 */
			uint32_t fraction;	/* :23 */
			int8_t   exponent;	/* :8 */
		} myfloat_t;
		typedef struct {
			uint8_t  sign;		/* :1 */
			uint64_t fraction;	/* :52 */
			int16_t  exponent;	/* :11 */
		} mydouble_t;

		void float2myfloat(myfloat_t* myfloat, float num);
		void double2mydouble(mydouble_t* mydouble, double num);

		void putNameValues(ConnectionOptions *options);
		void getNameValues(ConnectionOptions *options);

	private:
		vector<uint8_t>* buff;
		bool OWNED;
		size_t	pos;
};

template<typename T> XdrCoding& XdrCoding::enc(const vector<T>& vec) {
	size_t size = vec.size();	
	enc(size);
	for(typename vector<T>::const_iterator i = vec.begin(); i != vec.end(); ++i) {
		enc(*i);	
	}
	return *this;
}

template<typename T> XdrCoding& XdrCoding::dec(vector<T>& vec) {
	vec.clear();
	size_t size;	
	dec(&size);
	vec.resize(size);
	for(size_t i = 0; i < size; ++i) {
		dec(&vec[i]);	
	}
	return *this;
}

/*template<> XdrCoding& XdrCoding::enc(const vector<uint8_t>& vec) {
	size_t size = vec.size();
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	space_for(sizeof(uint32_t) + size + pad);
	enc(size);
	memcpy(&(*buff)[pos],&vec[0],size);
	memset(&(*buff)[pos+size],0,pad);
	pos+=size_on_buff;
	return *this;
}

template<> XdrCoding& XdrCoding::dec(vector<uint8_t>& vec) {
	vec.clear();
	size_t size;	
	dec(&size);
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	ck_space_avl(size + pad);
	vec.resize(size);
	memcpy(&vec[0],&(*buff)[pos],size);
	pos+=size_on_buff;
	return *this;
}*/
/*template<typename T> XdrCoding& XdrCoding::enc<uint8_t>(const vector<uint8_t>& vec) {
	size_t size = vec.size();
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	space_for(sizeof(uint32_t) + size + pad);
	enc(size);
	memcpy(&(*buff)[pos],&vec[0],size);
	memset(&(*buff)[pos+size],0,pad);
	pos+=size_on_buff;
	return *this;
}

template<typename T> XdrCoding& XdrCoding::dec<uint8_t>(vector<uint8_t>& vec) {
	vec.clear();
	size_t size;	
	dec(&size);
	size_t pad = (4 - (size%4))%4;
	size_t size_on_buff = size + pad;
	ck_space_avl(size + pad);
	vec.resize(size);
	memcpy(&vec[0],&(*buff)[pos],size);
	pos+=size_on_buff;
	return *this;
}*/



#endif
