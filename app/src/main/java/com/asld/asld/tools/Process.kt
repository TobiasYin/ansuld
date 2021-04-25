package com.asld.asld.tools

import android.util.Log
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.thread

const val PTAG = "ProcessUtil"

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
    var path: String = "",
    argv: List<String> = listOf(),
    val env: ArrayList<EnvItem> = arrayListOf()
) {
    val argv: ArrayList<String> = ArrayList(argv)

    companion object {
        private val runningProcess: HashMap<Int, Process> = HashMap()
        private fun addRunningProcess(p: Process) {
            if (!p.hasExec()) return

            synchronized(runningProcess) {
                runningProcess[p.pid] = p
            }

            Thread {
                p.status = ProcessUtil.waitPid(p.pid)
                p.isEnd = true
                synchronized(runningProcess) {
                    runningProcess.remove(p.pid)
                }
                p.closeStreams()
                Log.d(TAG, "Process [${p.path}|${p.pid}] end status:  ${p.status}")

            }.start()
        }

        fun isRunning(pid: Int): Boolean {
            return runningProcess.containsKey(pid)
        }

        fun killAll() {
            synchronized(runningProcess) {
                runningProcess.entries.forEach { it.value.kill() }
                runningProcess.clear()
            }
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
    var outLogger = false
    var errLogger = false
    var chdir = ""

    fun useLogger() {
        outLogger = true
        errLogger = true
    }

    private fun startISLogger(s: InputStream, level: Int) {
        val scanner = Scanner(s)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            Log.println(level, PTAG, "$path, $pid: $line")
        }
    }

    fun hasExec() = pid != -1

    fun addEnv(item: EnvItem) {
        if (pid != -1)
            return
        env.add(item)
    }

    fun addArg(arg: String) {
        if (pid != -1)
            return
        argv.add(arg)
    }

    fun exec() {
        if (pid != -1) {
            throw ProcessHasBeenExecException()
        }
        Log.d(PTAG, "process exec: $path ${argv.joinToString(" ")}")
        val fds = Fds()
        val arrArgv = Array(argv.size) { argv[it] }

        ProcessUtil.createProcess(CreateProcessArgs(path, arrArgv, env.toTypedArray(), chdir), fds)

        Log.d(TAG, "exec: Success create process: ${path}, ${fds.pid}")

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
        if (outLogger) {
            Log.d(PTAG, "exec: $path use outlogger")
            thread(true) {
                startISLogger(stdout, Log.DEBUG)
            }
        }
        if (errLogger) {
            Log.d(PTAG, "exec: $path use errlogger")
            thread(true) {
                startISLogger(stderr, Log.ERROR)
            }
        }
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

    fun isRunning(): Boolean = Process.isRunning(pid)

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


class CreateProcessArgs(
    val path: String,
    val argv: Array<String>,
    val env: Array<EnvItem> = arrayOf(),
    val chdir: String = ""
)

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

    external fun createProcess(args: CreateProcessArgs, fds: Fds)
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
