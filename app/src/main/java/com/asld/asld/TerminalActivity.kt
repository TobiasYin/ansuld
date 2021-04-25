package com.asld.asld

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
import com.asld.asld.service.Line
import com.asld.asld.service.ShellDaemon
import java.util.*

const val TAG = "TerminalActivityTAG"


class TerminalActivity : AppCompatActivity() {


    var curOffset = 0
    var nowInput = ""

    var handlerID = 0
    val adaptor = TerminalItemAdaptor()
    var lastUpdate = -1

    lateinit var linesView: RecyclerView
    val handler = Handler(Looper.getMainLooper()) {
        addLineUpdate(it.arg1)
        true
    }


    var out: Thread? = null
    var err: Thread? = null

    fun addLineUpdate(line: Int) {
        Log.d(TAG, "addLineUpdate: $line")
        val nowLast = ShellDaemon.lines.size - 1
        if (nowLast == lastUpdate)
            return
        adaptor.notifyItemRangeInserted(lastUpdate + 1, nowLast - lastUpdate)
        linesView.scrollToPosition(nowLast)
        lastUpdate = nowLast
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityTernimalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        linesView = binding.linesList
        binding.linesList.layoutManager = LinearLayoutManager(this)
        binding.linesList.adapter = adaptor
        // 把已有的元素列为已加载元素
        lastUpdate = ShellDaemon.lines.size - 1

        handlerID = ShellDaemon.addLineListener {
            val m = Message()
            m.arg1 = it.lineCount
            handler.sendMessage(m)
        }

        binding.execCmdButton.setOnClickListener {
            val text = binding.commandInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.commandInput.text.clear()
                nowInput = ""
                ShellDaemon.execCmd(text)
            }
        }
        binding.startVnc.setOnClickListener {
            ShellDaemon.execCmd("./run_vnc.sh")
        }
        binding.killVnc.setOnClickListener {
            ShellDaemon.execCmd("vncserver -kill :1")
        }
        binding.lastInput.setOnClickListener {
            if (curOffset == 0) {
                nowInput = binding.commandInput.text.toString()
            }
            if (ShellDaemon.history.size - curOffset - 1 < 0) {
                return@setOnClickListener
            }
            curOffset += 1
            binding.commandInput.setText(ShellDaemon.history[ShellDaemon.history.size - curOffset].data)
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
            if (ShellDaemon.history.size - curOffset < 0) {
                return@setOnClickListener
            }
            binding.commandInput.setText(ShellDaemon.history[ShellDaemon.history.size - curOffset].data)
        }

    }

    override fun onDestroy() {
//        proc.kill()
        ShellDaemon.removeLineListener(handlerID)
        super.onDestroy()
    }
}


class TerminalItemAdaptor :
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
        if (position >= ShellDaemon.lines.size) return
        val item = ShellDaemon.lines[position]
        when (item.type) {
            Line.LineTypeNorm -> {
                holder.line.text = item.data
                holder.line.setTextColor(Color.BLACK)
            }
            Line.LineTypeInput -> {
                holder.line.text = "$ " + item.data
                holder.line.setTextColor(Color.BLACK)
            }
            Line.LineTypeErr -> {
                holder.line.text = item.data
                holder.line.setTextColor(Color.RED)
            }
        }
    }


    override fun getItemCount(): Int = ShellDaemon.lines.size
}