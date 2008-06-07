#ifndef LOG_H_
#define LOG_H_

#define ELVIN_LOG_LEVEL 1

#define LOG_LEVEL_DIAGNOSTIC 5
#define LOG_LEVEL_TRACE 6

#define LOGGING(loglevel) (ELVIN_LOG_LEVEL >= loglevel)

#if LOGGING (LOG_LEVEL_TRACE)
  #define TRACE(message) (elvin_log (LOG_LEVEL_TRACE, message))
#else
  #define TRACE(message)
#endif

#if LOGGING (LOG_LEVEL_DIAGNOSTIC)
  #define DIAGNOSTIC(message) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message)) 
  #define DIAGNOSTIC1(message, arg1) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message, arg1))
  #define DIAGNOSTIC2(message, arg1, arg2) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message, arg1, arg2))
#else
  #define DIAGNOSTIC(message) 
  #define DIAGNOSTIC1(message, arg1)
  #define DIAGNOSTIC2(message, arg1, arg2)
#endif

void elvin_log (int level, const char *message, ...);

#endif /*LOG_H_*/
