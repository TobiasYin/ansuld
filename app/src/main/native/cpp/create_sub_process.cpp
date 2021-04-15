//
// Created by 尹瑞涛 on 2021/3/27.
//

#include <malloc.h>
#include "unistd.h"
#include "create_sub_process.h"
#include <android/log.h>
#include "pthread.h"

#define BUFFER_SIZE 1024 * 10



inline int circle_add(int i, int size) {
    return (i + 1) % size;
}

inline void output(const char *split, int head, int top, int size, int mode) {
    int buf_size;
    if (top >= head) {
        buf_size = top - head + 1;
    } else {
        buf_size = top + 1 + size - head;
    }
    char *buf = new char[buf_size + 1];
    int index = 0;
    for (int i = head; i < top; i = circle_add(i, size)) {
        buf[index++] = split[i];
    }
    __android_log_print(ANDROID_LOG_DEBUG, "SUB_PROCESS_OUTPUT", "%s", buf);
    delete[] buf;
}

void *log_thread(void *args) {
    log_thread_arg *arg = (log_thread_arg *) args;
    char buf[BUFFER_SIZE];
    char split[BUFFER_SIZE * 2];
    int top = 0;
    int head = 0;
    int read_size;
    while (read_size = read(arg->fd, buf, BUFFER_SIZE)) {
        for (int i = 0; i < read_size; i++) {
            split[top] = buf[i];
            int add_top = circle_add(top, BUFFER_SIZE * 2);
            if (split[top] == '\n' || add_top == head) {
                output(split, head, top, BUFFER_SIZE * 2, arg->prio);
                head = add_top;
            }
            top = add_top;
        }
    }

    free(arg);
    return nullptr;
}

pipe_fd create_log_thread(int prio) {
    pipe_fd fds;
    int ret = pipe((int *) (&fds));
    if (ret != 0) {
        return pipe_fd{-1, -1};
    }
    log_thread_arg *arg = (log_thread_arg *) malloc(sizeof(log_thread_arg));
    arg->prio = prio;
    arg->fd = fds.read;


    pthread_t id;
    pthread_create(&id, NULL, log_thread, (void *) arg);
    __android_log_print(ANDROID_LOG_DEBUG, "CREATE_LOG_THREAD_LOG", "create log thread, tid: %ld, type: %d", id, prio);
    return fds;
}

pipe_fd create_std_out_pipe() {
    return create_log_thread(STDOUT_FILENO);
}

pipe_fd create_std_error_pip() {
    return create_log_thread(STDERR_FILENO);
}

pid_t create_sub_process(char* path, char** args){
    __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "fork!");
    pipe_fd out = create_std_out_pipe();
    pipe_fd err = create_std_error_pip();
    pid_t pid = fork();
    if (pid == 0) {
        dup2(out.write, STDOUT_FILENO);
        dup2(err.write, STDERR_FILENO);
        close(out.read);
        close(out.read);
        int res = execvp(path, args);
        __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "sub process exec error, code: %d, path: %s", res, path);
        for (int i = 0; args[i]; ++i) {
            __android_log_print(ANDROID_LOG_DEBUG, "NATIVE_LOG", "sub process exec error, args i: %d, value: %s", i, args[i]);
        }
        // never return
        return pid;
    } else {
        close(out.write);
        close(out.write);
        return pid;
    }
}

