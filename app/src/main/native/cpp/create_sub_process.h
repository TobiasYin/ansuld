

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


#ifdef __cplusplus
extern "C" {
#endif

pid_t create_sub_process(char* path, char** args);


#ifdef __cplusplus
}
#endif
#endif
