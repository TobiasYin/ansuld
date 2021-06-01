package org.minal.minal

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.minal.minal.databinding.ActivityDownloadImagesBinding
import org.minal.minal.tools.Downloader
import java.io.File
import org.minal.minal.tools.Process
import org.minal.minal.tools.ProgressBarDialog
import java.io.FileOutputStream
import java.util.*


val baseURL = "http://192.168.2.190:8088"

fun checkSystemExist(item: DownloadItem, baseDir: File, relaDir: String): Boolean {
    val sysFile = File(baseDir, "system")
    if (!sysFile.exists())
        return false
    val s = Scanner(sysFile)
    if (s.hasNextLine()) {
        val line = s.nextLine()
        if (line != item.fileName) {
            return false
        }
        return File("${baseDir.absolutePath}/${relaDir}/bin/bash").exists()
    }
    return false
}

fun systemBackProcess(
    item: DownloadItem,
    tarPack: File,
    relaDir: String,
    tarExtraArgs: List<String> = listOf()
) {
    Process("rm", listOf("-rf", relaDir)).apply {
        chdir = tarPack.parent!!
        useLogger()
        exec()
        waitProcess()
    }
    Process("rm", listOf("-rf", "system")).apply {
        chdir = tarPack.parent!!
        useLogger()
        exec()
        waitProcess()
    }
    Process("mkdir", listOf(relaDir)).apply {
        chdir = tarPack.parent!!
        useLogger()
        exec()
        waitProcess()
    }
    val tarArgs = arrayListOf("-xzf", tarPack.absolutePath)
    if (tarExtraArgs.isNotEmpty()) {
        tarArgs.addAll(tarExtraArgs)
    }
    Process("tar", tarArgs).apply {
        chdir = tarPack.parent!!
        useLogger()
        exec()
        waitProcess()
    }
    //写入文件名
    val os = FileOutputStream(File(tarPack.parent!!, "system"))
    os.write(item.fileName.toByteArray())
    os.close()
    Process("rm", listOf("-rf", tarPack.absolutePath)).apply {
        chdir = tarPack.parent!!
        useLogger()
        exec()
        waitProcess()
    }
}

val downloadFiles = listOf(
    DownloadItem(
        "$baseURL/proot-termux-src/proot.arm-64",
        "proot",
        "启动子系统的必要依赖"
    ),
    DownloadItem(
        "$baseURL/lubuntu-desktop.tar.gz",
        "lubuntu.tar.gz",
        "精简版的系统镜像，可从多个镜像中选择一个",
        { item, file ->
            systemBackProcess(item, file, "lubuntu")
        }, { item, baseDir ->
            checkSystemExist(item, baseDir, "lubuntu")
        }),
    DownloadItem(
        "$baseURL/lubuntu-fullv.tar.gz",
        "lubuntu-full.tar.gz",
        "完整版系统，附带常用办公软件、浏览器、编程工具，可从多个镜像中选择一个",
        { item, file ->
            systemBackProcess(item, file, "lubuntu", listOf("-C", "lubuntu"))
        }, { item, baseDir ->
            checkSystemExist(item, baseDir, "lubuntu")
        }),
    DownloadItem(
        "$baseURL/fserver_minal",
        "fserver",
        "文件服务器，开启文件服务器后可以从http://ip:8088访问此应用的文件"
    )
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

class DownloadItem(
    val url: String,
    val fileName: String,
    val desc: String,
    val backProcess: ((f: DownloadItem, File) -> Unit)? = null,
    val checker: ((DownloadItem, File) -> Boolean)? = null
) {
    var downloading = false
    var downloadRate = 0.0f
    var backProcessing = false
    var err = false

    fun checkStatus(filesDir: File): Boolean {
        if (checker != null)
            return checker.invoke(this, filesDir)
        return File(filesDir, fileName).exists()
    }

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
        val downloadDesc: TextView = view.findViewById(R.id.download_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.download_item, parent, false)
        return ItemViewHolder(view)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, p: Int) {
        val position = holder.adapterPosition
        val item = items[position]
        holder.downloadTitle.text = item.fileName
        holder.downloadButton.text = "Download"
        holder.downloadDesc.text = item.desc
        holder.downloadStatus.text = "Status: " +
                when {
                    item.downloading ->
                        "Downloading..."
                    item.backProcessing ->
                        "Downloaded, Extracting..."
                    item.err ->
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
                            it.textView.text = "downloading... (${item.downloadRate.format(2)}%)"
                        }
                        Thread.sleep(200)
                    }
                    item.downloading = false
                    item.backProcessing = true
                    it.updateView {
                        it.textView.text = "downloaded, extracting..."
                    }
                    sendUpdateMessage()
                    item.backProcess?.invoke(item, File(baseDir, item.fileName))
                    item.backProcessing = false
                    sendUpdateMessage()
                } catch (e: Exception) {
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