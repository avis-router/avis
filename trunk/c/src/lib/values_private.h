#ifndef VALUES_PRIVATE_H_
#define VALUES_PRIVATE_H_

#include <avis/errors.h>
#include <avis/values.h>

#include "byte_buffer.h"

bool value_read (ByteBuffer *buffer, Value *value, ElvinError *error);

bool value_write (ByteBuffer *buffer, Value *value, ElvinError *error);

#endif /*VALUES_PRIVATE_H_*/
