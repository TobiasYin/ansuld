//
// Created by 尹瑞涛 on 2021/4/15.
//

#ifndef JNI_HELPER
#define JNI_HELPER

#include <jni.h>
#include "malloc.h"


// Java string to char*, return heap malloc mem, remember to free
char *jstringToChar(JNIEnv *env, jstring jstr) {
    int length = (env)->GetStringLength(jstr);
    const jchar *jcstr = (env)->GetStringChars(jstr, 0);
    char *rtn = (char *) malloc(length + 1);
    for (int i = 0; i < length; i++) {
        rtn[i] = (char) (jcstr[i] & 0x00FF);
    }
    (env)->ReleaseStringChars(jstr, jcstr);
    rtn[length] = 0;
    return rtn;
}

#endif //JNI_HELPER
