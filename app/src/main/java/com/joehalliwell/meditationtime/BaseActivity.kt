package com.joehalliwell.meditationtime

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import java.lang.Exception

open class BaseActivity : AppCompatActivity(),  SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "MeditationTime"

    protected lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        configureTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
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
        }
    }

}