package com.joehalliwell.meditationtime

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.drawable.Drawable
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
    private val HUB_RADIUS = 0.1f;

    // Pre-allocated view stuff
    private lateinit var _clockRect: RectF
    private val _pointerPath = Path()
    private val _tempPath = Path()
    private val _pointerTransform = Matrix()

    private lateinit var _painter: Paint

    private var _listener: TimerViewListener? = null
    private var _duration = 0.5f
    private var _elapsed = 0f



    private lateinit var _overlay: Drawable

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

    var overlay: Drawable
        get() = _overlay
        set(value) {
            _overlay = value
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

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.TimerView, defStyle, 0
        )
        a.recycle()

        _painter = Paint(ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = 10f
        }
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setListener(listener: TimerViewListener) {
        this._listener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val relX = event.x - _clockRect.centerX()
                val relY = _clockRect.centerY() - event.y
                val hub = HUB_RADIUS * width

                if (relX*relX + relY*relY < hub*hub) {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) _listener?.onHubTouch()
                    return super.onTouchEvent(event)
                }

                val quantize = 1.0f
                val angle =
                    quantize * Math.round(Math.toDegrees(Math.atan2(relX.toDouble(), relY.toDouble())) / quantize)
                Log.i(TAG, "Angle " + angle)

                // Gosh this is ugly!
                if (_listener!=null && !_listener!!.onDialTouch(angle / 360)) {
                        return true
                }
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
        //Log.i(TAG, "Size: %d".format(size))
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!changed) return

        Log.i(TAG, "Computing new layout")

        val size = Math.min(width, height)
        _clockRect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (size - (paddingLeft + paddingRight)).toFloat(),
            (size - (paddingTop + paddingBottom)).toFloat()
        )
        _clockRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val width = 0.05f * _clockRect.width() / 2
        val height = 0.999f * _clockRect.height() / 2
        _pointerPath.reset()
        _pointerPath.moveTo(-width, 0f)
        _pointerPath.lineTo(0f, -height)
        _pointerPath.lineTo(width, 0f)
        //_pointerPath.arcTo(-width, -width, width, width, 0f, 180f, false)
        _pointerPath.close()

        _pointerPath.addCircle(0f, 0f, HUB_RADIUS * size, Path.Direction.CW)
        _pointerPath.close()

        //Log.i(TAG, "BBOX: %s".format(_clockRect.toString()))
    }

    override fun onDraw(canvas: Canvas) {
        _painter.color = _bgColor //Color.valueOf(0.2f, 0.2f, 0.2f, 1.0f).toArgb()
        canvas.drawArc(_clockRect, 0f, 360f, true, _painter)
        _painter.color = _durColor
        canvas.drawArc(_clockRect, -90f, 360 * duration, true, _painter)
        _painter.color = _remainingColor
        canvas.drawArc(_clockRect, -90f, 360 * (duration - elapsed), true, _painter)


        _painter.color = _pointerColor
        _painter.setShadowLayer(8f, 4f, 4f, 0x80000000.toInt())
        _pointerTransform.reset()
        _pointerTransform.postRotate(360 * (duration - elapsed), 0f, 0f)
        _pointerTransform.postTranslate(_clockRect.centerX(), _clockRect.centerY())
        _pointerPath.transform(_pointerTransform, _tempPath)
        canvas.drawPath(_tempPath, _painter)
        _painter.clearShadowLayer()

        val hubRadius = HUB_RADIUS * _clockRect.width()
        _overlay.apply {
            setTint(_bgColor)
            setBounds(
                (_clockRect.centerX() - hubRadius).toInt(),
                (_clockRect.centerY() - hubRadius).toInt(),
                (_clockRect.centerX() + hubRadius).toInt(),
                (_clockRect.centerY() + hubRadius).toInt()
            )
            draw(canvas)
        }
    }
}
