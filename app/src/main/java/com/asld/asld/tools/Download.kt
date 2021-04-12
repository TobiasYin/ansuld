package com.asld.asld.tools

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

object DownloadManager {
    lateinit var relativeRoot: String
        private set
}

class Downloader(private val url: String, private val savePath: String, private val threads: Int = 4) {
    inner class Partition(val start: Long, val end: Long, var nowAt: Long = 0)
    private val client = OkHttpClient()
    private var ifRange = false
    private var contentLength: Long = 0
    private lateinit var saveFile: File
    private var saveRandomAccessFile: RandomAccessFile? = null
    private var partStatus = ArrayList<Partition>()
    private var lastModified : Long? = null

    private fun checkHeaders() {
        val request = Request.Builder()
                .url(url)
                .addHeader("Connection", "Keep-Alive")
                .head()
                .build()
        val resp = client.newCall(request).execute()
        if (resp.code != 200) {
            throw IOException("Response code: ${resp.code}")
        }
        checkIfRange(resp)
        val lastModified = resp.headers["Last-Modified"]
        if (lastModified != null){
            val zdt: ZonedDateTime = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
            this.lastModified = zdt.toEpochSecond()
        }
    }
    private fun checkIfRange(resp: Response) {
        contentLength = resp.headers["Content-Length"]?.toLong() ?: -1
        if (resp.headers["Accept-Ranges"] != "bytes" || contentLength == -1L) {
            ifRange = false
        }
        ifRange = true
    }

    private fun createSaveFile() {
        val f = File(DownloadManager.relativeRoot, savePath)
        if (!f.exists()) {
            var success = false
            f.mkdirs()
            success = f.createNewFile()
            if (!success) {
                throw IOException("Error on create file")
            }
        }
        if (ifRange) {
            saveRandomAccessFile = RandomAccessFile(f.absoluteFile, "rw")
            saveRandomAccessFile!!.setLength(contentLength)
        }
    }

    private fun partition() {
        val partitions = if (ifRange && threads > 0 && threads < 100) threads else 1
        val partSize = contentLength / partitions
        repeat(partitions){
            val end = if (it == partitions - 1) contentLength - 1 else (partSize + 1) * it
            partStatus.add(Partition(partSize * it, end))
        }
    }

    private fun execute(){
        partition()
        repeat(partStatus.size) {
            val part = partStatus[it]
            val req = Request.Builder().url(url).addHeader("Range", "bytes=${part.nowAt}-${part.end}")
        }
    }
}