#ifndef NAMED_VALUES_INT_H_
#define NAMED_VALUES_INT_H_

#include "byte_buffer.h"

bool named_values_write (ByteBuffer *buffer, NamedValues *values, 
                         ElvinError *error);

bool named_values_read (ByteBuffer *buffer, NamedValues *values, 
                        ElvinError *error);

#endif /*NAMED_VALUES_INT_H_*/
