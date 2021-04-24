package com.asld.asld

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asld.asld.databinding.ActivityTernimalBinding
import com.asld.asld.tools.Downloader
import com.asld.asld.tools.Process
import java.lang.Compiler.command
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class OriginShellActivity : AppCompatActivity() {
    val proc = Process("sh")
    val lines = ArrayList<Line>()
    val adaptor = TerminalItemAdaptor(lines)
    lateinit var linesView: RecyclerView
    val handler = Handler(Looper.getMainLooper()) {
        val line = it.obj as Line
        addLine(line)
        true
    }

    fun addLine(line: Line) {
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
        proc.exec()
        proc.stdin.write("cd ${filesDir.absolutePath} \n".toByteArray())

        binding.execCmdButton.setOnClickListener {
            val text = binding.commandInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.commandInput.text.clear()
                proc.stdin.write((text + "\n").toByteArray())
                addLine(Line(text, Line.LineTypeInput))
            }
        }

        Thread {
            val scanner = Scanner(proc.stdout)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val m = Message()
                m.obj = Line(line, Line.LineTypeNorm)
                handler.sendMessage(m)
            }

        }.start()
        Thread {
            val scanner = Scanner(proc.stderr)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val m = Message()
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