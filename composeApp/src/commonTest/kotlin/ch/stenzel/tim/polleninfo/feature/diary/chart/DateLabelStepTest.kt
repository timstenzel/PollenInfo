package ch.stenzel.tim.polleninfo.feature.diary.chart

import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelStepTest {

    private val xs = listOf(0f, 40f, 80f, 120f)

    @Test
    fun `every label is drawn while the widest fits the spacing with its gap`() {
        assertEquals(1, dateLabelStep(xs, widths = listOf(30f, 34f, 20f, 25f), gap = 6f))
    }

    @Test
    fun `one pixel more than the spacing draws every second label`() {
        assertEquals(2, dateLabelStep(xs, widths = listOf(30f, 35f, 20f, 25f), gap = 6f))
    }

    @Test
    fun `labels wider than two spacings draw every third`() {
        assertEquals(3, dateLabelStep(xs, widths = listOf(75f), gap = 6f))
    }

    @Test
    fun `the narrowest spacing decides`() {
        assertEquals(2, dateLabelStep(listOf(0f, 40f, 70f), widths = listOf(30f), gap = 6f))
    }

    @Test
    fun `a single tick or none is always drawn`() {
        assertEquals(1, dateLabelStep(listOf(10f), widths = listOf(500f), gap = 6f))
        assertEquals(1, dateLabelStep(emptyList(), widths = emptyList(), gap = 6f))
    }
}
