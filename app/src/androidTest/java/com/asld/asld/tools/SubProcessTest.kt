package com.asld.asld.tools

import junit.framework.TestCase

class SubProcessTest : TestCase(){
    fun testCreateSub(){
        println("Run test")
        val s = SubProcess()
        s.createSubProcess("ls", arrayOf())
    }
}