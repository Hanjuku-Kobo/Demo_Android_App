package com.example.esp32ble.ml

import android.graphics.Bitmap
import com.example.esp32ble.data.Person

interface PoseDetector : AutoCloseable {

    fun estimatePoses(bitmap: Bitmap): List<Person>

    fun lastInferenceTimeNanos(): Long
}