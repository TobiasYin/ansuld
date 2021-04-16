

#ifndef _Included_create_sub_process
#define _Included_create_sub_process

struct log_thread_arg {
    int prio;
    int fd;
};

struct pipe_fd {
    int read;
    int write;
};

struct stdfd{
    pid_t pid;
    int in;
    int out;
    int err;
};


#ifdef __cplusplus
extern "C" {
#endif

pid_t create_sub_process(char* path, char** args);
stdfd create_sub_process_fds(char *path, char **args);

#ifdef __cplusplus
}
#endif
#endif
