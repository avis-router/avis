#ifndef NAMED_VALUES_INT_H_
#define NAMED_VALUES_INT_H_

#include "byte_buffer.h"

bool attributes_write (ByteBuffer *buffer, Attributes *attributes, 
                         ElvinError *error);

bool attributes_read (ByteBuffer *buffer, Attributes *attributes, 
                        ElvinError *error);

#endif /*NAMED_VALUES_INT_H_*/
