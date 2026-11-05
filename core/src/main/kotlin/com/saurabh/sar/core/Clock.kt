package com.saurabh.sar.core

/** All time in SAR comes from here, so tests can use a fake clock. */
fun interface Clock {
    fun now(): Long
}
