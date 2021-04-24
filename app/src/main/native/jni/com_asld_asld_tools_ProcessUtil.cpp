//
// Created by 尹瑞涛 on 2021/4/15.
//

#include "com_asld_asld_tools_ProcessUtil.h"
#include "../cpp/create_sub_process.h"
#include <android/log.h>
#include <wait.h>
#include "unistd.h"

extern "C" JNIEXPORT jint JNICALL Java_com_asld_asld_tools_ProcessUtil_createSubProcess
        (JNIEnv *env, jobject that, jstring path, jobjectArray argv) {

    char *p = jstringToChar(env, path);
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "path: %s", p);
    int array_len = env->GetArrayLength(argv);
    char **args = new char *[array_len + 2];
    args[0] = p;
    for (int i = 0; i < array_len; ++i) {
        jstring s = (jstring) env->GetObjectArrayElement(argv, i);
        args[i + 1] = jstringToChar(env, s);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "argv: %d: %s", i, args[i + 1]);

    }
    args[array_len + 1] = nullptr;
    pid_t res = create_sub_process(p, args);

    for (int i = 0; i < array_len + 1; ++i) {
        free(args[i]);
    }
    delete[]args;
    return res;
}

extern "C" void
Java_com_asld_asld_tools_ProcessUtil_createSubProcessFds(JNIEnv *env, jobject thiz, jstring path,
                                                         jobjectArray argv, jobject fds) {
    char *p = jstringToChar(env, path);
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "path: %s", p);
    int array_len = env->GetArrayLength(argv);
    char **args = new char *[array_len + 2];
    args[0] = p;
    for (int i = 0; i < array_len; ++i) {
        jstring s = (jstring) env->GetObjectArrayElement(argv, i);
        args[i + 1] = jstringToChar(env, s);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "argv: %d: %s", i, args[i + 1]);

    }
    args[array_len + 1] = nullptr;

    stdfd res = create_sub_process_fds(p, args);

    jclass clazz = env->GetObjectClass(fds);
    jfieldID pid, in, out, err;
    pid = env->GetFieldID(clazz, "pid", "I");
    in = env->GetFieldID(clazz, "in", "I");
    out = env->GetFieldID(clazz, "out", "I");
    err = env->GetFieldID(clazz, "err", "I");

    env->SetIntField(fds, pid, res.pid);
    env->SetIntField(fds, in, res.in);
    env->SetIntField(fds, out, res.out);
    env->SetIntField(fds, err, res.err);

    for (int i = 0; i < array_len + 1; ++i) {
        free(args[i]);
    }
    delete[]args;
}

extern "C" jint Java_com_asld_asld_tools_ProcessUtil_waitPid(JNIEnv *env, jobject thiz, jint pid) {
    int status;

    int r = waitpid(pid, &status, 0);
    if (r == 0){
        return -10000;
    }
    if WIFEXITED(status) {
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "[%d] EXITED, %d", pid, WEXITSTATUS(status));
        return WEXITSTATUS(status);
    }
    if WIFSIGNALED(status) {
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "[%d] SIGNALED, %d", pid, WTERMSIG(status));
        return WTERMSIG(status);
    }
    return -1;
}

extern "C" jint Java_com_asld_asld_tools_ProcessUtil_closeFd(JNIEnv *env, jobject thiz, jint fd) {
    close(fd);
    return 0;
}

extern "C" void
Java_com_asld_asld_tools_ProcessUtil_createSubProcessEnv(JNIEnv *env, jobject thiz, jstring path,
                                                         jobjectArray argv, jobjectArray envs,
                                                         jobject fds) {
    int envlen = env->GetArrayLength(envs);

    env_item **items = new env_item *[envlen + 1];
    items[envlen] = nullptr;

    if (envlen != 0) {
        jobject o = env->GetObjectArrayElement(envs, 0);
        jclass clazz = env->GetObjectClass(o);
        jfieldID key, value, mode, sep;
        key = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
        value = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
        mode = env->GetFieldID(clazz, "mode", "I");
        sep = env->GetFieldID(clazz, "sep", "C");

        for (int i = 0; i < envlen; ++i) {
            env_item *item = new env_item;
            jobject obj = env->GetObjectArrayElement(envs, i);
            jstring k = (jstring) env->GetObjectField(obj, key);
            item->key = jstringToChar(env, k);
            jstring v = (jstring) env->GetObjectField(obj, value);
            item->value = jstringToChar(env, v);
            item->mode = env->GetIntField(obj, mode);
            item->sep = env->GetCharField(obj, sep);
            items[i] = item;
        }
    }


    char *p = jstringToChar(env, path);
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "path: %s", p);
    int array_len = env->GetArrayLength(argv);
    char **args = new char *[array_len + 2];
    args[0] = p;
    for (int i = 0; i < array_len; ++i) {
        jstring s = (jstring) env->GetObjectArrayElement(argv, i);
        args[i + 1] = jstringToChar(env, s);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "argv: %d: %s", i, args[i + 1]);

    }
    args[array_len + 1] = nullptr;

    stdfd res = create_sub_process_env(p, args, items);

    jclass clazz = env->GetObjectClass(fds);
    jfieldID pid, in, out, err;
    pid = env->GetFieldID(clazz, "pid", "I");
    in = env->GetFieldID(clazz, "in", "I");
    out = env->GetFieldID(clazz, "out", "I");
    err = env->GetFieldID(clazz, "err", "I");

    env->SetIntField(fds, pid, res.pid);
    env->SetIntField(fds, in, res.in);
    env->SetIntField(fds, out, res.out);
    env->SetIntField(fds, err, res.err);

    for (int i = 0; i < array_len + 1; ++i) {
        free(args[i]);
    }
    delete[]args;

    for (int i = 0; i < envlen; ++i) {
        free(items[i]->value);
        free(items[i]->key);
        delete items[i];
    }
    delete[]items;
}

extern "C" void
Java_com_asld_asld_tools_ProcessUtil_createProcess(JNIEnv *env, jobject thiz, jobject args,
                                                   jobject fds) {

    jclass clazz = env->GetObjectClass(args);
    jfieldID envID, pathID, argvID, chdirID;
    envID = env->GetFieldID(clazz, "env", "[Lcom/asld/asld/tools/EnvItem;");
    pathID = env->GetFieldID(clazz, "path", "Ljava/lang/String;");
    argvID = env->GetFieldID(clazz, "argv", "[Ljava/lang/String;");
    chdirID = env->GetFieldID(clazz, "chdir", "Ljava/lang/String;");

    jstring path = (jstring) env->GetObjectField(args, pathID);
    jstring chdir = (jstring) env->GetObjectField(args, chdirID);
    jobjectArray envs = (jobjectArray) env->GetObjectField(args, envID);
    jobjectArray argv = (jobjectArray) env->GetObjectField(args, argvID);

    int envlen = env->GetArrayLength(envs);


    env_item **items = nullptr;
    if (envlen != 0) {
        items = new env_item *[envlen + 1];
        items[envlen] = nullptr;
        jobject o = env->GetObjectArrayElement(envs, 0);
        jclass clazz = env->GetObjectClass(o);
        jfieldID key, value, mode, sep;
        key = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
        value = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
        mode = env->GetFieldID(clazz, "mode", "I");
        sep = env->GetFieldID(clazz, "sep", "C");

        for (int i = 0; i < envlen; ++i) {
            env_item *item = new env_item;
            jobject obj = env->GetObjectArrayElement(envs, i);
            jstring k = (jstring) env->GetObjectField(obj, key);
            item->key = jstringToChar(env, k);
            jstring v = (jstring) env->GetObjectField(obj, value);
            item->value = jstringToChar(env, v);
            item->mode = env->GetIntField(obj, mode);
            item->sep = env->GetCharField(obj, sep);
            items[i] = item;
        }
    }

    char *chdir_c = nullptr;
    if (env->GetStringLength(chdir) != 0) {
        chdir_c = jstringToChar(env, chdir);
    }

    char *p = jstringToChar(env, path);
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "path: %s", p);
    int array_len = env->GetArrayLength(argv);
    char **argstr = new char *[array_len + 2];
    argstr[0] = p;
    for (int i = 0; i < array_len; ++i) {
        jstring s = (jstring) env->GetObjectArrayElement(argv, i);
        argstr[i + 1] = jstringToChar(env, s);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "argv: %d: %s", i, argstr[i + 1]);

    }
    argstr[array_len + 1] = nullptr;

    startProcessArgs args1;
    args1.args = argstr;
    args1.path = p;
    args1.env = items;
    args1.chdir = chdir_c;
    stdfd res = create_process(args1);

    clazz = env->GetObjectClass(fds);
    jfieldID pid, in, out, err;
    pid = env->GetFieldID(clazz, "pid", "I");
    in = env->GetFieldID(clazz, "in", "I");
    out = env->GetFieldID(clazz, "out", "I");
    err = env->GetFieldID(clazz, "err", "I");

    env->SetIntField(fds, pid, res.pid);
    env->SetIntField(fds, in, res.in);
    env->SetIntField(fds, out, res.out);
    env->SetIntField(fds, err, res.err);

    for (int i = 0; i < array_len + 1; ++i) {
        free(argstr[i]);
    }
    delete[]argstr;

    for (int i = 0; i < envlen; ++i) {
        free(items[i]->value);
        free(items[i]->key);
        delete items[i];
    }
    delete[]items;
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "sucess to end");
    if (chdir_c != nullptr) {
        free(chdir_c);
    }
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "sucess to free chdir");
}
