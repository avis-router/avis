/** \file
 * Ensure int8_t, int32_t, int64_t, etc are defined. 
 */
#ifndef ELVIN_STDTYPES_H
#define ELVIN_STDTYPES_H

typedef double real64_t;
  
#include "avis_client_config.h"

#ifdef HAVE_STDINT_H
  #include <stdint.h>
#else
  typedef signed char int8_t;
  typedef signed short int16_t;
  typedef signed int int32_t;
  typedef signed long long int64_t;

  typedef unsigned char uint8_t;
  typedef unsigned short uint16_t;
  typedef unsigned int uint32_t;
  typedef unsigned long long uint64_t;
#endif

#ifdef HAVE_STDBOOL_H
  #include <stdbool.h>
#else
  #define bool int
  #define true 1
  #define false 0
#endif

#endif /* ELVIN_STDTYPES_H */
