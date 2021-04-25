package com.asld.asld.vnc

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.media.MediaRouter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.asld.asld.R
import com.asld.asld.exception.ErrorCode
import com.asld.asld.service.ShellDaemon
import com.asld.asld.tools.ProgressBarDialog
import kotlin.math.min


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VncActivity : AppCompatActivity() {
    private val TAG = "VncActivity"
    lateinit var vncCanvas: VncCanvas
    private lateinit var connection: ConnectionBean
    private lateinit var resolution: Point
    var port = 5900
    private var initializedServer = false
    private var initializedClient = false
    private lateinit var mClipboardManager: ClipboardManager
    lateinit var inputHandler: PointerInputHandler
    private lateinit var vncPresentation: VncPresentation

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("vnc", (msg.obj as ErrorCode).toString())
            when (msg.obj as ErrorCode) {
                ErrorCode.VNC_CONN_TO_CLIENT_BREAK -> {
                    vncCanvas.vncConn.shutdown()
                    initVncClient()
                }
                ErrorCode.VNC_CONN_TO_SERVER_BREAK -> initVncServer()
            }
        }
    }

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_pad)
        // set the second screen
        Log.d("vnc", "begin")

        if (!chooseDisplay()) {
            Log.d("vnc", "no display")
            val dialog = ProgressBarDialog.create(this, "Failed to find a second display!").apply {
                setCancelable(true)
                setOnCancelListener {
                    finish()
                }
            }
//            Toast.makeText(this, "Failed to find a second display!", Toast.LENGTH_LONG)
        } else {

            mClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            inputHandler = PointerInputHandler(this)
            inputHandler.init()

            ProgressBarDialog.create(this, "Init vnc Server...") {
                initVncServer()
                it.updateView { it.textView.text = "Init vnc client..." }
                initVncClient()
                it.updateView { it.cancel() }
            }
        }
    }

    private fun initVncServer() {
        port = ShellDaemon.startVNC(resolution)
        initializedServer = true
    }

    private fun endVncServer() {
        if (initializedServer) {
            ShellDaemon.killVNC()
            initializedServer = false
        }
    }

    private fun initVncClient() {
        /*
		 * Setup connection bean.
		 */
        connection = defaultConnectionBean(port)
        Log.d(TAG, "Got raw intent " + connection.toString())


        /*
         * Setup canvas and conn.
         */
        val conn = VNCConn()
        conn.setHandler(handler)
        // add conn to canvas
        runOnUiThread {
            vncPresentation.show()
            vncCanvas = vncPresentation.getVncCanvas()
            vncCanvas.initializeVncCanvas(this, inputHandler, conn)
            // add canvas to conn. be sure to call this before init!
            conn.setCanvas(vncCanvas)
            // the actual connection init
            conn.init(connection) {}

            initializedClient = true
        }


    }

    private fun endVncClient() {
        if (initializedClient) {
            vncCanvas.vncConn.shutdown()
            initializedClient = false
        }
    }

    private fun chooseDisplay(): Boolean {
        val route =
            (getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter).getSelectedRoute(
                2
            )
        Log.d("vnc", route.toString())
        if (route != null) {
            val presentationDisplay = route.presentationDisplay

            if (presentationDisplay != null) {
                Log.d(
                    TAG,
                    "chooseDisplay: height(${presentationDisplay}), width(${presentationDisplay.width})"
                )
                val point = Point()
                presentationDisplay.getRealSize(point)
                Log.d(TAG, "chooseDisplay: $point")
                resolution = buildScaleBy(point)

                Log.d("vnc", "${resolution.x}x${resolution.y}")
                vncPresentation = VncPresentation(this, presentationDisplay)
                return true
            }
        }
        return false
    }

    private fun buildScaleBy(point: Point): Point {
        var width = min(point.x, R.integer.max_resolution_width)
        var height = min(point.y, R.integer.max_resolution_height)
        val ratio = (point.y.toFloat()) / point.x
        if (width * ratio > height) {
            width = (height / ratio).toInt()
        } else {
            height = (width * ratio).toInt()
        }
        return Point(width, height)

    }

    private fun defaultConnectionBean(port: Int): ConnectionBean {
        val conn = ConnectionBean()

        conn.address = "127.0.0.1"
//        conn.address = "192.168.2.129"
        conn.id = 0 // is new!!
        try {
            conn.port = port
        } catch (nfe: NumberFormatException) {
        }
        conn.userName = "root"
        conn.password = "ansuldserver"
//        conn.password = "qwe123"
        conn.useLocalCursor = true // always enable

        conn.colorModel = COLORMODEL.C24bit.toString()

        return conn
    }

    private fun showWaitingDialog(): Unit {
        /* 等待Dialog具有屏蔽其他控件的交互能力
         * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
         * 下载等事件完成后，主动调用函数关闭该Dialog
         */
        val waitingDialog = ProgressDialog(this);
        waitingDialog.setTitle("我是一个等待Dialog");
        waitingDialog.setMessage("等待中...");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();
    }

    // hide systemUI
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return inputHandler.onGenericMotionEvent(event)
    }

    override fun onStop() {
        super.onStop()
        if (initializedClient) {
            vncCanvas.disableRepaints()
            vncPresentation.hide()
        }

    }

    override fun onResume() {
        super.onResume()
        if (initializedClient) {
            vncCanvas.enableRepaints()
            vncPresentation.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        endVncServer()
        endVncClient()
        if (initializedClient) {
            vncPresentation.dismiss()
        }
        finish()
    }

    override fun onPause() {
        super.onPause()
        // needed for the GLSurfaceView
        if (initializedClient) {
            vncCanvas.onPause()

            // get VNC cuttext and post to Android
            if (vncCanvas.vncConn.cutText != null) {
                try {
                    mClipboardManager.text = vncCanvas.vncConn.cutText
                } catch (e: Exception) {
                    //unused
                }
            }
        }
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}