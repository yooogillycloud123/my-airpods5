#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>

static const char *arg_value(int argc, char **argv, const char *prefix) {
    size_t n = strlen(prefix);
    for (int i = 1; i < argc; ++i) {
        if (strncmp(argv[i], prefix, n) == 0) return argv[i] + n;
    }
    return NULL;
}

int main(int argc, char **argv) {
    uid_t uid = getuid();
    if (uid != 0 && uid != 2000) {
        fprintf(stderr, "STARTER_FATAL uid=%d\n", uid);
        return 6;
    }

    const char *apk = arg_value(argc, argv, "--apk=");
    const char *port = arg_value(argc, argv, "--port=");
    const char *token = arg_value(argc, argv, "--token=");

    if (!apk || !port || !token) {
        fprintf(stderr, "STARTER_FATAL missing args\n");
        return 2;
    }

    if (access(apk, R_OK) != 0) {
        fprintf(stderr, "STARTER_FATAL apk unreadable errno=%d\n", errno);
        return 3;
    }

    pid_t pid = fork();
    if (pid < 0) {
        fprintf(stderr, "STARTER_FATAL fork errno=%d\n", errno);
        return 4;
    }

    if (pid > 0) {
        printf("STARTER_OK pid=%d uid=%d\n", pid, uid);
        fflush(stdout);
        return 0;
    }

    if (setsid() < 0) _exit(10);
    chdir("/");

    int in = open("/dev/null", O_RDONLY);
    int log = open("/data/local/tmp/aaairpods_route.log",
                   O_CREAT | O_WRONLY | O_TRUNC, 0644);

    if (in >= 0) {
        dup2(in, STDIN_FILENO);
        if (in > STDERR_FILENO) close(in);
    }

    if (log >= 0) {
        dup2(log, STDOUT_FILENO);
        dup2(log, STDERR_FILENO);
        if (log > STDERR_FILENO) close(log);
    } else {
        int devnull = open("/dev/null", O_WRONLY);
        if (devnull >= 0) {
            dup2(devnull, STDOUT_FILENO);
            dup2(devnull, STDERR_FILENO);
            if (devnull > STDERR_FILENO) close(devnull);
        }
    }

    setenv("CLASSPATH", apk, 1);

    char classpath_arg[4096];
    snprintf(classpath_arg, sizeof(classpath_arg),
             "-Djava.class.path=%s", apk);

    char *const child_argv[] = {
            "/system/bin/app_process",
            classpath_arg,
            "/system/bin",
            "--nice-name=aa_airpods_route_daemon",
            "com.aaairpods.bridge.RouteDaemon",
            (char *) port,
            (char *) token,
            NULL
    };

    execv(child_argv[0], child_argv);

    dprintf(STDERR_FILENO,
            "STARTER_CHILD_FATAL execv errno=%d\n", errno);
    _exit(5);
}
