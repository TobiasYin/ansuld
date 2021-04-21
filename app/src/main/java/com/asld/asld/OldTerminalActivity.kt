package com.asld.asld

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class OldTerminalActivity : AppCompatActivity() {
    val proc = Process("sh")
    val lines = ArrayList<Line>()
    val adaptor = TerminalItemAdaptor(lines)
    lateinit var linesView: RecyclerView
    val handler = Handler(Looper.getMainLooper()){
        val line = it.obj as Line
        addLine(line)
        true
    }

    fun addLine(line: Line){
        lines.add(line)
        adaptor.notifyItemInserted(lines.size - 1)
        linesView.scrollToPosition(lines.size - 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTernimalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        linesView = binding.linesList
        binding.linesList.layoutManager = LinearLayoutManager(this)
        binding.linesList.adapter = adaptor
        thread  {
            val downloader = Downloader("http://192.168.2.190:8088/proot-runtime.tar.gz", "proot-runtime.tar.gz")
            downloader.run()
        }
        proc.addEnv(
            EnvItem(
                "PATH",
                "${filesDir.absolutePath}/usr/bin",
                EnvItem.ENV_MODE_CONCATENATE
            )
        )
        proc.addEnv(
            EnvItem(
                "PATH",
                "${filesDir.absolutePath}/usr/libexec",
                EnvItem.ENV_MODE_CONCATENATE
            )
        )
        proc.addEnv(
            EnvItem(
                "SHELL",
                "${filesDir.absolutePath}/usr/bin/bash",
                EnvItem.ENV_MODE_OVERWRITE
            )
        )
        proc.addEnv(EnvItem("PREFIX", "${filesDir.absolutePath}/usr", EnvItem.ENV_MODE_OVERWRITE))
        proc.addEnv(EnvItem("ANDROID_ROOT", "/system", EnvItem.ENV_MODE_OVERWRITE))
        proc.addEnv(EnvItem("LD_LIBRARY_PATH", "${filesDir.absolutePath}/usr/lib", EnvItem.ENV_MODE_CONCATENATE))
        proc.addEnv(
            EnvItem(
                "LD_PRELOAD",
                "${filesDir.absolutePath}/usr/lib/libtermux-exec.so",
                EnvItem.ENV_MODE_OVERWRITE
            )
        )
        proc.exec()
        proc.stdin.write("cd ${filesDir.absolutePath} \n".toByteArray())

        binding.execCmdButton.setOnClickListener {
            val text = binding.commandInput.text.toString().trim()
            if (text.isNotEmpty()){
                binding.commandInput.text.clear()
                proc.stdin.write((text + "\n").toByteArray())
                addLine(Line(text, Line.LineTypeInput))
            }
        }

        Thread {
            val scanner = Scanner(proc.stdout)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val m =  Message()
                m.obj = Line(line, Line.LineTypeNorm)
                handler.sendMessage(m)
            }

        }.start()
        Thread {
            val scanner = Scanner(proc.stderr)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val m =  Message()
                m.obj = Line(line, Line.LineTypeErr)
                handler.sendMessage(m)
            }

        }.start()
    }

    override fun onDestroy() {
        proc.kill()
        super.onDestroy()
    }
}