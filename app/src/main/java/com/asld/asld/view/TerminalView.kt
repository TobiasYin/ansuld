package com.asld.asld.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.asld.asld.R
import java.lang.StringBuilder

const val TAG = "MYVIEW"

abstract class Drawable(val view: TerminalView) {
    val paint: Paint = Paint()
    var theme: TerminalTheme = view.theme
        set(value) {
            updateTheme(value)
            field = value
        }

    protected open fun updateTheme(theme: TerminalTheme) {}

    abstract fun onDraw(canvas: Canvas)
    open fun initPaint() {}
}

class TerminalTheme(
    val backGroundColor: Int = Color.BLACK,
    val primaryTextColor: Int = Color.WHITE,
    val cursorColor: Int = Color.WHITE,
    val fontSize: Float = 50f,
    val lineSpace: Float = 10f
)

class TerminalView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var draws = ArrayList<Drawable>()
    var theme = TerminalTheme()
        set(value) {
            draws.forEach { it.theme = value }
            field = value
        }

     val cursor: Cursor
     val text: Text
     val backGround: BackGround

    init {
        backGround = newDrawable()
        text = newDrawable()
        cursor = newDrawable()
        cursor.text = text
        text.cursor = cursor
    }

    private inline fun <reified T : Drawable> newDrawable(): T {
        val clz = T::class.java
        val mCreate = clz.getConstructor(TerminalView::class.java)
        mCreate.isAccessible = true
        val item = mCreate.newInstance(this)
        item.initPaint()
        draws.add(item)
        return item
    }

    init {

        addText(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer mattis non felis vitae pretium. Vestibulum lacinia turpis vitae nisl tempus, at consectetur enim consequat. Proin gravida, velit a ultricies egestas, enim purus suscipit diam, a viverra risus diam sed dui. Phasellus consectetur, nisl ac maximus gravida, justo erat accumsan magna, eu pulvinar nunc mi sit amet nisi. Nullam auctor congue tortor, a viverra tortor molestie vehicula. Vestibulum placerat est turpis, vitae rhoncus ex vestibulum eu. Curabitur eu libero quis orci scelerisque sodales eget in quam.\n" +
                    "\n" +
                    "Mauris facilisis nisi lectus, sit amet commodo urna malesuada vitae. Curabitur tortor libero, finibus ut augue eu, pellentesque tincidunt ante. Morbi leo tellus, commodo a mauris dictum, suscipit laoreet felis. Nunc quis lorem ut dolor commodo ultricies a eu mauris. Vivamus molestie mauris at lectus luctus commodo. Suspendisse consectetur molestie auctor. Curabitur dictum efficitur leo, scelerisque cursus magna dignissim non. Nunc ullamcorper nisi risus, at pulvinar turpis fringilla ut. Sed sagittis ac purus ac finibus. Sed porta dolor nec eleifend laoreet. Integer sodales, felis at sagittis ornare, felis purus molestie lacus, molestie ultricies neque augue vitae lacus. Sed bibendum, eros ut imperdiet lobortis, sapien arcu blandit eros, sit amet bibendum mauris lorem eget nisl. In ultrices ornare lacus non suscipit. Nulla id pharetra mi. Proin nec mauris in sapien facilisis bibendum sed vitae lectus.\n" +
                    "\n" +
                    "In eget feugiat sapien. Vestibulum interdum eu magna vel lobortis. Nunc ligula nulla, tempor malesuada lectus rutrum, convallis tempus est. Mauris volutpat luctus arcu at porta. Mauris cursus erat leo, sit amet volutpat magna elementum et. Praesent rutrum placerat urna id lacinia. Vivamus lacus lacus, egestas lobortis iaculis vitae, posuere quis elit. Integer at nulla elementum, tristique nunc nec, pulvinar dui. Nunc at metus lectus. Suspendisse auctor sagittis dui, vel finibus sapien scelerisque quis. Sed non porttitor diam, fermentum euismod erat.\n" +
                    "\n" +
                    "Mauris congue mi vitae nulla fringilla, nec interdum tellus luctus. Nulla finibus, nunc quis feugiat consequat, lectus mi ultrices nisi, a feugiat odio sem et ipsum. Donec viverra a lorem sit amet varius. Nam elit turpis, semper et turpis eu, fringilla tincidunt augue. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Nunc bibendum, justo sit amet auctor feugiat, justo orci hendrerit erat, sit amet egestas ligula sem a orci. Vestibulum ut risus nisl. Suspendisse finibus risus diam. Donec non justo ultricies, consequat arcu non, tempus nibh. Ut congue commodo enim, in blandit massa pharetra in. Interdum et malesuada fames ac ante ipsum primis in faucibus. Sed eget diam nec arcu varius viverra. Sed libero nisi, sodales non congue eget, maximus in orci. Nam non commodo diam.\n" +
                    "\n" +
                    "Sed sapien lectus, molestie et vestibulum vel, cursus eget nulla. Curabitur euismod nulla odio, vel posuere libero hendrerit faucibus. Duis lacinia dolor quis porttitor ullamcorper. In vel commodo turpis. Donec condimentum magna massa, at blandit lectus facilisis vel. Integer ullamcorper, arcu sit amet pulvinar elementum, urna nulla mollis nisl, eget scelerisque dolor nisl sed est. Vivamus dictum ante at justo dignissim, non convallis diam pharetra. In viverra lorem porttitor, suscipit nunc nec, congue lorem. Mauris in dolor at neque vulputate luctus id ac ligula. Quisque efficitur bibendum est id mattis. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Praesent pretium urna vitae rutrum dapibus. Suspendisse vitae felis sit amet enim finibus finibus. Integer a arcu augue. Donec efficitur vel ante vitae convallis."
        )
    }

    fun addText(text: String) {
        this.text.addText(text)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        text.width = measuredWidth
        Log.d(TAG, "measure: w: $measuredWidth, h:$measuredHeight, lines: ${text.lines.size}")
        setMeasuredDimension(measuredWidth, (text.getBottom() + theme.lineSpace).toInt())
    }


    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG, "draw: w: $width, h:$height")

        canvas?.let { canvas ->
            draws.forEach { it.onDraw(canvas) }
        }
    }
}

class Cursor(view: TerminalView) : Drawable(view) {
    var line: Int = 0
    var offset: Int = 0
    var text: Text? = null

    override fun initPaint() {
        paint.color = theme.cursorColor
        paint.alpha = (255 * 0.5).toInt()
    }

    fun flash() {
        text?.let {
            line = it.lines.size - 1
            if (line < 0)
                line = 0
            offset = if(line < it.lines.size) it.lines[line].length else 0
        }
    }

    fun move(x: Int, y: Int) {
        Log.d(TAG, "Cursor move: $x $y")
        text?.let {
            line += y
            if (line > it.lines.size - 1){
                line = it.lines.size - 1
            }
            if(line < 0){
                line = 0
            }
            offset += x
            val realLine = it.lines[line]
            if (offset >= realLine.length){
                if (line < it.lines.size - 1){
                    line += 1
                    offset = 0
                }else if (line == it.lines.size - 1){
                    offset = realLine.length
                }else{
                    offset = realLine.length - 1
                }
            }
            if (offset < 0) {
                line -= 1
                offset = if (line in 0 until it.lines.size) {
                    it.lines[line].length - 1
                } else {
                    0
                }
            }
        }
        view.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        text?.let {
            val drawLine =
                if (line > it.lines.size - 1) it.lines.size - 1 else if (line < 0) 0 else line
            val drawOffset =
                if (drawLine > it.lines.size - 1 || drawLine < 0) 0 else if (offset > it.lines[drawLine].length) it.lines[drawLine].length else if (offset < 0) 0 else offset

            val yb = it.getLineBottom(drawLine) + theme.lineSpace / 2
            val y = yb + it.paint.fontMetrics.ascent
            var x = it.paint.measureText(it.lines[drawLine].substring(0, drawOffset))
            var xr = if (drawOffset > it.lines[drawLine].length - 1) 2 * x - it.paint.measureText(it.lines[drawLine].substring(0, drawOffset - 1)) else  it.paint.measureText(it.lines[drawLine].substring(0, drawOffset + 1))
            x += it.padding
            xr += it.padding

            Log.d(TAG, "onDraw: Draw Cursor,  x: $x, xr: $xr, y: $y, yb: $yb")
            Log.d(TAG, "onDraw: Draw Cursor, drawLine: $drawLine, drawOffset: $drawOffset line: $line offset: $offset")
            canvas.drawRect(x, y, xr, yb, paint)
        }
    }
}

class Text(view: TerminalView) : Drawable(view) {
    private val splitText = false
    private var text = ""
    private var lastWidth = 0
    private var lastLineEnd = 0
    private var lastSpace = -1
    var cursor: Cursor? = null

    var padding = 30f
        set(value) {
            val diff = value != field
            field = value
            if (diff)
                cutLines()
        }
    var lines = ArrayList<String>()
    var width = 0
        set(value) {
            val diff = value != field
            field = value
            if (diff)
                cutLines()
        }

    var line = StringBuilder()
    var nextLine = StringBuilder()

    fun getLineBottom(lineNo: Int): Float{
        return (lineNo + 1) * theme.fontSize + lineNo * theme.lineSpace
    }

    fun getBottom(): Float{
        return (lines.size) * theme.fontSize + (lines.size - 1) * theme.lineSpace
    }

    override fun initPaint() {
        val font = ResourcesCompat.getFont(view.context, R.font.monaco)
        paint.typeface = font
        paint.textSize = theme.fontSize
        paint.isAntiAlias = true
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.color = theme.primaryTextColor
    }

    fun cutLines() {
        if (width == 0 || width == lastWidth)
            return
        if (width != lastWidth) {
            lines.clear()
            line.clear()
            nextLine.clear()
            lastLineEnd = 0
            lastSpace = -1
            lastWidth = width
        }
        addText(text)
    }


    private fun notSplitAddChar(c: Char, nowAt: Int): Boolean {
        return if (c == '\n') {
            lines.add(line.toString())
            lastLineEnd = nowAt + 1
            line.clear()
            true
        } else if (paint.measureText(line.toString()) > width - padding * 2) {
            lastLineEnd = if (c == ' ' || c == '\n') {
                nowAt + 1
            } else {
                nextLine.append(c)
                line.deleteCharAt(line.length - 1)
                nowAt + 1
            }
            lines.add(line.toString())
            val temp = line
            line = nextLine
            nextLine = temp
            nextLine.clear()
            true
        } else {
            false
        }
    }

    private fun splitAddChar(c: Char, nowAt: Int): Boolean {
        return if (c == '\n') {
            lines.add(line.toString())
            lastLineEnd = nowAt + 1
            line.clear()
            true
        } else if (paint.measureText(line.toString()) > width - padding * 2) {
            if (c == ' ' || c == '\n') {
                lastLineEnd = nowAt + 1
            } else if (lastSpace > lastLineEnd) {
                nextLine.append(line.substring(lastSpace - lastLineEnd + 1, line.length))
                line.delete(lastSpace - lastLineEnd, line.length)
                lastLineEnd = lastSpace + 1
            } else {
                if (c != ' ' && c != '\n')
                    nextLine.append(c)
                line.deleteCharAt(line.length - 1)
                lastLineEnd = nowAt + 1
            }
            lines.add(line.toString())
            val temp = line
            line = nextLine
            nextLine = temp
            nextLine.clear()
            true
        } else {
            false
        }
    }


    private fun addChar(c: Char, nowAt: Int): Boolean {
        if (c == ' ')
            lastSpace = nowAt
        line.append(c)
        return if (splitText)
            splitAddChar(c, nowAt)
        else
            notSplitAddChar(c, nowAt)
    }


    fun addText(text: String) {

        this.text += text
        var nowAt = 0

        if (line.isNotEmpty()) {
            if (lines.size > 0)
                lines.removeAt(lines.size - 1)
            while (nowAt < text.length) {
                val c = text[nowAt]
                if (addChar(c, nowAt)) {
                    nowAt++
                    break
                }
                nowAt++
            }
        }

        lastWidth = width
        line.clear()

        while (nowAt < text.length) {
            val c = text[nowAt]
            addChar(c, nowAt)
            nowAt++
        }

        if (line.isNotEmpty()) {
            lines.add(line.toString())
            line.append('.')
            if (paint.measureText(line.toString()) > width - padding * 2){
                lines.add("")
                line.clear()
            }else{
                line.deleteCharAt(line.length - 1)
            }
        }else{
            lines.add("")
        }

        this.cursor?.flash()
    }


    override fun updateTheme(theme: TerminalTheme) {
        paint.color = theme.primaryTextColor
    }


    override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "draw text: lines: ${lines.size}}")
        lines.forEachIndexed { index, it ->
            canvas.drawText(it, padding, getLineBottom(index), paint)
        }
    }
}

class BackGround(view: TerminalView) : Drawable(view) {
    override fun updateTheme(theme: TerminalTheme) {
        paint.color = theme.backGroundColor
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(theme.backGroundColor)
    }
}