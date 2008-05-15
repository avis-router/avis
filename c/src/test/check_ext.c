#include <check.h>

#include <elvin/errors.h>

#include "check_ext.h"

void fail_on_error (Elvin_Error *error)
{
  fail_unless (error->code == ELVIN_ERROR_NONE, error->message);
}
