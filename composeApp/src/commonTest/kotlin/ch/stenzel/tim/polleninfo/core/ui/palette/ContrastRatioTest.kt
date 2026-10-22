package ch.stenzel.tim.polleninfo.core.ui.palette

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ContrastRatioTest {

    @Test
    fun `black on white is the maximum of 21`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), absoluteTolerance = 1e-9)
    }

    @Test
    fun `identical colours are the minimum of 1`() {
        assertEquals(1.0, contrastRatio(Color.White, Color.White), absoluteTolerance = 1e-9)
        assertEquals(1.0, contrastRatio(Color(0xFF2E5B3B), Color(0xFF2E5B3B)), absoluteTolerance = 1e-9)
    }

    @Test
    fun `the order of the two colours does not matter`() {
        val a = Color(0xFFA07400)
        val b = Color(0xFFE7E8D6)

        assertEquals(contrastRatio(a, b), contrastRatio(b, a), absoluteTolerance = 1e-12)
    }

    @Test
    fun `a known mid grey on white matches the published ratio`() {
        // #777777 on white is the textbook just-below-AA example: 4.48:1.
        assertEquals(4.48, contrastRatio(Color(0xFF777777), Color.White), absoluteTolerance = 0.01)
    }
}
