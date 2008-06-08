#ifndef ATTRIBUTES_PRIVATE_H_
#define ATTRIBUTES_PRIVATE_H_

#include "byte_buffer.h"

bool attributes_write (ByteBuffer *buffer, Attributes *attributes, 
                       ElvinError *error);

bool attributes_read (ByteBuffer *buffer, Attributes *attributes, 
                      ElvinError *error);

#endif /*ATTRIBUTES_PRIVATE_H_*/
