package com.sensorpic.demo.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.sensorpic.demo.R
import timber.log.Timber

class CameraCoverView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private lateinit var extraCanvas: Canvas
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
    private var rectangle: RectF? = null
//    private var rectangle2 = RectF(0f, 0f, 0f, 0f)
    private var isBlockingWhole = false
    private var imageIsPortrait = true
    private var blocked: Boolean? = false
    private var recognitionCategory: String? = null
    private var recognitionProbability: Int? = null
    private val paintStrokeWidth = 12f // has to be float
    private lateinit var wholeScreenRect: RectF
//    private val outline = RectF(0f, 0f, 600f, 600f)

    private val paintOutline = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE // Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = paintStrokeWidth // default: Hairline-width (really thin)
    }

    private val paintSolid = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL_AND_STROKE // Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.MITER // default: MITER
        strokeCap = Paint.Cap.BUTT // default: BUTT
        strokeWidth = paintStrokeWidth // default: Hairline-width (really thin)
    }

    private val paintTextRed = Paint().apply {
        textSize = 96f
        color = Color.RED
        isAntiAlias = true
        isDither = true
    }

    private val paintTextAmber = Paint().apply {
        textSize = 96f
        color = Color.YELLOW
        isAntiAlias = true
        isDither = true
    }

    private val paintTextGreen = Paint().apply {
        textSize = 96f
        color = Color.GREEN
        isAntiAlias = true
        isDither = true
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        wholeScreenRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(bitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isBlockingWhole) {
            canvas.drawRect(wholeScreenRect, paintSolid)
//        } else {
//            canvas.drawRect(rectangle2, paintOutline)
//            rectangle2.left = rectangle?.left ?: 0f * 1
//            rectangle2.top = rectangle?.top ?: 0f * 1
//            rectangle2.right = (rectangle?.right ?: 0f) * 2
//            rectangle2.bottom = (rectangle?.bottom ?: 0f) * 2
//
//            Timber.i("XXX x: ${rectangle?.right} ${rectangle2.right}")
        }
        val summary = when (blocked) {
            true -> "${recognitionCategory?.capitalize()} $recognitionProbability% BLOCKED"
            null -> "${recognitionCategory?.capitalize()} $recognitionProbability%"
            false -> "OK"
        }

        val paint = when (blocked) {
            true -> paintTextRed
            null -> paintTextAmber
            false -> paintTextGreen
        }
        canvas.drawText(summary, 32f, 128f, paint)
    }

    fun coverRectangle(rectangle: RectF, isPortrait: Boolean) {
        isBlockingWhole = false
        this.rectangle = rectangle
        this.imageIsPortrait = isPortrait
        invalidate()
    }

    fun showBlockedSummary(blockWhole: Boolean, title: String, probability100: Int) {
        Timber.i("Showing blocked summary")
        isBlockingWhole = blockWhole
        blocked = true
        recognitionProbability = probability100
        recognitionCategory = title
        invalidate()
    }

    fun showAlmostBlockedSummary(title: String, probability100: Int) {
        blocked = null
        recognitionProbability = probability100
        recognitionCategory = title
        invalidate()
    }

    fun showNotBlockedSummary() {
        blocked = false
        recognitionProbability = null
        recognitionCategory = null
        invalidate()
    }

    fun clearCover() {
        isBlockingWhole = false
        rectangle = null
        invalidate()
    }

    /// Private methods

    private fun createInitialRectangle(): RectF {
        return RectF(0f, 0f, 0f, 0f)
    }

}
