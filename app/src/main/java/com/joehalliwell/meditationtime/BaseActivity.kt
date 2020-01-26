package com.joehalliwell.meditationtime

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import java.lang.Exception

open class BaseActivity : AppCompatActivity(),  SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "MeditationTime"

    protected lateinit var preferences: SharedPreferences

    val fullscreen: Boolean
        get() {
            return preferences.getBoolean(
                resources.getString(R.string.fullscreen_pk),
                resources.getBoolean(R.bool.fullscreen_default)
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        configureTheme()
        super.onCreate(savedInstanceState)
        configureSystemUi() // Needs to run after the super.onCreate() for some reason
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) configureSystemUi()
        super.onWindowFocusChanged(hasFocus)
    }

    private fun configureSystemUi() {
        window?.decorView?.apply {
            var flags = 0
            if (fullscreen) {
                flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            flags = flags or View.SYSTEM_UI_FLAG_LOW_PROFILE
//            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
//            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            systemUiVisibility = flags
        }
    }

    private fun configureTheme() {
        val nightMode = preferences.getBoolean(
            resources.getString(R.string.night_mode_pk),
            resources.getBoolean(R.bool.night_mode_default))
        AppCompatDelegate.setDefaultNightMode(
            if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO);

        val theme = preferences.getString(
            resources.getString(R.string.theme_pk),
            resources.getString(R.string.theme_default)
        )
        try {
            val themeId = resources.getIdentifier(theme, "style", packageName)
            setTheme(themeId)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG,"Preference updated: " + key)
        when (key) {
            getString(R.string.theme_pk) -> recreate()
            getString(R.string.night_mode_pk) -> recreate()
            getString(R.string.fullscreen_pk) -> configureSystemUi()
        }
    }

}