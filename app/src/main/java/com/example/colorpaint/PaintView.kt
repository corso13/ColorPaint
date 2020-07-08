package com.example.colorpaint

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View

class PaintView : View  {
    companion object {
        const val TYPE_MARKER = 2
        const val TYPE_PENCIL = 1
        const val TYPE_PEN = 0

        private const val DEFAULT_BRUSH_SIZE = 15f
        private const val DEFAULT_BRUSH_COLOR = Color.BLACK
        private const val DEFAULT_BG_COLOR = Color.WHITE
        private const val DEFAULT_ALPHA = 255
        private const val MARKER_ALPHA = 175
    }

    private var mPaint = Paint()
    private var mCanvas = Canvas()
    private val mBitmapPaint = Paint()
    private var mPath = Path()
    private var mPathHistory = mutableListOf<Stroke>()
    private var mRedoHistory = mutableListOf<Stroke>()
    lateinit var mBitmap : Bitmap

    var brushSize = DEFAULT_BRUSH_SIZE
        set(size) {
            field = size
            mPaint.strokeWidth = size
        }
    var brushColor = DEFAULT_BRUSH_COLOR
        set(color) {
            field = color
            mPaint.color = color
        }
    var brushType = TYPE_PEN
        set(type) {
            if (type in 0..2) field = type
        }

    constructor(context: Context) : super(context, null) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        // init paint settings
        mPaint.color = DEFAULT_BRUSH_COLOR
        mPaint.strokeWidth = DEFAULT_BRUSH_SIZE
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.alpha = DEFAULT_ALPHA
    }

    fun initDisplay(displayMetrics: DisplayMetrics) {
        // init canvas to screen size
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()

        mCanvas.drawColor(DEFAULT_BG_COLOR)

        for (stroke in mPathHistory) {
            mPaint.color = stroke.color
            mPaint.strokeWidth = stroke.size

            if (stroke.type == TYPE_MARKER) {
                mPaint.alpha = MARKER_ALPHA
            } else {
                mPaint.alpha = DEFAULT_ALPHA
            }

            mPaint.maskFilter = stroke.mask
            mCanvas.drawPath(stroke.path, mPaint)
        }

        canvas.drawBitmap(mBitmap, 0f,0f, mBitmapPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val xPos = event.x
        val yPos = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // create new brush stroke
                mPath = Path()
                var mStroke = Stroke(brushSize, brushColor, mPath, brushType)

                // add stroke to undo/redo history
                mPathHistory.add(mStroke)
                mRedoHistory.clear()

                mPath.moveTo(xPos, yPos)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                mPath.lineTo(xPos, yPos)
            }
            else -> return false
        }
        postInvalidateDelayed(15)
        return false
    }

    fun undo() {
        if (!mPathHistory.isEmpty()) {
            val stroke = mPathHistory.last()
            mPathHistory.remove(stroke)
            mRedoHistory.add(stroke)
        }
        invalidate()
    }

    fun redo() {
        if (!mRedoHistory.isEmpty()) {
            val stroke = mRedoHistory.last()
            mRedoHistory.remove(stroke)
            mPathHistory.add(stroke)
        }
        invalidate()
    }

    fun clear() {
        mRedoHistory.clear()
        mPathHistory.clear()
        invalidate()
    }

    class Stroke(size: Float, color: Int, path: Path, type: Int) {
        val size = size
        val color = color
        val path = path
        val type = type
        var mask : BlurMaskFilter? = null

        init {
            if (type == TYPE_PENCIL) {
                mask = BlurMaskFilter(size / 4f, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }
}