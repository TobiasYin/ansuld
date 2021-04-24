package com.asld.asld

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asld.asld.databinding.ActivityDownloadImagesBinding
import com.asld.asld.tools.Downloader
import java.io.File
import com.asld.asld.tools.Process
import kotlin.concurrent.thread


val baseURL = "http://192.168.2.190:8088"

val downloadFiles = listOf(
    DownloadItem(
        "$baseURL/proot-termux-src/proot.arm-64",
        "proot"
    ),
    DownloadItem(
        "$baseURL/hello",
        "hello"
    ),
    DownloadItem(
        "$baseURL/lubuntu-desktop.tar.gz",
        "lubuntu.tar.gz"
    ) {
        val proc = Process("tar", listOf("-xvzf", it.absolutePath))
        proc.chdir = it.parent!!
        proc.useLogger()
        proc.exec()
        proc.waitProcess()
    }
)


class DownloadImages : AppCompatActivity() {


    val handler = Handler(Looper.getMainLooper()) {
        val pos = it.obj as Int
        adaptor.notifyItemChanged(pos)
        true
    }
    lateinit var adaptor: DownloadItemAdaptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDownloadImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adaptor = DownloadItemAdaptor(downloadFiles, filesDir, handler)
        binding.downloadList.layoutManager = LinearLayoutManager(this)
        binding.downloadList.adapter = adaptor

    }
}

class DownloadItem(val url: String, val fileName: String, val backProcess: (f: File) -> Unit = {}) {
    var downloading = false
    var downloadRate = 0.0f
    var backProcessing = false

    fun checkStatus(filesDir: File): Boolean =
        File(filesDir, fileName).exists()
}


class DownloadItemAdaptor(
    val items: List<DownloadItem>,
    val baseDir: File,
    val updateHandler: Handler
) :
    RecyclerView.Adapter<DownloadItemAdaptor.ItemViewHolder>() {
    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val downloadButton: Button = view.findViewById(R.id.download_button)
        val downloadTitle: TextView = view.findViewById(R.id.download_title)
        val downloadStatus: TextView = view.findViewById(R.id.download_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.download_item, parent, false)
        return ItemViewHolder(view)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.downloadTitle.text = item.fileName
        holder.downloadButton.text = "Download"
        holder.downloadStatus.text = "Status: " +
                when {
                    item.downloading ->
                        "Downloading (${item.downloadRate}%)"
                    item.backProcessing ->
                        "Downloaded, Extracting..."
                    item.checkStatus(baseDir) ->
                        "Existing"
                    else ->
                        "Not Exist"
                }
        holder.downloadButton.setOnClickListener {
            if (item.downloading)
                return@setOnClickListener
            item.downloading = true
            val downloader = Downloader(item.url, item.fileName, 100)
            thread {
                val sendUpdateMessage = {
                    val m = Message()
                    m.obj = position
                    updateHandler.sendMessage(m)
                }
                downloader.run()
                while (!downloader.isFinish) {
                    item.downloadRate = downloader.getProgress() * 100
                    sendUpdateMessage()
                    Thread.sleep(200)
                }
                item.downloading = false
                item.backProcessing = true
                sendUpdateMessage()
                item.backProcess(File(baseDir, item.fileName))
                item.backProcessing = false
                sendUpdateMessage()
            }

        }

    }


    override fun getItemCount(): Int = items.count()
}