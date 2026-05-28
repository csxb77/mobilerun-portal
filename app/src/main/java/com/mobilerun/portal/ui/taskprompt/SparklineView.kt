package com.mobilerun.portal.ui.taskprompt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mobilerun.portal.R

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var data: List<Int> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.task_prompt_accent)
    }

    private val emptyBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.stroke_gray)
    }

    private val rect = RectF()
    private val density = resources.displayMetrics.density
    private val barCornerRadius = 4f * density
    private val gapPx = 4f * density
    private val minBarHeight = 2f * density

    fun setData(counts: List<Int>) {
        data = counts
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (80 * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val count = data.size
        val totalGap = gapPx * (count - 1)
        val barWidth = (width - totalGap - paddingLeft - paddingRight) / count
        if (barWidth <= 0) return

        val drawableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (drawableHeight <= 0f) return
        val maxCount = data.max().coerceAtLeast(1)

        for (i in data.indices) {
            val x = paddingLeft + i * (barWidth + gapPx)
            val value = data[i]

            if (value == 0) {
                val top = paddingTop + drawableHeight - minBarHeight
                rect.set(x, top, x + barWidth, paddingTop + drawableHeight)
                canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, emptyBarPaint)
            } else {
                val barHeight = (value.toFloat() / maxCount * drawableHeight)
                    .coerceAtLeast(minBarHeight)
                val top = paddingTop + drawableHeight - barHeight
                rect.set(x, top, x + barWidth, paddingTop + drawableHeight)
                canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, barPaint)
            }
        }
    }
}
