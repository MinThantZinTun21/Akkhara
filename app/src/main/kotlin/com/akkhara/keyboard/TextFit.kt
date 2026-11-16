package com.akkhara.keyboard

import android.graphics.Paint

/** Makes this paint's text smaller, if need be, so that [text] fits in [room] pixels. Never makes it larger. */
internal fun Paint.shrinkToFit(text: String, room: Float) {
    val width = measureText(text)
    if (width > room) textSize *= room / width
}
