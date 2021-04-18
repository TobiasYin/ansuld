//
// Created by 尹瑞涛 on 2021/4/15.
//

#include "com_asld_asld_tools_ProcessUtil.h"
#include "../cpp/create_sub_process.h"
#include <android/log.h>
#include <wait.h>
#include "unistd.h"

extern "C" JNIEXPORT jint JNICALL Java_com_asld_asld_tools_ProcessUtil_createSubProcess
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

extern "C" void
Java_com_asld_asld_tools_ProcessUtil_createSubProcessFds(JNIEnv *env, jobject thiz, jstring path,
                                                         jobjectArray argv, jobject fds) {
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

    stdfd res = create_sub_process_fds(p, args);

    jclass clazz = env->GetObjectClass(fds);
    jfieldID pid, in, out, err;
    pid = env->GetFieldID(clazz , "pid", "I");
    in = env->GetFieldID(clazz , "in", "I");
    out = env->GetFieldID(clazz , "out", "I");
    err = env->GetFieldID(clazz , "err", "I");

    env->SetIntField(fds, pid, res.pid);
    env->SetIntField(fds, in, res.in);
    env->SetIntField(fds, out, res.out);
    env->SetIntField(fds, err, res.err);

    for (int i = 0; i < array_len + 1; ++i) {
        free(args[i]);
    }
    delete []args;
}

extern "C" jint Java_com_asld_asld_tools_ProcessUtil_waitPid(JNIEnv *env, jobject thiz, jint pid) {
    int status;

    waitpid(pid, &status, 0);
    if WIFEXITED(status) {
        return WEXITSTATUS(status);
    }
    if WIFSIGNALED(status){
        return WSTOPSIG(status);
    }
    return -1;
}

extern "C" jint Java_com_asld_asld_tools_ProcessUtil_closeFd(JNIEnv *env, jobject thiz, jint fd) {
    close(fd);
    return 0;
}
