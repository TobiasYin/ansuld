package com.asld.asld.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
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
    val fontSize: Float = 50f
)

class TerminalView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var draws = ArrayList<Drawable>()
    var theme = TerminalTheme()
        set(value) {
            draws.forEach { it.theme = value }
            field = value
        }

    private val cursor: Cursor
    private val text: Text
    private val backGround: BackGround

    init {
        backGround = newDrawable()
        text = newDrawable()
        cursor = newDrawable()
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

        text.addText(
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
        refreshDrawableState()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "measure: w: $measuredWidth, h:$measuredHeight")
        text.width = measuredWidth
        setMeasuredDimension(measuredWidth, ((text.lines.size + 1) * theme.fontSize).toInt())
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


    override fun onDraw(canvas: Canvas) {

    }
}

class Text(view: TerminalView) : Drawable(view) {
    private val splitText = false
    private var text = ""
    private var lastWidth = 0
    private var lastLineEnd = 0
    private var lastSpace = -1
    var lines = ArrayList<String>()
    var width = 0
        set(value) {
            var diff = value != field
            field = value
            if (diff)
                cutLines()
        }

    var line = StringBuilder()
    var nextLine = StringBuilder()

    override fun initPaint() {
        paint.textSize = theme.fontSize
        paint.isAntiAlias = true
        paint.flags = Paint.ANTI_ALIAS_FLAG
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
        } else if (paint.measureText(line.toString()) > width) {
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
        } else if (paint.measureText(line.toString()) > width) {
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
        lines.clear()

        while (nowAt < text.length) {
            val c = text[nowAt]
            addChar(c, nowAt)
            nowAt++
        }
        if (line.isNotEmpty()) {
            lines.add(line.toString())
        }
    }


    override fun updateTheme(theme: TerminalTheme) {
        paint.color = theme.primaryTextColor
    }


    override fun onDraw(canvas: Canvas) {
        val yOffset = 0f
        Log.d(TAG, "draw text: lines: ${lines.size}}")
        lines.forEachIndexed { index, it ->
            canvas.drawText(it, 10f, (index + 1) * theme.fontSize + yOffset, paint)
        }
    }
}

class BackGround(view: TerminalView) : Drawable(view) {
    override fun updateTheme(theme: TerminalTheme) {
        paint.color = theme.backGroundColor
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, view.width.toFloat(), view.height.toFloat(), paint)
    }
}