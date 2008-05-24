#ifndef LOG_H_
#define LOG_H_

#define ELVIN_DEBUG_LEVEL 3

#define LOG_LEVEL_DIAGNOSTIC 5
#define LOG_LEVEL_TRACE 6

#if ELVIN_DEBUG_LEVEL >= LOG_LEVEL_TRACE
  #define TRACE(message) (elvin_log (LOG_LEVEL_TRACE, message))
#else
  #define TRACE(message)
#endif

#if ELVIN_DEBUG_LEVEL >= LOG_LEVEL_DIAGNOSTIC
  #define DIAGNOSTIC(message) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message)) 
  #define DIAGNOSTIC1(message, arg1) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message, arg1))
#else
  #define DIAGNOSTIC(message) 
  #define DIAGNOSTIC1(message, arg1)
#endif

void elvin_log (int level, const char *message, ...);

#endif /*LOG_H_*/
