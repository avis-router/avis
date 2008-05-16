#ifndef CHECK_EXT_H_
#define CHECK_EXT_H_

#include <elvin/errors.h>

#define fail_on_error(error) \
  (fail_unless ((error)->code == ELVIN_ERROR_NONE, \
      "Elvin error: %s", (error)->message))

#define fail_unless_error_code(error,expected_code) \
  (fail_unless ((error)->code == (expected_code), \
      "Expected elvin error"), elvin_error_reset (error))

#endif /*CHECK_EXT_H_*/
