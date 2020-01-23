package com.joehalliwell.meditationtime

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.drawable.Drawable
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity(), TimerViewListener, Runnable {

    val TAG = "MeditationTime"

    private var maximumDuration = 60L * 1000 * 60
    private var runOn = true

    private var _duration = 0L
    private var _segments = 4

    private var _start = 0L
    private var _elapsed = 0L
    private var _stage = -1

    private lateinit var timerView: TimerView
    private lateinit var timerTextView: TextView
    private lateinit var extraTimeTextView: TextView
    private lateinit var preferences: SharedPreferences
    private lateinit var soundPool: SoundPool
    private val soundBank = HashMap<Int, Int>()
    private lateinit var _pauseOverlay: Drawable
    private lateinit var _playOverlay: Drawable

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

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Remove app name from action bar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        timerTextView = findViewById<TextView>(R.id.timeTextView)
        extraTimeTextView = findViewById<TextView>(R.id.extraTimeTextView)
        timerView = findViewById<TimerView>(R.id.timerView)
        timerView.setListener(this);

        _pauseOverlay = resources.getDrawable(R.drawable.ic_pause_black_24dp, null)
        _playOverlay = resources.getDrawable(R.drawable.ic_play_arrow_black_24dp, null)

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .build()
        for (resId in arrayOf(R.raw.start, R.raw.mid, R.raw.end, R.raw.post)) {
            soundBank[resId] = soundPool.load(this, resId, 1)
        }

        try {
            duration = preferences.getLong("duration", maximumDuration / 3)
        }
        catch (ex: ClassCastException) {
            duration = maximumDuration / 3
        }

        reset()
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
            // a general rule, you should design your app to hide the status bar whenever you
            // hide the navigation bar.
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onDialTouch(position: Float) {
        // Only permit a change when stopped
        if (isRunning() || _elapsed > 0) return
        var d = position
        if (d <= 0) d+= 1.0f
        duration = (maximumDuration * d).roundToLong()
        Log.i(TAG, "Duration: " + duration)
    }

    override fun onHubTouch() {
        if (!isRunning()) start()
        else pause()
    }

    private fun isRunning(): Boolean = _start > 0

    fun start() {
        if (isRunning()) return
        _start = System.currentTimeMillis()
        val editor: Editor = preferences.edit()
        editor.putLong("duration", duration)
        editor.commit()
        timerHandler.postDelayed(this, 0)
    }

    fun reset() {
        _stage = -1
        _elapsed = 0
        pause()
    }
    fun resetClickHandler(view: View) = reset()

    fun pause() {
        _start = 0
        timerHandler.removeCallbacks(this)
        updateViews()
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
        timerTextView.setText(getHoursAndMinutes(_duration, _elapsed))
        extraTimeTextView.setText(if (_elapsed > _duration) "+" else "")
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
