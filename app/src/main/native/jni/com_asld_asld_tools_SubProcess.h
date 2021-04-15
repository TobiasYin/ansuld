//
// Created by 尹瑞涛 on 2021/4/15.
//
#include "jni_helpers.h"

#ifndef ANSULD_COM_ASLD_ASLD_TOOLS_SUBPROCESS_H
#define ANSULD_COM_ASLD_ASLD_TOOLS_SUBPROCESS_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_asld_asld_tools_SubProcess
 * Method:    createSubProcess
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_com_asld_asld_tools_SubProcess_createSubProcess
  (JNIEnv *, jobject, jstring, jobjectArray);
#ifdef __cplusplus
}
#endif

#endif //ANSULD_COM_ASLD_ASLD_TOOLS_SUBPROCESS_H
