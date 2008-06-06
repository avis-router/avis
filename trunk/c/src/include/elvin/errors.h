#ifndef ERRORS_H_
#define ERRORS_H_

#include <elvin/stdtypes.h>

/**
 * Error reporting information for functions that may fail.
 * The Avis Client Library uses this as a sort of simple
 * exception mechanism to report errors encountered when calling subroutines.
 * Functions that can fail on error take a pointer to an error instance as 
 * their last parameter, and the error will be loaded with an error code and
 * message if the the function (or a sub function) fails. Functions which
 * would otherwise be void will often return true/false also as a convenience.
 * 
 * @see elvin_error_ok()
 * @see on_error_return_false()
 * @see on_error_return()
 */
typedef struct
{
  int code;
  const char *message;
} ElvinError;

#define ELVIN_HOST_ERROR_BASE 10000
#define ELVIN_ERROR_BASE 20000

#define ELVIN_ERROR_NONE ELVIN_ERROR_BASE
#define ELVIN_ERROR_INTERNAL (ELVIN_ERROR_BASE + 1)
#define ELVIN_ERROR_PROTOCOL (ELVIN_ERROR_BASE + 2)
#define ELVIN_ERROR_CONNECTION_CLOSED (ELVIN_ERROR_BASE + 3)
#define ELVIN_ERROR_INVALID_URI (ELVIN_ERROR_BASE + 4)
#define ELVIN_ERROR_SYNTAX (ELVIN_ERROR_BASE + 5)
#define ELVIN_ERROR_TRIVIAL_EXPRESSION (ELVIN_ERROR_BASE + 6)

#define elvin_error_create() {ELVIN_ERROR_NONE, NULL}

/* TODO */
#define elvin_error_destroy(error)

/**
 * Macro statement to return false if an error is set in the "error" variable
 * inside the current scope.
 * 
 * @param stat The statement to execute before the test.
 * 
 * See also on_error_return().
 */
#define on_error_return_false(stat) on_error_return (stat, false)

/**
 * Macro statement to return a given value if an error is set in the 
 * "error" variable inside the current scope.
 * 
 * @param stat The statement to execute before the test.
 * @param retval The value to return on error.
 * 
 * See also on_error_return().
 */
#define on_error_return(stat, retval) \
  {stat; if (elvin_error_occurred (error)) return (retval);}

/*
 * Convert the h_errno error code generated by gethostbyname () etc to
 * an error code. 
 */
#define HOST_TO_ELVIN_ERROR(code) (ELVIN_HOST_ERROR_BASE + code)
#define ERRNO_TO_ELVIN_ERROR(code) (code)

void elvin_perror (const char *tag, ElvinError *error);
bool elvin_error_from_errno (ElvinError *error);
bool elvin_error_set (ElvinError *error, int code, const char *message);
bool elvin_error_assert (ElvinError *error, bool condition, 
                         int code, const char *message);

/** True if no error has occurred. */
#define elvin_error_ok(error) ((error)->code == ELVIN_ERROR_NONE)

/** True if an error has occurred. */
#define elvin_error_occurred(error) (!elvin_error_ok (error))

/** Reset the error info back to OK state. */
#define elvin_error_reset(error) \
  ((error)->code = ELVIN_ERROR_NONE, (error)->message = NULL)

#endif /*ERRORS_H_*/
