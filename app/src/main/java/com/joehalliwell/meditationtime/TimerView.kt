package com.joehalliwell.meditationtime

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.math.MathUtils


/**
 * TODO: document your custom view class.
 */
class TimerView : View {

    private val TAG = "TimerView"

    // Pre-allocated view stuff
    private var _clockRect: RectF = RectF(0f, 0f, 100f, 100f)
    private lateinit var _path: Path
    private lateinit var _painter: Paint

    private var _listener: TimerTouchListener? = null
    private var _duration = 1.0f / 3
    private var _elapsed = 0f

    @ColorInt private var _bgColor: Int = resources.getColor(R.color.clockface)
    @ColorInt private var _pointerColor = resources.getColor(R.color.detail)
    @ColorInt private var _durColor = resources.getColor(R.color.timeTotal)
    @ColorInt private var _remainingColor = resources.getColor(R.color.timeRemaining)

    var duration: Float
        get() = _duration
        set(value) {
            _duration = MathUtils.clamp(value, -1.0f, 1.0f)
            invalidate()
        }

    var elapsed: Float
        get() = _elapsed
        set(value) {
            _elapsed = value
            invalidate()
        }


    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    fun setListener(listener: TimerTouchListener) {
        this._listener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val relX = event.x - width / 2
                val relY = height / 2 - event.y
                val quantize = 1.0f
                val angle =
                    quantize * Math.round(Math.toDegrees(Math.atan2(relX.toDouble(), relY.toDouble())) / quantize)
                Log.i(TAG, "Angle " + angle)
                _listener?.onTimerTouch(angle/360)
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Force 1:1 aspect ratio
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (width > height) height else width
        Log.i(TAG, "Size: %d".format(size))
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        _clockRect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - (paddingLeft + paddingRight)).toFloat(),
            (height - (paddingTop + paddingBottom)).toFloat()
        )
        val width = 0.05f * _clockRect.width() / 2
        val height = 0.999f * _clockRect.height() / 2
        _path = Path()
        _path.moveTo(-width, 0f)
        _path.lineTo(0f, -height)
        _path.lineTo(width, 0f)
        //_path.arcTo(-width, -width, width, width, 0f, 180f, false)
        _path.close()

        Log.i(TAG, "BBOX: %s".format(_clockRect.toString()))
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.TimerView, defStyle, 0
        )


        _painter = Paint(ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = 10f
        }

        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        _painter.color = _bgColor //Color.valueOf(0.2f, 0.2f, 0.2f, 1.0f).toArgb()
        canvas.drawArc(_clockRect, 0f, 360f, true, _painter)
        _painter.color = _durColor
        canvas.drawArc(_clockRect, -90f, 360 * duration, true, _painter)
        _painter.color = _remainingColor
        canvas.drawArc(_clockRect, -90f, 360 * (duration - elapsed), true, _painter)


        _painter.color = _pointerColor

        canvas.save()
        canvas.translate(_clockRect.centerX(), _clockRect.centerY())
        canvas.drawCircle(0f, 0f, _clockRect.width() / 10, _painter)
        canvas.rotate( 360 * (duration - elapsed))
        canvas.drawPath(_path, _painter)
        canvas.restore()
    }
}
