package com.joehalliwell.meditationtime

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        supportFragmentManager
//            .beginTransaction()
//            .replace(R.id.settings_fragment, SettingsFragment())
//            .commit()
    }


    fun selectTime(view : View) {
        val seconds = 20;
        val intent = Intent(this, TimerActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, seconds)
        }
        startActivity(intent)
    }

}
