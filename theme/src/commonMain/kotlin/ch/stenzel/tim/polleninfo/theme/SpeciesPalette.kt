package ch.stenzel.tim.polleninfo.theme

import androidx.compose.ui.graphics.Color

// Species palette: one categorical colour per pollen type, for the diary's lines and their
// checkbox legend. In species order: blue, magenta, violet, cyan, indigo, teal, lilac.
//
// The hues stay inside cyan → blue → violet → magenta so that no line can be read as a severity:
// SeverityPalette owns grey, traffic-light green, amber, orange and red. That range holds seven
// hues only just, so the order is part of the design — it is one of the few that clear the
// colour-vision checks for neighbouring legend entries in both schemes (worst neighbouring pair
// under simulated colour-blindness ΔE 10.2 light / 8.1 dark, OKLab ×100; with normal vision 19.3 /
// 15.9). Re-run a palette validator on any change rather than nudging one value by eye.
//
// Each colour has an explicit light and dark value, as in SeverityPalette, and the ratio beside it
// is its WCAG 2.1 contrast against `surfaceLight` (0xFFFBFAF3) or `surfaceDark` (0xFF16211A), the
// diary chart card's background. All clear the 3.5:1 the lines need (`PaletteContrastTest`). The
// chart card must stay on `surface`: on the lighter dark `surfaceContainerLowest` (raised, 0xFF1F2C23)
// Ash-dark falls to 3.11:1.
//
// Known limit: Oak's light teal is the nearest any value comes to a severity colour — ΔE 9.4 from
// severityLowLight. Kept on purpose: severity colours are not drawn in the diary chart, and every
// line is named by its checkbox, so colour never identifies a species alone.
//
// This module cannot see the app's species ids, so the mapping lives in the app
// (`core/ui/species/SpeciesColors.kt`).

// Light
val speciesAlderLight = Color(0xFF1F6FD1) // 4.72:1
val speciesBirchLight = Color(0xFFC2338A) // 4.85:1
val speciesHazelLight = Color(0xFF6A3FC1) // 6.49:1
val speciesBeechLight = Color(0xFF0090A8) // 3.61:1
val speciesAshLight = Color(0xFF2F4BA8) // 7.45:1
val speciesOakLight = Color(0xFF00897B) // 4.13:1
val speciesGrassesLight = Color(0xFFB565C9) // 3.54:1

// Dark
val speciesAlderDark = Color(0xFF4C8DF0) // 5.03:1
val speciesBirchDark = Color(0xFFE05AA8) // 4.89:1
val speciesHazelDark = Color(0xFF9A73F0) // 4.8:1
val speciesBeechDark = Color(0xFF1495B0) // 4.7:1
val speciesAshDark = Color(0xFF6567D8) // 3.54:1
val speciesOakDark = Color(0xFF1AA595) // 5.42:1
val speciesGrassesDark = Color(0xFFB86FCC) // 4.88:1
