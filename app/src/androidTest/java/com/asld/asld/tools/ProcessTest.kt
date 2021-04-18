package com.asld.asld.tools

import junit.framework.TestCase

class ProcessTest : TestCase(){
    fun testCreateSub(){
        println("Run test")
        val process = Process("grep", listOf("h"))

        process.exec()

        process.stdin.write("hello\n".toByteArray())
        process.closeStdin()

//        var arr = ByteArray(5){0}
//        process.stdout.read(arr)

        var arr = process.stdout.readBytes()

        val s = String(arr)

        assert(s == "hello\n")

        ProcessUtil.createSubProcess("ls", arrayOf())
    }
}