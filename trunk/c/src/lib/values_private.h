#ifndef VALUES_PRIVATE_H_
#define VALUES_PRIVATE_H_

#include <elvin/errors.h>
#include <elvin/values.h>

#include "byte_buffer.h"

Value *value_read (ByteBuffer *buffer, ElvinError *error);

bool value_write (ByteBuffer *buffer, Value *value, ElvinError *error);

#endif /*VALUES_PRIVATE_H_*/
