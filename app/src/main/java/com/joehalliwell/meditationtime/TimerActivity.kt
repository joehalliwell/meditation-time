package com.joehalliwell.meditationtime

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager


class TimerActivity : AppCompatActivity() {

    var startTime = 0L
    var durationSeconds = 0
    var stage = 0
    lateinit var timerTextView : TextView

    val timerHandler = Handler()
    val timerRunnable: Runnable = object : Runnable {
        override fun run() {
            updateStage()
            updateDisplay()
            timerHandler.postDelayed(this, 500)
        }
    }

    fun updateDisplay() {
        val elapsed: Long = System.currentTimeMillis() - startTime
        val seconds =  (elapsed / 1000).toInt()
        val minutes = seconds / 60
        timerTextView.setText(String.format("Stage %d\n%d:%02d\n%d/%d",
            stage,
            minutes,
            seconds % 60,
            seconds,
            durationSeconds))

    }

    fun updateStage() {
        val elapsed: Long = System.currentTimeMillis() - startTime
        val actualStage = (elapsed.toDouble() * 4 / (durationSeconds * 1000)).toInt()

        if (stage == actualStage) return;
        stage = actualStage;
        if (stage == 0) {
            play(R.raw.start)
        }
        else if (stage > 0 && stage < 4) {
            play(R.raw.mid)
        }
        else if (stage == 4) {
            play(R.raw.end)
        }
        else {
            play(R.raw.post)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        timerTextView = findViewById<TextView>(R.id.textView)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val duration = sharedPreferences.getString("duration", "20").orEmpty()

        durationSeconds = Integer.parseInt(duration)

        start()
    }

    override fun onStop() {
        super.onStop()
        stop()
    }


    fun play(resource: Int) {
        val mp = MediaPlayer.create(this, resource)
        timerHandler.postDelayed(timerRunnable, 0);
        mp.start()
    }

    fun start() {
        startTime = System.currentTimeMillis().toLong()
        stage = -1;
        timerHandler.post(timerRunnable)
    }

    fun stop() {
        timerHandler.removeCallbacks(timerRunnable)
    }
}
