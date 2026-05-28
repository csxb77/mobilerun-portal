package com.mobilerun.portal.ui.taskprompt

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mobilerun.portal.R

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var counts: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    private val accentColor = ContextCompat.getColor(context, R.color.task_prompt_accent)
    private val mutedColor = ContextCompat.getColor(context, R.color.text_gray)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accentColor
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.task_prompt_stroke)
        strokeWidth = 1f * resources.displayMetrics.density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedColor
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accentColor
    }

    private val linePath = Path()
    private val fillPath = Path()
    private val density = resources.displayMetrics.density

    fun setData(counts: List<Int>, labels: List<String>) {
        this.counts = counts
        this.labels = labels
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (130 * density).toInt()
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (counts.isEmpty()) return

        val count = counts.size
        val labelHeight = 20f * density
        val chartLeft = paddingLeft.toFloat()
        val chartRight = (width - paddingRight).toFloat()
        val chartTop = paddingTop + 8f * density
        val chartBottom = height - paddingBottom - labelHeight
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return

        val maxCount = counts.max().coerceAtLeast(1)
        val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

        // Build points
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        for (i in 0 until count) {
            xs[i] = chartLeft + i * stepX
            ys[i] = chartTop + chartHeight - (counts[i].toFloat() / maxCount * chartHeight)
        }

        // Draw baseline grid line
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, gridPaint)

        // Build smooth line path using cubic bezier
        linePath.reset()
        fillPath.reset()
        linePath.moveTo(xs[0], ys[0])
        fillPath.moveTo(xs[0], chartBottom)
        fillPath.lineTo(xs[0], ys[0])

        for (i in 1 until count) {
            val cx = (xs[i - 1] + xs[i]) / 2f
            linePath.cubicTo(cx, ys[i - 1], cx, ys[i], xs[i], ys[i])
            fillPath.cubicTo(cx, ys[i - 1], cx, ys[i], xs[i], ys[i])
        }

        fillPath.lineTo(xs[count - 1], chartBottom)
        fillPath.close()

        // Gradient fill
        fillPaint.shader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            (accentColor and 0x00FFFFFF) or 0x59000000,
            (accentColor and 0x00FFFFFF) or 0x00000000,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Draw dots at each data point
        val dotRadius = 3f * density
        for (i in 0 until count) {
            if (counts[i] > 0) {
                canvas.drawCircle(xs[i], ys[i], dotRadius, dotPaint)
            }
        }

        // Draw date labels (start, middle, end)
        if (labels.size >= count && count >= 3) {
            val labelY = height - paddingBottom.toFloat()
            val indicesToDraw = listOf(0, count / 2, count - 1)
            for (i in indicesToDraw) {
                labelPaint.textAlign = when (i) {
                    0 -> Paint.Align.LEFT
                    count - 1 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(labels[i], xs[i], labelY, labelPaint)
            }
        }
    }
}
