package com.asld.asld

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asld.asld.databinding.ActivityDownloadImagesBinding
import com.asld.asld.tools.Downloader
import java.io.File
import com.asld.asld.tools.Process
import com.asld.asld.tools.ProgressBarDialog
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
        "$baseURL/fserver",
        "fserver"
    ),
    DownloadItem(
        "$baseURL/lubuntu-desktop.tar.gz",
        "lubuntu.tar.gz"
    ) {
        val proc = Process("tar", listOf("-xzf", it.absolutePath))
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
        adaptor = DownloadItemAdaptor(this, downloadFiles, filesDir, handler)
        binding.downloadList.layoutManager = LinearLayoutManager(this)
        binding.downloadList.adapter = adaptor

    }
}

class DownloadItem(val url: String, val fileName: String, val backProcess: (f: File) -> Unit = {}) {
    var downloading = false
    var downloadRate = 0.0f
    var backProcessing = false
    var err = false

    fun checkStatus(filesDir: File): Boolean =
        File(filesDir, fileName).exists()

}


class DownloadItemAdaptor(
    val context: Context,
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
                        "Downloading..."
                    item.backProcessing ->
                        "Downloaded, Extracting..."
                    item.err->
                        "Download Error, Please Try again!"
                    item.checkStatus(baseDir) ->
                        "Existing"
                    else ->
                        "Not Exist"
                }
        holder.downloadButton.setOnClickListener {
            if (item.downloading)
                return@setOnClickListener
            item.downloading = true
            item.backProcessing = false
            item.err = false
            val downloader = Downloader(item.url, item.fileName, 100)
            ProgressBarDialog.create(context, "loading...") {
                val sendUpdateMessage = {
                    val m = Message()
                    m.obj = position
                    updateHandler.sendMessage(m)
                }
                try {
                    sendUpdateMessage()
                    downloader.run()
                    while (!downloader.isFinish) {
                        item.downloadRate = downloader.getProgress() * 100
                        it.updateView {
                            it.textView.text ="downloading... (${item.downloadRate.format(2)}%)"
                        }
                        Thread.sleep(200)
                    }
                    item.downloading = false
                    item.backProcessing = true
                    it.updateView {
                        it.textView.text ="downloaded, extracting..."
                    }
                    sendUpdateMessage()
                    item.backProcess(File(baseDir, item.fileName))
                    item.backProcessing = false
                    sendUpdateMessage()
                }catch (e: Exception){
                    item.downloading = false
                    item.err = true
                    sendUpdateMessage()
                    e.printStackTrace()
                }
            }
        }

    }


    override fun getItemCount(): Int = items.count()
}

fun Float.format(digits: Int) = "%.${digits}f".format(this)