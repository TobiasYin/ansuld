

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

#define ENV_MODE_CONCATENATE 1
#define ENV_MODE_OVERRIDE 2
#define ENV_MODE_SKIP 3

struct env_item{
    char * key;
    char * value;
    int mode;
    char sep;
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
