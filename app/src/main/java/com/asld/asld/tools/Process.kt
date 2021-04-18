package com.asld.asld.tools

import java.io.*

class CreateProcessException(msg: String = "Create Process Exception") : Exception(msg)
class ProcessHasNotExecException(msg: String = "Process has not execution yet") : Exception(msg)

class Fds {
    var pid = -1
    var `in` = -1
    var out = -1
    var err = -1

    fun isNotValid(): Boolean = pid == -1 || `in` == -1 || out == -1 || err == -1
}

class Process(
    val path: String, val argv: List<String> = listOf(), val env: Map<String, String> = hashMapOf()
) {

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
    lateinit var stdin: FileOutputStream
    lateinit var stdout: InputStream
    lateinit var stderr: InputStream
    var status = -1
    var isEnd = false
        private set

    fun hasExec() = pid != -1

    fun exec() {
        val fds = Fds()
        val arrArgv = Array(argv.size) { argv[it] }
        ProcessUtil.createSubProcessFds(path, arrArgv, fds)
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

    fun readFISToBAIS(inp: FileInputStream): ByteArrayInputStream {
        return ByteArrayInputStream(inp.readBytes())
    }

    fun closeStdout() {
        if (stdout is FileInputStream) {
            val bis = readFISToBAIS(stdout as FileInputStream)
            closeInput(stdout as FileInputStream)
            stdout = bis
        } else{
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

    external fun createSubProcess(path: String, argv: Array<String>): Int
    external fun createSubProcessFds(path: String, argv: Array<String>, fds: Fds)
    external fun waitPid(pid: Int): Int
    external fun closeFd(fd: Int): Int
}
