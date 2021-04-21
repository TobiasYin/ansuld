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
JNIEXPORT jint JNICALL Java_com_asld_asld_tools_ProcessUtil_createSubProcess
  (JNIEnv *, jobject, jstring, jobjectArray);
#ifdef __cplusplus
}
#endif

extern "C"
JNIEXPORT void JNICALL
Java_com_asld_asld_tools_ProcessUtil_createSubProcessFds(JNIEnv *env, jobject thiz, jstring path,
                                                         jobjectArray argv, jobject fds);


extern "C"
JNIEXPORT jint JNICALL
Java_com_asld_asld_tools_ProcessUtil_waitPid(JNIEnv *env, jobject thiz, jint pid);

extern "C"
JNIEXPORT jint JNICALL
Java_com_asld_asld_tools_ProcessUtil_closeFd(JNIEnv *env, jobject thiz, jint fd);


extern "C"
JNIEXPORT void JNICALL
Java_com_asld_asld_tools_ProcessUtil_createSubProcessEnv(JNIEnv *env, jobject thiz, jstring path,
                                                         jobjectArray argv, jobjectArray env_1,
                                                         jobject fds);

extern "C"
JNIEXPORT void JNICALL
Java_com_asld_asld_tools_ProcessUtil_createProcess(JNIEnv *env, jobject thiz, jobject args,
                                                   jobject fds);

#endif //ANSULD_COM_ASLD_ASLD_TOOLS_SUBPROCESS_H