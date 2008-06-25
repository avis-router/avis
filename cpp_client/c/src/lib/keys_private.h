#ifndef KEYS_PRIVATE_H_
#define KEYS_PRIVATE_H_

#include <avis/errors.h>
#include <avis/keys.h>

#include "byte_buffer.h"

bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error);

bool elvin_keys_write (ByteBuffer *buffer, Keys *keys, ElvinError *error);

#endif /*KEYS_PRIVATE_H_*/
