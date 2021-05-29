package org.minal.minal.vnc

import android.content.Context
import java.net.NetworkInterface
import java.net.SocketException

fun getActiveNetworkInterface(c: Context?): NetworkInterface? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (inetAddress.isLoopbackAddress) break // break inner loop, continue with outer loop
                return intf // this is not the loopback and it has an IP address assigned
            }
        }
    } catch (e: SocketException) {
        e.printStackTrace()
    }
    // nothing found
    return null
}