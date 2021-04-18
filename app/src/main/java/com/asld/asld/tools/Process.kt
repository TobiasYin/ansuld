package com.asld.asld.tools

import java.io.*

class CreateProcessException(msg: String = "Create Process Exception") : Exception(msg)
class ProcessHasBeenExecException(msg: String = "Process Already been Execution") : Exception(msg)
class ProcessHasNotExecException(msg: String = "Process has not execution yet") : Exception(msg)

class Fds {
    var pid = -1
    var `in` = -1
    var out = -1
    var err = -1

    fun isNotValid(): Boolean = pid == -1 || `in` == -1 || out == -1 || err == -1
}

class Process(
    val path: String,
    argv: List<String> = listOf(),
    val env: HashMap<String, EnvItem> = hashMapOf()
) {
    val argv: ArrayList<String> = ArrayList(argv)
    companion object {
        private val runningProcess: HashSet<Process> = HashSet()
        private fun addRunningProcess(p: Process) {
            if (!p.hasExec()) return

            synchronized(runningProcess) {
                runningProcess.add(p)
            }

            Thread {
                p.status = ProcessUtil.waitPid(p.pid)
                p.isEnd = true
                synchronized(runningProcess) {
                    runningProcess.remove(p)
                }
                p.closeStreams()

            }.start()
        }

        fun killAll() {
            runningProcess.forEach { it.kill() }
        }
    }

    var pid: Int = -1
        private set
    lateinit var stdin: FileOutputStream
        private set
    lateinit var stdout: InputStream
        private set
    lateinit var stderr: InputStream
        private set
    var status = -1
        private set
    var isEnd = false
        private set

    fun hasExec() = pid != -1

    fun addEnv(item: EnvItem) {
        if (pid != -1)
            return
        env[item.key] = item
    }

    fun addArg(arg: String){
        if (pid != -1)
            return
        argv.add(arg)
    }

    fun exec() {
        if (pid != -1) {
            throw ProcessHasBeenExecException()
        }
        val fds = Fds()
        val arrArgv = Array(argv.size) { argv[it] }

        if (env.size != 0) {
            ProcessUtil.createSubProcessEnv(path, arrArgv, env, fds)
        } else {
            ProcessUtil.createSubProcessFds(path, arrArgv, fds)
        }

        if (fds.isNotValid()) {
            throw CreateProcessException()
        }
        val stdinFd = FileDescriptor()
        val stdoutFd = FileDescriptor()
        val stderrFd = FileDescriptor()
        val fdField = FileDescriptor::class.java.getDeclaredField("descriptor")
        fdField.isAccessible = true
        fdField.set(stdinFd, fds.`in`)
        fdField.set(stderrFd, fds.err)
        fdField.set(stdoutFd, fds.out)

        pid = fds.pid
        stdin = FileOutputStream(stdinFd)
        stdout = FileInputStream(stdoutFd)
        stderr = FileInputStream(stderrFd)
        addRunningProcess(this)
    }

    fun kill() {
        if (pid == -1) {
            throw ProcessHasNotExecException()
        }
        Process("kill", listOf("-9", pid.toString())).exec()
    }

    fun waitProcess() {
        ProcessUtil.waitPid(pid)
    }


    private fun closeInput(inp: FileInputStream) {
        val fd = inp.fd
        inp.close()
        ProcessUtil.closeFd(fd)
    }

    fun closeStdin() {
        val fd = stdin.fd
        stdin.close()
        ProcessUtil.closeFd(fd)
    }

    private fun readFISToBAIS(inp: FileInputStream): ByteArrayInputStream {
        return ByteArrayInputStream(inp.readBytes())
    }

    fun closeStdout() {
        if (stdout is FileInputStream) {
            val bis = readFISToBAIS(stdout as FileInputStream)
            closeInput(stdout as FileInputStream)
            stdout = bis
        } else {
            stdout.close()
        }
    }

    fun closeStderr() {
        if (stderr is FileInputStream) {
            val bis = readFISToBAIS(stderr as FileInputStream)
            closeInput(stderr as FileInputStream)
            stderr = bis
        } else {
            stdout.close()
        }
    }

    fun closeStreams() {
        closeStdin()
        closeStdout()
        closeStderr()
    }
}

class EnvItem(
    val key: String,
    val value: String,
    val mode: Int = ENV_MODE_OVERWRITE,
    val sep: Char = ':'
) {
    companion object {
        const val ENV_MODE_CONCATENATE = 1
        const val ENV_MODE_OVERWRITE = 2
        const val ENV_MODE_SKIP = 3
    }
}

object ProcessUtil {

    init {
        System.loadLibrary("create-sub-process-lib")
    }

    fun closeFd(fd: FileDescriptor): Int {
        val fdField = FileDescriptor::class.java.getDeclaredField("descriptor")
        fdField.isAccessible = true
        val rfd = fdField.get(fd) as Int
        return closeFd(rfd)
    }

    fun createSubProcessEnv(
        path: String,
        argv: Array<String>,
        env: HashMap<String, EnvItem>,
        fds: Fds
    ) {
        val envArr = env.entries.map { it.value }.toTypedArray()
        createSubProcessEnv(path, argv, envArr, fds)
    }


    external fun createSubProcess(path: String, argv: Array<String>): Int
    external fun createSubProcessFds(path: String, argv: Array<String>, fds: Fds)
    external fun createSubProcessEnv(
        path: String,
        argv: Array<String>,
        env: Array<EnvItem>,
        fds: Fds
    )

    external fun waitPid(pid: Int): Int
    external fun closeFd(fd: Int): Int
}
