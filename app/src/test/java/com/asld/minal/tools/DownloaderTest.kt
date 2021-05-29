package org.minal.minal.tools

import android.util.Log
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class)
@PowerMockIgnore("javax.net.ssl.*")
class DownloaderTest : TestCase() {

    fun testTestRun() {
        PowerMockito.mockStatic(Log::class.java)

        println(Log::class.java.methods)

        PowerMockito.`when`(Log.e(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer {
            val tag = it.arguments[0] as String
            val message = it.arguments[1] as String
            val throwable = it.arguments[2] as Throwable?
            println("$tag $message ${throwable?.message}")
            0
        }

        PowerMockito.`when`(Log.d(Mockito.any(), Mockito.any())).thenAnswer{
            val tag = it.arguments[0] as String
            val message = it.arguments[1] as String
            println("$tag $message")
            0
        }

        println(Log::class.java.methods)
        Log.e("h", "g", null)
        println(Log::class.java.declaredMethods)

        DownloadManager.relativeRoot =
            "/Users/tobias/projects/ansuld/app/src/main/java/org/minal/minal/tools/"
        val d = Downloader("http://127.0.0.1:8088/h1", "test/hello", 100)
        d.run()
        while (!d.isFinish) {
            println(d.getProgress())
            Thread.sleep(10)
        }
        println("End")
    }
}