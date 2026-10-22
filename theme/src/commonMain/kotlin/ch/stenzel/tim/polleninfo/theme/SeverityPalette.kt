package ch.stenzel.tim.polleninfo.theme

import androidx.compose.ui.graphics.Color

// Severity palette: neutral grey → green → amber → orange → red, from calmest to worst.
//
// These sit outside the Material 3 scheme, so nothing generates dark equivalents for them: each has
// an explicit light and dark value, because a red tuned for a near-white surface is a glaring blob
// on a near-black one.
//
// The ratio beside each value is its WCAG 2.1 contrast against the matching scheme's surface —
// `surfaceLight` (0xFFFBFAF3) or `surfaceDark` (0xFF16211A). Every value also has to clear the 3:1
// minimum for non-text graphics (SC 1.4.11) against every background a bar, dot or gauge sits on —
// background, surface, raised, sunken track, wash and map land — which `PaletteContrastTest` in
// :composeApp checks for both schemes. Moderate-light is the tightest (3.2:1 on wash, 3.39:1 on the
// sunken track); darken it first if it reads weakly.
//
// This module cannot see the app's severity type, so the mapping from a severity to one of these
// lives in the app (`core/ui/severity/SeverityColors.kt`).

// Light
val severityNoneLight = Color(0xFF6B6B60) // 5.15:1
val severityLowLight = Color(0xFF2E7D32) // 4.9:1
val severityModerateLight = Color(0xFFA07400) // 4.02:1
val severityHighLight = Color(0xFFB4500F) // 4.9:1
val severityVeryHighLight = Color(0xFFB3261E) // 6.25:1

// Dark
val severityNoneDark = Color(0xFF9C9C90) // 5.98:1
val severityLowDark = Color(0xFF7DD87F) // 9.48:1
val severityModerateDark = Color(0xFFF0B429) // 8.89:1
val severityHighDark = Color(0xFFF08135) // 6.23:1
val severityVeryHighDark = Color(0xFFF2564B) // 4.9:1
