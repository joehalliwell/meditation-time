package com.joehalliwell.meditationtime

import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlin.math.roundToLong
import android.view.WindowManager


class MainActivity : BaseActivity(), TimerViewListener, Runnable {

    private var _duration = 0L
    private var _segments = 4

    private var _start = 0L
    private var _elapsed = 0L
    private var _stage = -1

    private lateinit var timerView: TimerView
    private lateinit var timerTextView: TextView
    private lateinit var extraTimeTextView: TextView
    private lateinit var _pauseOverlay: Drawable
    private lateinit var _playOverlay: Drawable

    private var maximumDuration = 60L * 1000 * 60

    private lateinit var soundPool: SoundPool
    private val soundBank = HashMap<Int, Int>()

    private val timerHandler = Handler()

    var duration: Long
        get() = (_duration)
        set(value) {
            _duration = value
            updateViews()
        }

    val runOn: Boolean
        get() {
            return preferences.getBoolean(
                resources.getString(R.string.run_on_pk),
                resources.getBoolean(R.bool.run_on_default)
            )
        }

    val intervalBells: Boolean
        get() {
            return preferences.getBoolean(
                resources.getString(R.string.interval_bells_pk),
                resources.getBoolean(R.bool.interval_bells_default)
            )
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Creating " + this)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.main_activity)

        timerTextView = findViewById<TextView>(R.id.timeTextView)
        extraTimeTextView = findViewById<TextView>(R.id.extraTimeTextView)
        timerView = findViewById<TimerView>(R.id.timerView)
        timerView.setListener(this)

        _pauseOverlay = resources.getDrawable(R.drawable.ic_pause_black_24dp, null).mutate()
        _playOverlay = resources.getDrawable(R.drawable.ic_play_arrow_black_24dp, null).mutate()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .build()
        for (resId in arrayOf(R.raw.start, R.raw.mid, R.raw.end, R.raw.post)) {
            soundBank[resId] = soundPool.load(this, resId, 1)
        }

        try {
            _duration = preferences.getLong(getString(R.string.duration_pk), maximumDuration / 3)
            _elapsed = preferences.getLong(getString(R.string.elapsed_pk), 0L)
            _stage = preferences.getInt(getString(R.string.stage_pk), 0)
        } catch (ex: ClassCastException) {
            ex.printStackTrace()
        }
        if (_stage > -1) start()
        updateViews()
        super.onStart()
    }

    override fun onStop() {
        preferences.edit().apply() {
            putLong(getString(R.string.duration_pk), _duration)
            putLong(getString(R.string.elapsed_pk), _elapsed)
            putInt(getString(R.string.stage_pk), _stage)
            commit()
        }
        super.onStop()
    }

    override fun onDestroy() {
        soundPool.release()
        return super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Settings")
        return when (item.itemId) {
            R.id.settings_button -> {
                val intent = Intent(this, PreferencesActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onDialTouch(angle: Double): Boolean {
        // Only permit a change when stopped
        if (isRunning() || _elapsed > 0) return false

        val quantize = 1.0 / 60.0;
        var quantized = Math.round(angle / quantize) * quantize
        if (quantized < 0) quantized += 1

        duration = (maximumDuration * quantized).roundToLong()
        Log.i(TAG, "Duration: " + duration)
        return true
    }

    override fun onHubTouch() {
        if (!isRunning()) start()
        else pause()
    }

    private fun isRunning(): Boolean = _start > 0

    fun start() {
        if (isRunning()) return
        _start = System.currentTimeMillis()
        timerHandler.postDelayed(this, 0)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun reset() {
        _stage = -1
        _elapsed = 0
        pause()
    }

    fun pause() {
        _start = 0
        timerHandler.removeCallbacks(this)
        updateViews()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    fun resetClickHandler(view: View) = reset()
    fun settingsClickHandler(view: View) {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }

    override fun run() {
        if (!isRunning()) return

        val now = System.currentTimeMillis()
        _elapsed += now - _start
        _start = now

        var targetStage = (_elapsed * _segments / _duration).toInt()
        if (_stage != targetStage) {
            Log.i(TAG, "Switching to stage " + targetStage)
            _stage = targetStage
            playSoundForStage(targetStage)
        }
        if (_stage == _segments && !runOn) reset()

        updateViews()

        timerHandler.postDelayed(this, 100)
    }


    private fun updateViews() {
        // Update timerView
        timerView.apply {
            duration = _duration.toFloat() / maximumDuration
            elapsed = _elapsed.toFloat() / maximumDuration
            overlay = if (isRunning()) _pauseOverlay else _playOverlay
        }

        // Update textView
        timerTextView.text = getHoursAndMinutes(_duration, _elapsed)
        extraTimeTextView.text = if (_elapsed > _duration) "+" else ""
    }

    private fun getHoursAndMinutes(duration: Long, elapsed: Long): String {
        val t = Math.abs(duration - elapsed)
        val minutes = t / 60000
        val seconds = (t % 60000) / 1000
        return String.format(
            "%02d:%02d",
            minutes,
            seconds
        )
    }

    private fun playSoundForStage(stage: Int) {
        Log.i(TAG, "Playing sound for stage " + stage)
        if (stage == 0) playSound(R.raw.start)
        else if (stage == _segments) playSound(R.raw.end)

        if (!intervalBells) return

        if (stage > 0 && stage < _segments) {
            playSound(R.raw.mid)
        } else if (stage > _segments) {
            playSound(R.raw.post)
        }
    }

    private fun playSound(resId: Int) {
        Log.i(TAG, "Playing sound: " + resId)
        soundBank.get(resId)?.let {
            soundPool.play(it, 0.9f, 0.9f, 0, 0, 1.0f)
        }
    }


}
