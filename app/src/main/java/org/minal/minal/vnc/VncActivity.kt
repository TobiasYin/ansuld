package org.minal.minal.vnc

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.*
import android.util.Log
import android.view.*
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.minal.minal.R
import org.minal.minal.exception.ErrorCode
import org.minal.minal.service.ShellDaemon
import org.minal.minal.tools.ProgressBarDialog
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.min


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VncActivity : AppCompatActivity() {
    private val TAG = "VncActivity"
    lateinit var vncCanvas: VncCanvas
    lateinit var vncConn: VNCConn
    private lateinit var connection: ConnectionBean
    private lateinit var frameBufferSize: Point
    var port = 5900
    private var initializedServer = false
    private var initializedClient = false
    private lateinit var mClipboardManager: ClipboardManager
    private lateinit var inputHandler: InputHandler
    private lateinit var vncPresentation: VncPresentation
    lateinit var touchPad: LinearLayout
    private lateinit var appBar: Toolbar
    private var presentationDismiss = false

    // if display's resolution is bigger than max, scale it to full the screen
    private var canvasScale: Float = 1f
    private var handleDisplayRemove = false
    private val handler = object : Handler(Looper.getMainLooper()) {
        //todo test error situation
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("vncErrorHandler", (msg.obj as ErrorCode).toString())
            when (msg.obj as ErrorCode) {
                ErrorCode.VNC_CONN_TO_CLIENT_BREAK -> {
                    vncCanvas.vncConn.shutdown()
                    initVncClient()
                    initVncServer()
                }
                ErrorCode.VNC_DISPLAY_CHANGED -> {
                    if (!handleDisplayRemove) {
                        Log.d(TAG, "handleMessage: display down")
                        handleDisplayRemove = true
                        fullRouteInit()
                        handleDisplayRemove = false
                        //TODO ?????????alertdialog
                    }
                }
            }
        }
    }

    private fun isTouchEvent(event: MotionEvent): Boolean {
        Log.d(
            TAG, "isTouchEvent: ${
                event.source == InputDevice.SOURCE_TOUCHSCREEN ||
                        event.source == InputDevice.SOURCE_TOUCHPAD
            }"
        )
        return event.source == InputDevice.SOURCE_TOUCHSCREEN ||
                event.source == InputDevice.SOURCE_TOUCHPAD
    }

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //todo appbar affect height
        setContentView(R.layout.activity_touch_pad)
        appBar = findViewById(R.id.vnc_toolbar)
        setSupportActionBar(appBar)
        touchPad = findViewById<LinearLayout>(R.id.touch_pad_layout).apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        changeAppBarVisibility()
        // ???????????????
        val that = this
        val handleTouch = { view: View, f: (View, MotionEvent) -> Unit ->
            view.setOnTouchListener { v, e ->
                Log.d(TAG, "onTouch: $e")
                if (!isTouchEvent(e)) false
                else {
                    val location = intArrayOf(0, 0)
                    view.getLocationInWindow(location)
                    val h = view.height
                    val w = view.width
                    if (!(e.rawX < location[0] || e.rawX > location[0] + w || e.rawY < location[1] || e.rawY > location[1] + h) && e.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                        f(v, e)
                    }
                    true
                }
            }

        }
        findViewById<TextView>(R.id.back).apply {
            handleTouch(this) { v, e ->
                that.finish()
            }
        }
        findViewById<TextView>(R.id.close_all).apply {
            handleTouch(this) { v, e ->
                ProgressBarDialog.create(that, "Clean Environments...") {
                    endVncClient()
                    vncPresentation.dismiss()
                    endVncServer()
                    that.finish()
                }
            }
        }

//        thread{
//            sleep(5000)
//            touchPad.requestPointerCapture()
//            touchPad.setOnCapturedPointerListener{v, e ->
//                Log.d(TAG, "OnCapturedPointerListener: $e")
//                true
//            }
//        }
        // set the second screen

        Log.d(TAG, "begin")

        mClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        inputHandler = InputHandler(this)
        inputHandler.init()
        touchPad = findViewById(R.id.touch_pad)
        fullRouteInit()
    }

    private fun updateExclude() {
        if (Build.VERSION.SDK_INT >= 30) {
            display?.getSize()?.let {
                window.systemGestureExclusionRects =
                    listOf(
                        Rect(0, 0, 50, it.y),
                        Rect(it.x - 50, 0, it.x, it.y),
                        Rect(0, 0, it.x, 50),
                        Rect(0, it.y - 50, it.x, it.y)
                    )
            }
        }
    }

    private fun fullRouteInit() {
        // initialize tags
        presentationDismiss = false
        initializedClient = false
        initializedServer = false
        if (!chooseDisplay()) {
            AlertDialog.Builder(this).apply {
                setCancelable(false)
                setTitle("ERROR")
                setMessage(ErrorCode.VNC_DISPLAY_NOT_FOUND.msg)
                setPositiveButton("Retry") { d, _ ->
                    d.cancel()
                    fullRouteInit()
                }
                setNegativeButton("Cancel") { _, _ ->
                    finish()
                }
            }.create().show()
        } else {
            ProgressBarDialog.create(this, "Init vnc server") {
                initVncServer()
                it.updateView { it.textView.text = "Init vnc client..." }
                initVncClient()
                it.updateView { it.textView.text = "Finish initiation" }
            }
        }
    }

    private fun initVncServer() {
        //todo ??????????????????
        port = ShellDaemon.startVNC(frameBufferSize)
        initializedServer = true
    }

    private fun endVncServer() {
        if (initializedServer) {
            Log.d(TAG, "endVncServer: ")
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
        vncConn = VNCConn()
        vncConn.setHandler(handler)
        // add conn to canvas
        runOnUiThread {
            vncPresentation.show()
            vncCanvas = vncPresentation.getVncCanvas()
            vncCanvas.initializeVncCanvas(this, vncConn, canvasScale)
            // add canvas to conn. be sure to call this before init!
            vncConn.setCanvas(vncCanvas)
            // the actual connection init
            vncConn.init(connection) {}

            initializedClient = true
            Log.d(TAG, "isShowing: ${vncPresentation.isShowing}")

        }


    }

    private fun endVncClient() {
        if (initializedClient) {
            vncConn.shutdown()
            initializedClient = false
        }
    }

    private fun chooseDisplay(): Boolean {
        // 1 for audio, 2 for video
        val route =
            (getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter).getSelectedRoute(
                2
            )
        Log.d(TAG, route.toString())
        val presentationDisplay = if (route != null && route.presentationDisplay != null) {
            route.presentationDisplay
        } else {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            for (i in dm.displays) {
                Log.d(TAG, "chooseDisplay: $i")
            }
            if (dm.displays.size <= 1) {
                Log.d(TAG, "No display found")
                return false
            } else {
                // TODO ??????????????????????????????
                dm.displays[1]
            }
        }

        Log.d(
            TAG,
            "chooseDisplay: height(${presentationDisplay}), width(${presentationDisplay.width})"
        )
        val presentationDisplaySize = presentationDisplay.getSize()
        Log.d(TAG, "chooseDisplay: $presentationDisplaySize")
        frameBufferSize = buildResolution(presentationDisplaySize)
        canvasScale = min(
            presentationDisplaySize.x.toFloat() / frameBufferSize.x,
            presentationDisplaySize.y.toFloat() / frameBufferSize.y
        )
        Log.d(TAG, "frameBufferSize:$frameBufferSize, canvasScale:$canvasScale")

        // ???????????????????????????
        val defaultDisplaySize = windowManager.getDefaultDisplay().getSize()
        Log.d(TAG, "chooseDisplay: $defaultDisplaySize")
        inputHandler.setPointerScale(
            presentationDisplaySize.x.toFloat() / defaultDisplaySize.x,
            presentationDisplaySize.y.toFloat() / defaultDisplaySize.y
        )
        vncPresentation = VncPresentation(this, presentationDisplay, handler).apply {
            setOnDismissListener {
                Log.d("VncPresentation", "ondismiss: ")
                presentationDismiss = true
            }
        }
        return true
    }

    private fun Display.getSize(): Point {
        val size = Point()
        this.getRealSize(size)
        return size
    }

    private fun getMainDisplay(): Display? {
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val arrayOfDisplays = dm.displays
        for (d in arrayOfDisplays) {
            if (d.displayId == 0) {
                return d
            }
        }
        return null
    }

    private fun buildResolution(point: Point): Point {
        // ????????????R.integer.????????????id?????????????????????
        var width = min(point.x, resources.getInteger(R.integer.max_resolution_width))
        var height = min(point.y, resources.getInteger(R.integer.max_resolution_height))
        val ratio = point.y.toFloat() / point.x
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

    /**
     * KeyBoard
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // when use finger to back send keycode_back to hide appbar
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            changeAppBarVisibility()
//            Log.d(TAG, "onKeyDown: changeAppBarVisibility")
            Log.d(TAG, "onKeyDown: back press")
            return true
        }
        Log.d(TAG, "onKeyDown: keyCode:$keyCode, $event")
        vncConn.sendKeyEvent(keyCode, event, false)
        return false
    }

    /**
     * KeyBoard
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // when use finger to back send keycode_back to hide appbar
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            changeAppBarVisibility()
            Log.d(TAG, "onKeyUp: changeAppBarVisibility")
            return true
        }
        Log.d(TAG, "onKeyDown: keyCode:$keyCode, $event")
        vncConn.sendKeyEvent(keyCode, event, false)
        return false
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return inputHandler.onGenericMotionEvent(event)
    }


    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        // ????????????????????????appBar
        supportActionBar?.also {
            if (supportActionBar!!.isShowing)
                changeAppBarVisibility()
        }
        return inputHandler.onGenericMotionEvent(event)
    }

    override fun onResume() {
        Log.d(TAG, "onResume: ")
        super.onResume()
        if (presentationDismiss) {
            Log.d(TAG, "onResume: put up")
            thread {
                sleep(200)
                runOnUiThread {
                    fullRouteInit()
                }
            }
        } else if (initializedClient) {
            vncCanvas.onResume()
            vncCanvas.enableRepaints()
//            vncPresentation.show()
            // get Android clipboard contents
            if (mClipboardManager.hasPrimaryClip()) {
                try {
                    vncCanvas.vncConn.sendCutText(mClipboardManager.primaryClip!!.getItemAt(0).text.toString())
                } catch (e: NullPointerException) {
                    //unused
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
        if (initializedClient)
            vncPresentation.show()
        thread {
            Thread.sleep(100)
            updateExclude()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        super.onPause()
        // needed for the GLSurfaceView
        if (initializedClient) {
            vncCanvas.onPause()
            vncCanvas.disableRepaints()
            // get VNC cuttext and post to Android
            if (vncConn.cutText != null) {
                copyTomClipboardManager(vncConn.cutText)
            }
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()
        if (initializedClient)
            vncPresentation.hide()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
        // only cut vnc client, keep server alive
        if (initializedClient) {
            endVncClient()
            vncPresentation.dismiss()
        }
    }

    fun changeAppBarVisibility() {
        if (supportActionBar!!.isShowing) {
            supportActionBar!!.hide()
//            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            Log.d(TAG, "onWindowFocusChanged: hideAppBar")
        } else {
            supportActionBar!!.show()
//            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            Log.d(TAG, "onWindowFocusChanged: showAppBar")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_vnc, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.back_button -> {
                finish()
            }
            R.id.kill_backend -> {
                ProgressBarDialog.create(this, "Clean Environments...") {
                    endVncClient()
                    vncPresentation.dismiss()
                    endVncServer()
                    finish()
                }
            }
        }
        return true
    }

    private fun copyTomClipboardManager(content: CharSequence?) {
        mClipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                null,
                content
            )
        ) //???????????????????????????????????????????????????????????????????????????
        if (mClipboardManager.hasPrimaryClip()) {
            mClipboardManager.primaryClip!!.getItemAt(0).text
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