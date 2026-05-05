package com.hebrewclock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnTimeChangedListener {
        fun onTimeChanged(hour: Int, minute: Int)
    }

    var onTimeChangedListener: OnTimeChangedListener? = null

    // Current time state (0–11 hours, 0–59 minutes)
    var currentHour: Int = 0
        private set
    var currentMinute: Int = 0
        private set

    // Dragging state
    private enum class DragTarget { NONE, HOUR, MINUTE }
    private var dragTarget = DragTarget.NONE

    // Paint objects
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val faceGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    // Colors
    private val colorFace = Color.parseColor("#FFF9E6")
    private val colorRim = Color.parseColor("#FF8C42")
    private val colorRimInner = Color.parseColor("#FFBB73")
    private val colorHourHand = Color.parseColor("#E63946")
    private val colorMinuteHand = Color.parseColor("#457B9D")
    private val colorCenter = Color.parseColor("#2D3142")
    private val colorCenterInner = Color.parseColor("#FFFFFF")
    private val colorTick = Color.parseColor("#BFA07A")
    private val colorHourTick = Color.parseColor("#8B5E3C")
    private val colorNumber = Color.parseColor("#3D2B1F")
    private val colorShadow = Color.parseColor("#40000000")

    // Geometry (computed in onSizeChanged)
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    // Paths for clock hands
    private val hourHandPath = Path()
    private val minuteHandPath = Path()

    // Animation
    private var glowAnimator: ValueAnimator? = null
    private var glowAlpha = 0f

    // Which hand is highlighted (just dragged)
    private var highlightedHand = DragTarget.NONE

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = minOf(w, h) / 2f * 0.92f

        // Update shader for face gradient
        faceGradientPaint.shader = RadialGradient(
            cx, cy * 0.85f,
            radius,
            intArrayOf(Color.WHITE, colorFace, Color.parseColor("#FFE5B4")),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun setTime(hour: Int, minute: Int) {
        currentHour = hour % 12
        currentMinute = minute.coerceIn(0, 59)
        invalidate()
        onTimeChangedListener?.onTimeChanged(currentHour, currentMinute)
    }

    fun animateToTime(hour: Int, minute: Int) {
        val targetHour = hour % 12
        val targetMinute = minute.coerceIn(0, 59)

        val startHourAngle = hourAngleDeg(currentHour, currentMinute)
        val startMinuteAngle = minuteAngleDeg(currentMinute)
        val endHourAngle = hourAngleDeg(targetHour, targetMinute)
        val endMinuteAngle = minuteAngleDeg(targetMinute)

        // Find shortest rotation direction
        var dh = (endHourAngle - startHourAngle + 360) % 360
        if (dh > 180) dh -= 360
        var dm = (endMinuteAngle - startMinuteAngle + 360) % 360
        if (dm > 180) dm -= 360

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val ha = (startHourAngle + dh * t + 360) % 360
                val ma = (startMinuteAngle + dm * t + 360) % 360
                currentHour = ((ha / 30f).toInt()) % 12
                currentMinute = ((ma / 6f).toInt()) % 60
                invalidate()
            }
        }
        animator.start()

        // Final snap
        postDelayed({
            currentHour = targetHour
            currentMinute = targetMinute
            invalidate()
            onTimeChangedListener?.onTimeChanged(currentHour, currentMinute)
        }, 620)
    }

    private fun hourAngleDeg(hour: Int, minute: Int): Float =
        (hour % 12) * 30f + minute * 0.5f

    private fun minuteAngleDeg(minute: Int): Float =
        minute * 6f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawRim(canvas)
        drawFace(canvas)
        drawTicks(canvas)
        drawNumbers(canvas)
        drawHands(canvas)
        drawCenter(canvas)
    }

    private fun drawRim(canvas: Canvas) {
        // Outer glow/shadow
        shadowPaint.color = colorShadow
        canvas.drawCircle(cx, cy + 6f, radius + 14f, shadowPaint)

        // Outer rim gradient
        val rimWidth = radius * 0.07f
        rimPaint.strokeWidth = rimWidth
        rimPaint.color = colorRim
        rimPaint.shader = SweepGradient(
            cx, cy,
            intArrayOf(
                Color.parseColor("#FF6B35"),
                Color.parseColor("#FFD166"),
                Color.parseColor("#FF6B35"),
                Color.parseColor("#FF8C42"),
                Color.parseColor("#FF6B35")
            ),
            null
        )
        canvas.drawCircle(cx, cy, radius - rimWidth / 2, rimPaint)
        rimPaint.shader = null

        // Inner rim highlight
        rimPaint.strokeWidth = rimWidth * 0.3f
        rimPaint.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(cx, cy, radius - rimWidth * 1.1f, rimPaint)
    }

    private fun drawFace(canvas: Canvas) {
        facePaint.color = colorFace
        val faceR = radius * 0.88f
        canvas.drawCircle(cx, cy, faceR, facePaint)
        canvas.drawCircle(cx, cy, faceR, faceGradientPaint)
    }

    private fun drawTicks(canvas: Canvas) {
        val outerR = radius * 0.83f
        for (i in 0 until 60) {
            val angleDeg = i * 6f - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cos = cos(angleRad).toFloat()
            val sin = sin(angleRad).toFloat()

            if (i % 5 == 0) {
                // Hour tick - long and thick
                val innerR = outerR * 0.87f
                tickPaint.strokeWidth = radius * 0.025f
                tickPaint.color = colorHourTick
                canvas.drawLine(
                    cx + innerR * cos, cy + innerR * sin,
                    cx + outerR * cos, cy + outerR * sin,
                    tickPaint
                )
            } else {
                // Minute tick - short and thin
                val innerR = outerR * 0.93f
                tickPaint.strokeWidth = radius * 0.012f
                tickPaint.color = colorTick
                canvas.drawLine(
                    cx + innerR * cos, cy + innerR * sin,
                    cx + outerR * cos, cy + outerR * sin,
                    tickPaint
                )
            }
        }
    }

    private fun drawNumbers(canvas: Canvas) {
        numberPaint.textSize = radius * 0.18f
        numberPaint.color = colorNumber
        val numR = radius * 0.67f

        for (i in 1..12) {
            val angleDeg = i * 30f - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = cx + numR * cos(angleRad).toFloat()
            val y = cy + numR * sin(angleRad).toFloat() - (numberPaint.ascent() + numberPaint.descent()) / 2f
            canvas.drawText(i.toString(), x, y, numberPaint)
        }
    }

    private fun buildHandPath(path: Path, length: Float, width: Float, backLength: Float) {
        path.reset()
        path.moveTo(0f, -length)
        path.cubicTo(width * 0.6f, -length * 0.6f, width, -backLength, width * 0.5f, backLength)
        path.lineTo(0f, backLength * 1.2f)
        path.lineTo(-width * 0.5f, backLength)
        path.cubicTo(-width, -backLength, -width * 0.6f, -length * 0.6f, 0f, -length)
        path.close()
    }

    private fun drawHands(canvas: Canvas) {
        // ---- Hour hand ----
        val hAngle = hourAngleDeg(currentHour, currentMinute)
        val hourLen = radius * 0.50f
        val hourWidth = radius * 0.07f

        buildHandPath(hourHandPath, hourLen, hourWidth, radius * 0.12f)

        val hm = Matrix()
        hm.postRotate(hAngle)
        hm.postTranslate(cx, cy)

        // Drop shadow
        shadowPaint.color = Color.parseColor("#40000000")
        val shadowPath = Path()
        hourHandPath.transform(hm, shadowPath)
        val shadowMatrix = Matrix()
        shadowMatrix.postTranslate(3f, 5f)
        shadowPath.transform(shadowMatrix)
        canvas.drawPath(shadowPath, shadowPaint)

        // Hand fill
        hourHandPaint.color = colorHourHand
        if (highlightedHand == DragTarget.HOUR) {
            hourHandPaint.color = Color.parseColor("#FF1744")
        }
        val hPath = Path()
        hourHandPath.transform(hm, hPath)
        canvas.drawPath(hPath, hourHandPaint)

        // Hand shine
        val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#50FFFFFF")
        }
        val shineMatrix = Matrix()
        shineMatrix.postScale(0.35f, 0.95f, 0f, 0f)
        shineMatrix.postRotate(hAngle)
        shineMatrix.postTranslate(cx, cy)
        val shinePath = Path()
        hourHandPath.transform(shineMatrix, shinePath)
        canvas.drawPath(shinePath, shinePaint)

        // ---- Minute hand ----
        val mAngle = minuteAngleDeg(currentMinute)
        val minLen = radius * 0.72f
        val minWidth = radius * 0.05f

        buildHandPath(minuteHandPath, minLen, minWidth, radius * 0.14f)

        val mm = Matrix()
        mm.postRotate(mAngle)
        mm.postTranslate(cx, cy)

        val mShadowPath = Path()
        minuteHandPath.transform(mm, mShadowPath)
        mShadowPath.transform(shadowMatrix)
        canvas.drawPath(mShadowPath, shadowPaint)

        minuteHandPaint.color = colorMinuteHand
        if (highlightedHand == DragTarget.MINUTE) {
            minuteHandPaint.color = Color.parseColor("#0077B6")
        }
        val mPath = Path()
        minuteHandPath.transform(mm, mPath)
        canvas.drawPath(mPath, minuteHandPaint)

        val shineMatrix2 = Matrix()
        shineMatrix2.postScale(0.35f, 0.95f, 0f, 0f)
        shineMatrix2.postRotate(mAngle)
        shineMatrix2.postTranslate(cx, cy)
        val shinePath2 = Path()
        minuteHandPath.transform(shineMatrix2, shinePath2)
        canvas.drawPath(shinePath2, shinePaint)
    }

    private fun drawCenter(canvas: Canvas) {
        centerPaint.color = colorCenter
        canvas.drawCircle(cx, cy, radius * 0.07f, centerPaint)
        centerInnerPaint.color = colorCenterInner
        canvas.drawCircle(cx, cy, radius * 0.035f, centerInnerPaint)
    }

    // ---- Touch / dragging ----

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tx = event.x
        val ty = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragTarget = identifyTarget(tx, ty)
                if (dragTarget != DragTarget.NONE) {
                    highlightedHand = dragTarget
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragTarget != DragTarget.NONE) {
                    val angle = angleDeg(tx - cx, ty - cy)
                    when (dragTarget) {
                        DragTarget.HOUR -> {
                            // Each 30° = 1 hour
                            val newHour = ((angle / 30f).toInt() + 12) % 12
                            if (newHour != currentHour) {
                                currentHour = newHour
                                invalidate()
                                onTimeChangedListener?.onTimeChanged(currentHour, currentMinute)
                            }
                        }
                        DragTarget.MINUTE -> {
                            // Each 6° = 1 minute
                            val newMinute = ((angle / 6f).toInt() + 60) % 60
                            if (newMinute != currentMinute) {
                                currentMinute = newMinute
                                invalidate()
                                onTimeChangedListener?.onTimeChanged(currentHour, currentMinute)
                            }
                        }
                        else -> {}
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragTarget = DragTarget.NONE
                highlightedHand = DragTarget.NONE
                invalidate()
            }
        }
        return false
    }

    private fun identifyTarget(tx: Float, ty: Float): DragTarget {
        val dx = tx - cx
        val dy = ty - cy
        val touchAngle = angleDeg(dx, dy)
        val touchDist = sqrt(dx * dx + dy * dy)

        val mAngle = minuteAngleDeg(currentMinute)
        val hAngle = hourAngleDeg(currentHour, currentMinute)

        // Check if touch is near a hand (angle-wise and distance-wise)
        val minHandLen = radius * 0.72f
        val hourHandLen = radius * 0.50f
        val tolerance = 25f // degrees

        val dMin = angleDiff(touchAngle, mAngle)
        val dHour = angleDiff(touchAngle, hAngle)

        val hitMinute = dMin < tolerance && touchDist < minHandLen * 1.1f && touchDist > radius * 0.08f
        val hitHour = dHour < tolerance && touchDist < hourHandLen * 1.1f && touchDist > radius * 0.08f

        return when {
            hitMinute && hitHour -> if (dMin < dHour) DragTarget.MINUTE else DragTarget.HOUR
            hitMinute -> DragTarget.MINUTE
            hitHour -> DragTarget.HOUR
            else -> DragTarget.NONE
        }
    }

    // Returns angle in degrees (0 = up/12 o'clock, clockwise)
    private fun angleDeg(dx: Float, dy: Float): Float {
        var a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
        if (a < 0) a += 360f
        if (a >= 360f) a -= 360f
        return a
    }

    private fun angleDiff(a: Float, b: Float): Float {
        var d = abs(a - b) % 360f
        if (d > 180f) d = 360f - d
        return d
    }
}
