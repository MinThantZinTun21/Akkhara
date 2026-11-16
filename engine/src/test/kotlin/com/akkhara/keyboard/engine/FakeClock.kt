package com.akkhara.keyboard.engine

/** Time the test controls, in milliseconds. */
class FakeClock(var now: Long = 0L) : () -> Long {
    override fun invoke(): Long = now

    fun advance(millis: Long) {
        now += millis
    }
}
