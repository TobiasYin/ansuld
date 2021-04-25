package com.asld.asld.service

import android.util.Log
import com.asld.asld.TAG
import com.asld.asld.tools.EnvItem
import com.asld.asld.tools.Process
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

object ShellDaemon {
    lateinit var proc: Process
    var lines = ArrayList<Line>()
    var history = ArrayList<Line>()

    var out: Thread? = null
    var err: Thread? = null
    var lineListener: HashMap<Int, (Line) -> Unit> = HashMap()

    private var id = 0

    private var init = false
    lateinit var baseDir: String
    lateinit var relaDir: String

    fun init(baseDir: String, relaDir: String) {
        this.baseDir = baseDir
        this.relaDir = relaDir
        init = true
    }


    fun addLineListener(l: (Line) -> Unit): Int {
        return synchronized(lineListener) {
            val mid = id++
            lineListener[mid] = l
            mid
        }
    }

    fun removeLineListener(id: Int) {
        synchronized(lineListener) {
            lineListener.remove(id)
        }
    }


    fun checkProc(): Boolean {
        if (!this::proc.isInitialized)
            return false
        return proc.hasExec() && proc.isRunning()
    }


    fun checkAndRun() {
        if (!checkProc())
            runProc()
    }

    fun runProc() {
        if (!init) {
            throw Exception("Run Proc before init")
        }
        if (checkProc()) {
            return
        }

        lines = ArrayList()
        proc = Process()
        initDaemonProcess(proc, baseDir, relaDir)
        proc.exec()
        listenOutput()
    }

    fun restart() {
        if (checkProc()) {
            kill()
        }
        runProc()
    }

    fun kill() {
        proc.kill()
        out?.interrupt()
        err?.interrupt()
    }

    private fun addLine(line: Line) {
        synchronized(this) {
            line.lineCount = lines.size
            lines.add(line)
            if (line.type == Line.LineTypeInput) {
                history.add(line)
            }
            lineListener.forEach {
                it.value(line)
            }
        }

    }


    fun execCmd(cmd: String) {
        checkAndRun()
        val inp = Line(cmd, type = Line.LineTypeInput)
        addLine(inp)
        proc.stdin.write((cmd + "\n").toByteArray())
    }

    fun execSyncCmd(cmd: String): Int {
        val standaloneProc = Process()
        initDaemonProcess(standaloneProc, baseDir, relaDir, false)
        standaloneProc.argv.addAll(listOf("-c", "\"$cmd\""))
        standaloneProc.exec()
        standaloneProc.waitProcess()
        return standaloneProc.status
    }

    fun startVNC(width: Int, height: Int): Int{
        killVNC()
        val port = 1
        val res = execSyncCmd("LD_PRELOAD=/lib/aarch64-linux-gnu/libgcc_s.so.1 vncserver :{$port} -localhost no -geometry ${width}x${height}\n")
        if (res != 0){
            throw Exception()
        }
        return 5900 + port
    }

    fun killVNC(){
        execSyncCmd("vncserver -kill :1")
    }


    fun listenOutput() {
        Log.d(TAG, "Success Init bash")
        out = thread {
            val scanner = Scanner(proc.stdout)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                addLine(Line(line, type = Line.LineTypeNorm))
            }
        }


        err = thread {
            val scanner = Scanner(proc.stderr)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                addLine(Line(line, type = Line.LineTypeErr))
            }

        }
    }

    fun initDaemonProcess(
        proc: Process,
        baseDir: String,
        relaDir: String,
        login: Boolean = true,
        cmd: String = ""
    ) {
        proc.path = "$baseDir/proot"
        proc.chdir = baseDir
        val tempDir = File("$baseDir/temp")
        if (!tempDir.exists()) {
            tempDir.mkdir()
        }
        proc.addEnv(EnvItem("PROOT_TMP_DIR", tempDir.absolutePath))
        proc.argv.addAll(
            listOf(
                "--link2symlink",
                "-0",
                "-r",
                relaDir,
                "-b",
                "/dev",
                "-b",
                "/proc",
                "-b",
                "$relaDir/root:/dev/shm",
                "-w",
                "/root",
                "/usr/bin/env",
                "-i",
                "HOME=/root",
                "PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games",
                "TERM=\$TERM",
                "LANG=C.UTF-8",
                "/bin/bash"
            )
        )
        if (login) {
            proc.argv.add("--login")
        }
    }
}

class Line(val data: String, var lineCount: Int = 0, val type: Int = Line.LineTypeNorm) {
    companion object {
        const val LineTypeNorm = 0
        const val LineTypeErr = 1
        const val LineTypeInput = 2
    }
}
