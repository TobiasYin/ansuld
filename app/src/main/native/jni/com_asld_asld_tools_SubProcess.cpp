//
// Created by 尹瑞涛 on 2021/4/15.
//

#include "com_asld_asld_tools_SubProcess.h"
#include "../cpp/create_sub_process.h"
#include <android/log.h>


extern "C" JNIEXPORT jint JNICALL Java_com_asld_asld_tools_SubProcess_createSubProcess
        (JNIEnv *env, jobject that, jstring path, jobjectArray argv){

    char *p = jstringToChar(env, path);
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "path: %s", p);
    int array_len = env->GetArrayLength(argv);
    char ** args = new char*[array_len + 2];
    args[0] = p;
    for (int i = 0; i < array_len; ++i) {
        jstring s =   (jstring)  env->GetObjectArrayElement(argv, i);
        args[i + 1] = jstringToChar(env, s);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "argv: %d: %s", i, args[i + 1]);

    }
    args[array_len + 1] = nullptr;
    pid_t res = create_sub_process(p, args);

    for (int i = 0; i < array_len + 1; ++i) {
        free(args[i]);
    }
    delete []args;
    return res;
}