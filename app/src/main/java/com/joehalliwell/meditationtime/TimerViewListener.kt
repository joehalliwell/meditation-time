package com.joehalliwell.meditationtime

interface TimerViewListener {
    fun onDialTouch(position: Float): Boolean
    fun onHubTouch()
}