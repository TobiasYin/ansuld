package com.asld.asld.vnc

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.media.MediaRouter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.asld.asld.R
import com.asld.asld.databinding.VncCanvasBinding

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VncActivity : AppCompatActivity() {
    private val TAG = "VncActivity"
    lateinit var vncCanvas: VncCanvas
    private lateinit var connection: ConnectionBean


    private lateinit var mClipboardManager: ClipboardManager
    private lateinit var inputHandler: PointerInputHandler
    private lateinit var vncPresentation: VncPresentation


    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title bar, status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // hide system ui after softkeyboard close as per https://stackoverflow.com/a/21278040/361413
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { hideSystemUI() }

        // Setup resizing when keyboard is visible.
        //
        // Ideally, we would let Android manage resizing but because we are using a fullscreen window,
        // most of the "normal" options don't work for us.
        //
        // We have to hook into layout phase and manually shift our view up by adding appropriate
        // bottom padding.
        val contentView = findViewById<View>(android.R.id.content)
        contentView.viewTreeObserver.addOnGlobalLayoutListener {
            val frame = Rect()
            contentView.getWindowVisibleDisplayFrame(frame)
            val contentBottom = contentView.bottom
            var paddingBottom = contentBottom - frame.bottom
            if (paddingBottom < 0) paddingBottom = 0

            //When padding is less then 20% of height, it is most probably navigation bar.
            if (paddingBottom > 0 && paddingBottom < contentBottom * .20) return@addOnGlobalLayoutListener
            contentView.setPadding(0, 0, 0, paddingBottom) //Update bottom
        }

        // set the second screen
        Log.d("vnc", "begin")

        val route = (getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter).getSelectedRoute(
            2
        )
        Log.d("vnc", route.toString())
        if (route != null) {
            val presentationDisplay = route.presentationDisplay
            Log.d(TAG, "chooseDisplay: height(${presentationDisplay.height}), weight(${presentationDisplay.width})")

            if (presentationDisplay != null) {
                vncPresentation = VncPresentation(this, presentationDisplay)
            }
        }
        vncPresentation.show()

        vncCanvas = vncPresentation.getVncCanvas()
        mClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        inputHandler = PointerInputHandler(this)
        inputHandler.init()


        /*
		 * Setup connection bean.
		 */
        connection = defaultConnectionBean()
//        if (connection.port === 0) connection.port = 5900
        Log.d(TAG, "Got raw intent " + connection.toString())
        // Parse a HOST:PORT entry
//        connection.parseHostPort(connection.address)


        /*
		 * Setup canvas and conn.
		 */
        val conn = VNCConn()
        vncCanvas.initializeVncCanvas(this, inputHandler, conn) // add conn to canvas
        conn.setCanvas(vncCanvas) // add canvas to conn. be sure to call this before init!
        // the actual connection init
        conn.init(connection) {}

        setContentView(R.layout.activity_touch_pad)
        vncCanvas.requestRender()
    }

    private fun defaultConnectionBean():ConnectionBean{
        val conn = ConnectionBean()

        conn.address = "127.0.0.1"
//        conn.address = "192.168.2.129"
        conn.id = 0 // is new!!
        try {
            conn.port = 5900
        } catch (nfe: NumberFormatException) {
        }
        conn.userName = "root"
        conn.password = "qwe123"
//        conn.password = "qwe123"
        conn.useLocalCursor = true // always enable

        conn.colorModel = COLORMODEL.C24bit.toString()

        return conn
    }

    @SuppressLint("ClickableViewAccessibility")
    fun onCreat1e(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = VncCanvasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        vncCanvas = binding.vncCanvas
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