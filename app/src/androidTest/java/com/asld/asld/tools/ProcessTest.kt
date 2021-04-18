package com.asld.asld.tools

import junit.framework.TestCase
import java.lang.Exception

class ProcessTest : TestCase() {
    fun testGrep() {
        println("Run test")
        val process = Process("grep", listOf("h"))
        process.addEnv(EnvItem("hello", "world"))

        process.exec()

        process.stdin.write("hello\n".toByteArray())
        process.closeStdin()

//        var arr = ByteArray(5){0}
//        process.stdout.read(arr)

        var arr = process.stdout.readBytes()

        val s = String(arr)

        assertEquals(s, "hello\n")
        assert(s == "hello\n")

    }

    fun testEnv() {
        println("Run test env")
        val process = Process("env")
        process.addEnv(EnvItem("hello", "world"))
        process.addEnv(EnvItem("PATH", "/mytools", EnvItem.ENV_MODE_CONCATENATE))
        process.exec()

        var arr = process.stdout.readBytes()

        val s = String(arr)

        var containsA = false
        var containsB = false
        s.split("\n").forEach {
            val items = it.split('=')
            if (items.size == 2) {
                if (items[0] == "hello" && items[1] == "world")
                    containsA = true
                if (items[0] == "PATH" && items[1].contains("/mytools"))
                    containsB = true
            }
        }
        assert(containsA)
        assert(containsB)
    }
}