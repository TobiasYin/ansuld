package com.asld.asld.tools

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class Fds {
    var pid = -1
    var `in` = -1
    var out = -1
    var err = -1

    fun isValid(): Boolean = pid == -1 || `in` == -1 || out == -1 || err == -1
}

class Process private constructor(
    val pid: Int,
    val stdin: FileOutputStream,
    val stdout: FileInputStream,
    val stderr: FileInputStream,
){
    companion object{
        fun create(path: String, argv: Array<String>): Process {
            val fds = Fds()
            SubProcess.createSubProcessFds(path, argv, fds)
            if (!fds.isValid()) {
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

            return Process(
                fds.pid,
                FileOutputStream(stdinFd),
                FileInputStream(stdoutFd),
                FileInputStream(stderrFd)
            )
        }
    }

}

class CreateProcessException(msg: String = "Create Process Exception") : Exception(msg)

object SubProcess {

    init {
        System.loadLibrary("create-sub-process-lib")
    }


    external fun createSubProcess(path: String, argv: Array<String>): Int
    external fun createSubProcessFds(path: String, argv: Array<String>, fds: Fds)
}