#ifndef ERRORS_H_
#define ERRORS_H_

#include <avis/stdtypes.h>

/**
 * Error reporting information for the Avis client library. Functions
 * in the library that may fail will require a pointer to an
 * ElvinError instance as their last parameter, which will be loaded
 * with an error code and message if the the function or any sub
 * function fails. Functions which would otherwise return void will
 * often also return true/false as a convenience.
 * 
 * Example:
 * <pre>
 * ElvinError error = elvin_error_create ();
 *  
 * function_that_may_fail (arg1, arg2, ..., &error);
 *
 * if (elvin_error_occurred (&error))
 *   elvin_perror ("error", &error);
 *
 * elvin_error_free (&error);
 * </pre>
 * 
 * @see elvin_error_set()
 * @see elvin_error_ok()
 * @see on_error_return_false()
 * @see on_error_return()
 */
typedef struct
{
  int    code;
  char * message;
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
#define ELVIN_ERROR_NACK (ELVIN_ERROR_BASE + 7)

/**
 * Initialise an error. This should be done to initialise a new error
 * instance. Use elvin_error_reset() to reset an existing instance,
 * and elvin_error_free() to release any resources allocated before
 * disposing.
 */
#define elvin_error_create() {ELVIN_ERROR_NONE, NULL}

/**
 * Free any resources allocated to an error instance and reset the
 * error code.  The error instance may be reused after this call.
 * 
 * @see elvin_error_reset()
 */
void elvin_error_free (ElvinError *error);

/** 
 * Reset the error info back to OK state. Synonymn for elvin_error_free().
 */
#define elvin_error_reset(error) (elvin_error_free (error))

/**
 * Macro statement to return false if an error is set in the "error"
 * variable inside the current scope.
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

/**
 * Convert the h_errno error code generated by gethostbyname () etc to
 * an error code that can be used with elvin_error_set().
 */
#define HOST_TO_ELVIN_ERROR(code) (ELVIN_HOST_ERROR_BASE + (code))

/**
 * Translate a system errno error code into a code that can be used with
 * elvin_error_set().
 */
#define ERRNO_TO_ELVIN_ERROR(code) (code)

/**
 * Like perror () but taking error info from an ElvinError instance.
 */
void elvin_perror (const char *tag, ElvinError *error);

/**
 * Load an error status from the system's "errno" error variable and
 * strerror () function.
 * 
 * @param error The error to affect.
 * 
 * @see elvin_error_set()
 */
bool elvin_error_from_errno (ElvinError *error);

/**
 * Signal an error has occurred. If an error is already set, this has
 * no effect (see elvin_error_reset() if you want to override any
 * existing error status).
 * 
 * @param error The error to target.
 * @param code The error code. One of the ELVIN_ERROR_* defines or 
 * HOST_TO_ELVIN_ERROR().
 * @param message The message, possibly including printf-style format 
 * placeholders.
 * @param ... The rest of the arguments if message contains format 
 * placeholders.
 * 
 * @return Returns false as a convenience so that this can be used in
 * return statements indicating an error.
 * 
 * @see elvin_error_from_errno()
 * @see elvin_error_free()
 */
bool elvin_error_set (ElvinError *error, int code, const char *message, ...);

bool elvin_error_assert (ElvinError *error, bool condition, 
                         int code, const char *message);

/** 
 * True if no error has occurred. 
 */
#define elvin_error_ok(error) ((error)->code == ELVIN_ERROR_NONE)

/** 
 * True if an error has occurred.
 */
#define elvin_error_occurred(error) (!elvin_error_ok (error))

#endif /*ERRORS_H_*/
