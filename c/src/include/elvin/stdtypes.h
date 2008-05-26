#ifndef ELVIN_STDTYPES_H
#define ELVIN_STDTYPES_H

#ifdef HAVE_STDBOOL_H
#include <stdbool.h>
#else
typedef signed char int8_t;
typedef signed short int16_t;
typedef signed int int32_t;
typedef signed long int64_t;

typedef unsigned char uint8_t;
typedef unsigned short uint16_t;
typedef unsigned int uint32_t;
typedef unsigned long uint64_t;
#endif

#ifdef HAVE_STDWINTBOOL_H
#include <stdint.h>
#else
#define bool int
#define true 1
#define false 0
#endif

#endif //ELVIN_STDTYPES_H
