package com.example.esp32ble.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.example.esp32ble.data.*

class UseMoveNet(private var detector: PoseDetector) {

    companion object {
        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
    }

    // process image
    fun processImage(bitmap: Bitmap) : Bitmap {
        val persons = mutableListOf<Person>()

        synchronized(Any()) {
            detector.estimatePoses(bitmap).let {
                persons.addAll(it)
            }
        }

        return visualize(persons, bitmap)
    }

    private fun visualize(persons: List<Person>, bitmap: Bitmap) : Bitmap {
        val resultBitmap: Bitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        val outputBitmap = VisualizationUtils.drawBodyKeypoints(
            bitmap,
            persons.filter { it.score > MIN_CONFIDENCE }, false
        )

        val canvas = Canvas(resultBitmap)

        val screenWidth: Int
        val screenHeight: Int
        val left: Int
        val top: Int

        if (canvas.height > canvas.width) {
            val ratio = outputBitmap.height.toFloat() / outputBitmap.width
            screenWidth = canvas.width
            left = 0
            screenHeight = (canvas.width * ratio).toInt()
            top = (canvas.height - screenHeight) / 2
        } else {
            val ratio = outputBitmap.width.toFloat() / outputBitmap.height
            screenHeight = canvas.height
            top = 0
            screenWidth = (canvas.height * ratio).toInt()
            left = (canvas.width - screenWidth) / 2
        }
        val right: Int = left + screenWidth
        val bottom: Int = top + screenHeight

        canvas.drawBitmap(
            outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
            Rect(left, top, right, bottom), null)

        return resultBitmap
    }
}