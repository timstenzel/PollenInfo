package ch.stenzel.tim.polleninfo.theme

import androidx.compose.ui.graphics.Color

// Map palette: the lakes on the All stations map, a muted blue so they read as water at a glance.
//
// Like the severity palette these sit outside the Material 3 scheme — a lake is blue whatever the
// dynamic colour is — so each has an explicit light and dark value. The fill is deliberately faint
// and carries nothing on its own; the edge is what shows the lake's shape, including where a border
// lake extends past the country outline onto the bare surface. Ratios are WCAG 2.1 contrast against
// `surfaceLight` (0xFFFCFAED) / `surfaceDark` (0xFF13140D); the edges clear 3:1 (SC 1.4.11).
//
// The lakes are landmarks, not data: nothing on the map is identified by them alone. Keep them
// clearly blue and quieter than the severity colours, so a dot on a shore is never lost in a lake.

// Light
val mapWaterLight = Color(0xFFBBD7EC) // 1.43:1
val mapWaterEdgeLight = Color(0xFF5F8DB5) // 3.36:1

// Dark
val mapWaterDark = Color(0xFF2A4258) // 1.78:1
val mapWaterEdgeDark = Color(0xFF7FA6C9) // 7.23:1
