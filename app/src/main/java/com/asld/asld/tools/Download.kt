package com.asld.asld.tools

import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.Exception
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList

const val TAG = "Download_UTIL"
const val RETRY_TIME = 10

object DownloadManager {
    lateinit var relativeRoot: String
}

class Downloader(
    private val url: String,
    private val savePath: String,
    private var threads: Int = 4
) {
    inner class Partition(val start: Long, val end: Long, val partID: Int, val PartVersion: Long?) {
        var retryTime = 0
        var nowAt: Long = start

        override fun toString(): String {
            return "start: $start end: $end nowAt: $nowAt id: $partID ver: $PartVersion rt: $retryTime"
        }
    }

    private val client = OkHttpClient()
    private var ifRange = false
    private var contentLength: Long = 0
    private lateinit var saveFile: File
    private var saveRandomAccessFile: RandomAccessFile? = null
    private var partStatus = ArrayList<Partition>()
    private var lastModified: Long? = null
    private var calls = ArrayList<Call>()
    private var isExec = false
    var isFinish = false
        private set

    private fun clearCacheState() {
        partStatus = ArrayList()
        calls = ArrayList()
        contentLength = 0
        ifRange = false
        lastModified = null
        isExec = false
    }

    private fun parseDate(date: String): Long =
        ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()

    private fun getLastModifiedFromHeader(resp: Response): Long? =
        resp.headers["Last-Modified"]?.let { parseDate(it) }

    private fun getContentLengthFromHeader(resp: Response): Long? =
        try {
            resp.headers["Content-Length"]?.toLong()
        } catch (e: Exception) {
            null
        }

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
        val lastModified = getLastModifiedFromHeader(resp)
        if (lastModified != null) {
            if (this.lastModified != null && this.lastModified != lastModified)
                clearCacheState()
            this.lastModified = lastModified
        }
        checkIfRange(resp)
        resp.headers["Content-Type"]?.let {
            if (it == "application/octet-stream")
                isExec = true
        }
    }

    private fun checkIfRange(resp: Response) {
        contentLength = getContentLengthFromHeader(resp) ?: -1
        if (resp.headers["Accept-Ranges"] != "bytes" || contentLength == -1L) {
            ifRange = false
        }
        ifRange = true
    }

    private fun createSaveFile() {
        val f = File(DownloadManager.relativeRoot, savePath)
        if (!f.exists()) {
            f.parent?.let {
                File(it).mkdirs()
            }
            val success = f.createNewFile()
            if (!success) {
                throw IOException("Error on create file")
            }
        }
        if (ifRange) {
            saveRandomAccessFile = RandomAccessFile(f.absoluteFile, "rw")
            saveRandomAccessFile!!.setLength(contentLength)
        }
        if (isExec) {
            f.setExecutable(true)
        }
        saveFile = f
    }

    private fun partition() {
        val partitions = if (ifRange && threads > 0) if (threads > 100) 100 else threads else 1
        val partSize = contentLength / partitions
        repeat(partitions) {
            val end = (if (it == partitions - 1) contentLength else (partSize) * (it + 1)) - 1
            partStatus.add(Partition(partSize * it, end, it, lastModified))
            Log.d(TAG, "partition: $it ${partStatus[it]}")
        }
    }

    private fun checkAndHandleTime(newTime: Long?): Boolean {
        if (newTime == lastModified) {
            return true
        }
        if (newTime == null || lastModified == null) {
            return false
        }
        synchronized(lastModified!!) {
            val lastModified = this.lastModified!!
            if (newTime <= lastModified)
                return false
            this.lastModified = newTime
            cancelAndRetry()
        }
        return false
    }

    private fun cancelAndRetry() {
        Log.e(TAG, "cancel All calls!")
        synchronized(calls) {
            if (calls.size == 0) return
            for (call in calls) {
                call.cancel()
            }
            clearCacheState()
            run()
        }
    }

    private fun partReqFail(part: Partition, e: IOException) {
        part.retryTime += 1
        Log.e(
            TAG,
            "requestPartition Request Error, part_id: ${part.partID}, retryTime: ${part.retryTime}, " +
                    "part version: ${part.PartVersion}, last: $lastModified Error: ",
            e
        )
        if (part.retryTime <= RETRY_TIME && part.PartVersion == lastModified) {
            requestPartition(part)
        }
    }

    private fun writeToFile(data: ByteArray, end: Long, part: Partition) {
        val len = (end - part.nowAt + 1).toInt()
        if (partStatus.size > 1) {
            if (saveRandomAccessFile != null) {
                synchronized(saveRandomAccessFile!!) {
                    if (partStatus.size <= part.partID || partStatus[part.partID].PartVersion != part.PartVersion) {
                        return
                    }
                    val file = saveRandomAccessFile!!
                    file.seek(part.nowAt)
                    file.write(data, 0, len)
                    part.nowAt = end + 1
                }
            } else {
                throw IOException("No File to write!")
            }
        } else {
            if (partStatus.size <= part.partID || partStatus[part.partID].PartVersion != part.PartVersion) {
                return
            }
            val file = saveFile
            val dataSub = if (data.size == len) data else {
                val arr = ByteArray(len)
                repeat(len) {
                    arr[it] = data[it]
                }
                arr
            }

            file.writeBytes(dataSub)
            part.nowAt = end + 1
        }
    }

    private fun checkIfDown() {
        synchronized(calls) {
            for (c in calls) {
                if (!c.isExecuted()) {
                    return
                }
            }
        }
        synchronized(partStatus) {
            for (p in partStatus) {
                if (p.nowAt < p.end)
                    return
            }
        }
        saveRandomAccessFile?.close()
        isFinish = true
    }

    private fun requestPartition(part: Partition) {
        Log.d(TAG, "call requestPartition , part: ${part.partID}")
        val req = Request.Builder()
            .url(url).addHeader("Range", "bytes=${part.nowAt}-${part.end}").get().build()
        val call = client.newCall(req)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                partReqFail(part, e)
            }

            override fun onResponse(call: Call, response: Response) {
                val modified = getLastModifiedFromHeader(response)
                if (!checkAndHandleTime(modified)) {
                    return
                }
                val retry = { info: String ->
                    Log.d(TAG, "Retry call!, part:${part.partID}, info: $info")
                    lastModified = 0
                    cancelAndRetry()
                }
                var length = getContentLengthFromHeader(response)
                var partEnd = part.end
                if (partStatus.size > 1) {
                    val range = response.headers["Content-Range"]
                    if (range == null) {
                        threads = 1
                        retry("no range retry")
                        return
                    }
                    // Content-Range: bytes 100-150/1270
                    var totalLen = 0L
                    var state = 0
                    var types = ""
                    var start = 0L
                    var end = 0L
                    var lastIndex = 0
                    range.forEachIndexed { i, c ->
                        when (state) {
                            0 -> if (c == ' ') {
                                types = range.slice(lastIndex until i)
                                lastIndex = i + 1
                                state = 1
                            }
                            1 -> if (c == '-') {
                                start = try {
                                    range.slice(lastIndex until i).toLong()
                                } catch (e: Exception) {
                                    0
                                }
                                lastIndex = i + 1
                                state = 2
                            }
                            2 -> if (c == '/') {
                                end = try {
                                    range.slice(lastIndex until i).toLong()
                                } catch (e: Exception) {
                                    0
                                }
                                lastIndex = i + 1
                                state = 3
                            }
                            3 -> if (i == range.length - 1) {
                                totalLen = try {
                                    range.slice(lastIndex..i).toLong()
                                } catch (e: Exception) {
                                    0
                                }
                                state = 4
                            }
                        }
                    }
                    if (state != 4 || types != "bytes") {
                        // Treats as server don't support multi threads download, use single thread restart
                        threads = 1
                        retry("not bytes range")
                        return
                    }
                    if (totalLen != contentLength) {
                        retry("total length error range: totalLen:$totalLen c: $contentLength")
                        return
                    }
                    if (start != part.nowAt || (length != null && (end - start + 1) != length)) {
                        partReqFail(
                            part,
                            IOException("Return Range not same as expect, expect range: ${part.nowAt}-${part.end}, get range: $start-$end $range , length:  $length")
                        )
                        return
                    }
                    length = end - start + 1
                    if (end < partEnd) partEnd = end
                } else {
                    if (length != null && length != contentLength) {
                        partReqFail(
                            part,
                            IOException("Part length(${length}) are not same with declare($contentLength)")
                        )
                    }
                    length = contentLength
                    partEnd = contentLength - 1
                }

                val data = response.body?.bytes()
                if (data == null) {
                    partReqFail(part, IOException("No body found"))
                    return
                }
                if (data.size.toLong() != length) {
                    partReqFail(
                        part,
                        IOException("Part length(${data.size}) are not same with declare($length)")
                    )
                    return
                }
                writeToFile(data, partEnd, part)
                checkIfDown()
            }
        })
        synchronized(calls) {
            if (calls.size > part.partID)
                calls[part.partID] = call
        }
    }

    private fun execute() {
        partition()
        repeat(partStatus.size) {
            val part = partStatus[it]
            requestPartition(part)
        }
    }

    fun getProgress(): Float {
        if (contentLength == 0L)
            return 0.0F
        var nowTotal = 0L
        partStatus.forEach { nowTotal += (it.nowAt - it.start) }
        return nowTotal.toFloat() / contentLength
    }

    fun run() {
        checkHeaders()
        createSaveFile()
        execute()
    }
}