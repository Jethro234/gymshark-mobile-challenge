package com.gymshark.catalogue.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ITERATIONS = 10
private const val FLINGS_PER_ITERATION = 3
private const val WAIT_TIMEOUT_MILLIS = 5_000L

// Keeps flings away from the screen edges, where gesture-nav back/forward can hijack them.
private const val GESTURE_MARGIN_DIVISOR = 5

/**
 * `docs/PERFORMANCE.md` §1a/§2 — scroll jank on the real sixty-product dataset (thirty grid
 * rows), no synthetic multiplied dataset needed for a real scroll.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollNoCompilation() = scroll(CompilationMode.None())

    @Test
    fun scrollBaselineProfile() = scroll(CompilationMode.Partial())

    private fun scroll(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = ITERATIONS,
            setupBlock = {
                pressHome()
                startActivityAndWait()
            },
        ) {
            val list =
                device.wait(Until.findObject(By.scrollable(true)), WAIT_TIMEOUT_MILLIS)
                    ?: error("No scrollable product list found within $WAIT_TIMEOUT_MILLIS ms.")
            list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
            repeat(FLINGS_PER_ITERATION) {
                list.fling(Direction.DOWN)
                device.waitForIdle()
            }
        }
}
