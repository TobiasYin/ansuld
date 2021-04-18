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
import com.asld.asld.tools.Process
import java.util.*
import kotlin.collections.ArrayList

class TerminalActivity : AppCompatActivity() {
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

class Line(val data: String, val type: Int = Line.LineTypeNorm){
    companion object{
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
        return  LineViewHolder(view)
    }


    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        when(lines[position].type){
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