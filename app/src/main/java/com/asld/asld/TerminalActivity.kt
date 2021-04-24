package com.asld.asld

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asld.asld.databinding.ActivityTernimalBinding
import com.asld.asld.tools.Downloader
import com.asld.asld.tools.EnvItem
import com.asld.asld.tools.Process
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

const val TAG = "TerminalActivityTAG"

fun initProotProcess(proc: Process, baseDir: String, relaDir: String) {
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
            "/bin/bash",
            "--login"
        )
    )
}

class TerminalActivity : AppCompatActivity() {

    var checkFiles = hashSetOf("proot", "lubuntu-desktop.tar.gz")

    companion object {
        var proc = Process("")
        var lines = ArrayList<Line>()
        var history = ArrayList<Line>()

        fun clearState() {
            proc = Process("")
            lines = ArrayList()
            history = ArrayList()
        }
    }

    var curOffset = 0
    var nowInput = ""

    val adaptor = TerminalItemAdaptor(lines)
    lateinit var linesView: RecyclerView
    val handler = Handler(Looper.getMainLooper()) {
        val line = it.obj as Line
        addLine(line)
        true
    }
    var out: Thread? = null
    var err: Thread? = null

    fun addLine(line: Line) {
        lines.add(line)
        adaptor.notifyItemInserted(lines.size - 1)
        linesView.scrollToPosition(lines.size - 1)
    }

    var initStatus = false


    override fun onStart() {
        super.onStart()
        var notExisted = false
        downloadFiles.forEach {
            if (checkFiles.contains(it.fileName)) {
                if (!it.checkStatus(filesDir.absoluteFile)) {
                    notExisted = true
                }
            }
        }
        if (notExisted) {
            val intent = Intent(this, DownloadImages::class.java)
            startActivity(intent)
            return
        }
        if (proc.hasExec() && !proc.isRunning()) {
            clearState()
        }
        initStatus = true
        if (!proc.hasExec()) {
            initProotProcess(proc, filesDir.absolutePath, "lubuntu")
            proc.exec()
            litsenOutput()
        }
    }

    fun litsenOutput() {
        thread {
            Log.d(TAG, "Success Init bash")
            out = thread {
                val scanner = Scanner(proc.stdout)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    val m = Message()
                    m.obj = Line(line, Line.LineTypeNorm)
                    handler.sendMessage(m)
                }

            }
            err = thread {
                val scanner = Scanner(proc.stderr)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    val m = Message()
                    m.obj = Line(line, Line.LineTypeErr)
                    handler.sendMessage(m)
                }

            }
        }
    }


    fun execCmd(cmd: String) {
        proc.stdin.write((cmd + "\n").toByteArray())
        val inp = Line(cmd, Line.LineTypeInput)
        addLine(inp)
        history.add(inp)
        curOffset = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityTernimalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        linesView = binding.linesList
        binding.linesList.layoutManager = LinearLayoutManager(this)
        binding.linesList.adapter = adaptor


        binding.execCmdButton.setOnClickListener {
            val text = binding.commandInput.text.toString().trim()
            if (text.isNotEmpty() && initStatus) {
                binding.commandInput.text.clear()
                nowInput = ""
                execCmd(text)
            }
        }
        binding.startVnc.setOnClickListener {
            execCmd("./run_vnc.sh")
        }
        binding.killVnc.setOnClickListener {
            execCmd("vncserver -kill :1")
        }
        binding.lastInput.setOnClickListener {
            if (curOffset == 0) {
                nowInput = binding.commandInput.text.toString()
            }
            if (history.size - curOffset - 1 < 0) {
                return@setOnClickListener
            }
            curOffset += 1
            binding.commandInput.setText(history[history.size - curOffset].data)
        }
        binding.nextInput.setOnClickListener {
            if (curOffset == 0) {
                return@setOnClickListener
            }
            curOffset -= 1
            if (curOffset == 0) {
                binding.commandInput.text.clear()
                binding.commandInput.setText(nowInput)
                return@setOnClickListener
            }
            if (history.size - curOffset < 0) {
                return@setOnClickListener
            }
            binding.commandInput.setText(history[history.size - curOffset].data)
        }

    }

    override fun onDestroy() {
//        proc.kill()
        super.onDestroy()
    }
}

class Line(val data: String, val type: Int = Line.LineTypeNorm) {
    companion object {
        const val LineTypeNorm = 0
        const val LineTypeErr = 1
        const val LineTypeInput = 2
    }
}

class TerminalItemAdaptor(val lines: List<Line>) :
    RecyclerView.Adapter<TerminalItemAdaptor.LineViewHolder>() {
    inner class LineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val line: TextView = view.findViewById(R.id.line_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.terminal_item, parent, false)
        return LineViewHolder(view)
    }


    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        when (lines[position].type) {
            Line.LineTypeNorm -> {
                holder.line.text = lines[position].data
                holder.line.setTextColor(Color.BLACK)
            }
            Line.LineTypeInput -> {
                holder.line.text = "$" + lines[position].data
                holder.line.setTextColor(Color.BLACK)
            }
            Line.LineTypeErr -> {
                holder.line.text = lines[position].data
                holder.line.setTextColor(Color.RED)
            }
        }
    }


    override fun getItemCount(): Int = lines.count()
}