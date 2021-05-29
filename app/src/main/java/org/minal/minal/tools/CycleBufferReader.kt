package org.minal.minal.tools

import java.io.InputStream

class CycleBufferReader(val inputStream: InputStream) {
    val bufferSize = 1024 * 20
    val buffer = ByteArray(bufferSize)
    var nowStart = 0
    var nowEnd = 0

    fun readLine(): String?{
        val readSize = if (nowEnd >= nowStart){
            inputStream.read(buffer, nowEnd, bufferSize - nowEnd)
        } else {
            inputStream.read(buffer, nowEnd, nowStart - nowEnd)
        }

        if (readSize == 0){
            return null
        }

        nowEnd = (nowEnd + readSize) % bufferSize

        var i = nowStart
        while (buffer[i] == '\n'.toByte()){
            i++
            if (nowEnd in (nowStart + 1)..i) break
            if (nowStart > nowEnd){
                i %= bufferSize
                if (i in (nowEnd + 1) until nowStart) break
            }
        }
        val nl = i

        val newLine = ByteArray(if (nl > nowStart) nl - nowStart else bufferSize-nowStart + nl)
        i = nowStart
        for (j in newLine.indices){
            newLine[j] = buffer[i]
            i++
        }

        nowStart = nl
        return String(newLine)
    }
}