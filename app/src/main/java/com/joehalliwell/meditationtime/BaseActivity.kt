package com.joehalliwell.meditationtime

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.lang.Exception
import android.content.Intent

/**
 * Base class for themeable activities
 */
open class BaseActivity : AppCompatActivity(),  SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "MeditationTime"

    protected lateinit var preferences: SharedPreferences
    private var active: Boolean = false
    private var needThemeUpdate = false

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

    override fun onStart() {
        super.onStart()
        active = true
        checkTheme()
    }

    override fun onStop() {
        super.onStop()
        active = false
    }

    private fun configureSystemUi() {
        window?.decorView?.apply {
            var flags = 0 //View.SYSTEM_UI_FLAG_LOW_PROFILE
            if (fullscreen) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        //View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        //View.SYSTEM_UI_FLAG_FULLSCREEN or
                        //View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        //View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            }
            else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
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

    fun updateTheme() {
        needThemeUpdate = true
        checkTheme()
    }

    private fun checkTheme() {
        if (active && needThemeUpdate) {
            recreateWithFade()
            needThemeUpdate = false
        }
    }

    private fun recreateWithFade() {
        Log.i(TAG, "Recreating " + this)
        finish()
        startActivity(Intent(this, javaClass))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG,"Preference updated: " + key + " in " + this)
        when (key) {
            getString(R.string.theme_pk) -> updateTheme()
            getString(R.string.night_mode_pk) -> updateTheme()
            getString(R.string.fullscreen_pk) -> configureSystemUi()
        }
    }


}