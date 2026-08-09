package com.gymshark.catalogue.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

private const val WAIT_TIMEOUT_MILLIS = 5_000L

/**
 * Generates `baseline-prof.txt` for :app (docs/PERFORMANCE.md, task 10.2). Exercises the
 * product list's real cold-start path plus one scroll, then navigates into a product's
 * detail screen — the three journeys `docs/ARCHITECTURE.md` §14.2 names as worth profiling.
 */
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() =
        baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            val list = device.wait(Until.findObject(By.scrollable(true)), WAIT_TIMEOUT_MILLIS)
            list?.fling(Direction.DOWN)
            device.waitForIdle()

            val firstCard = device.wait(Until.findObject(By.clickable(true)), WAIT_TIMEOUT_MILLIS)
            firstCard?.click()
            device.waitForIdle()
        }
}
