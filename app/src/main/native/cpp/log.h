#ifndef LOG_H
#define LOG_H

#define LEVEL 2
#ifdef ANDROID_RUNTIME

#include <android/log.h>

#else
#include "stdio.h"
#endif

typedef enum LogPriority {
    LOG_UNKNOWN = 0,
    LOG_DEFAULT, /* only for SetMinPriority() */
    LOG_VERBOSE,
    LOG_DEBUG,
    LOG_INFO,
    LOG_WARN,
    LOG_ERROR,
    LOG_FATAL,
    LOG_SILENT, /* only for SetMinPriority(); must be last */
} LogPriority;


int log_print(int prio, const char *tag, const char *fmt, ...);

#ifdef ANDROID_RUNTIME

int log_print(int prio, const char *tag, const char *fmt, ...) {

    va_list myargs;
    va_start(myargs, fmt);
    int ret = __android_log_vprint(prio, tag, fmt, myargs);
    va_end(myargs);
    return ret;
}

#else
int log_print(int prio, const char* tag, const char* fmt, ...){
    if (prio >= LEVEL){
        printf("%s", tag);
        va_list myargs;
        va_start(myargs, fmt);
        int ret = vprintf(fmt, myargs);
        va_end(myargs);
        return ret;
    }
    return 0;
}

#endif


#endif