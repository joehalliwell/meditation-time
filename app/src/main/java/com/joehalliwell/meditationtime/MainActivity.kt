package com.joehalliwell.meditationtime

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity(), TimerTouchListener, Runnable {

    val TAG = "MeditationTime"

    private var maximumDuration = 60L * 1000 * 60
    private var runOn = true

    private var _duration = 0L
    private var _segments = 4

    private var _start = 0L
    private var _elapsed = 0L
    private var _stage = -1

    private lateinit var timerView: TimerView
    private lateinit var textView: TextView
    private lateinit var preferences: SharedPreferences
    private lateinit var soundPool: SoundPool
    private val soundBank = HashMap<Int, Int>()

    val timerHandler = Handler()

    var duration: Long
        get() = (_duration)
        set(value) {
            _duration = value
            updateViews()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.textView)

        timerView = findViewById<TimerView>(R.id.timerView)
        timerView.setListener(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        soundPool = SoundPool.Builder().build()

        for (resId in arrayOf(R.raw.start, R.raw.mid, R.raw.end, R.raw.post)) {
            soundBank[resId] = soundPool.load(this, resId, 1)
        }



        try {
            duration = preferences.getLong("duration", maximumDuration / 3)
        }
        catch (ex: ClassCastException) {
            duration = maximumDuration / 3
        }
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

    private fun isRunning(): Boolean = _start > 0

    fun start(view: View) {
        if (isRunning()) return
        _start = System.currentTimeMillis()
        _stage = -1
        val editor: Editor = preferences.edit()
        editor.putLong("duration", duration)
        editor.commit()
        timerHandler.postDelayed(this, 0)
    }

    fun stop(view: View) {
        timerHandler.removeCallbacks(this)
        _start = 0
        _elapsed = 0
        updateViews()
    }

    override fun onTimerTouch(position: Float) {
        Log.i(TAG, "Got touch at " + position)

        if (isRunning()) return

        var d = position
        if (d <= 0) d+= 1
        duration = (maximumDuration * d).roundToLong()
        Log.i(TAG, "Duration: " + duration)
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
        if (_stage == _segments && !runOn) stop(this.textView)

        updateViews()

        timerHandler.postDelayed(this, 100)
    }


    private fun updateViews() {
        timerView?.duration = _duration.toFloat() / maximumDuration
        timerView?.elapsed = _elapsed.toFloat() / maximumDuration
        textView.setText(getHoursAndMinutes(_duration - _elapsed))
    }

    private fun getHoursAndMinutes(elapsed: Long): String {
        val minutes = elapsed / 60000
        val seconds = (elapsed % 60000) / 1000
        return String.format("%02d:%02d",  minutes, seconds)
    }

    private fun playSoundForStage(stage: Int) {
        Log.i(TAG, "Playing sound for stage " + stage)
        if (stage==0) playSound(R.raw.start)
        else if (stage>0 && stage<_segments) playSound(R.raw.mid)
        else if (stage == _segments) playSound(R.raw.end)
        else playSound(R.raw.post)
    }

    private fun playSound(resId: Int) {
        Log.i(TAG, "Playing sound: " + resId)
        soundBank.get(resId)?.let {
            soundPool.play(it, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }
}
