package com.joehalliwell.meditationtime

interface TimerViewListener {
    fun onDialTouch(angle: Double): Boolean
    fun onHubTouch()
}