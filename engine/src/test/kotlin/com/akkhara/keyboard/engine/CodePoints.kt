package com.akkhara.keyboard.engine

/**
 * [text] as its code units in hexadecimal, so that a failed comparison of
 * Myanmar text shows the stored order rather than how a font draws it.
 */
internal fun codePoints(text: String): String =
    text.map { "%04X".format(it.code) }.joinToString(" ")
