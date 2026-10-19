package ch.stenzel.tim.polleninfo.theme

import androidx.compose.ui.graphics.Color

// Map palette: the lakes on the All stations map, a muted blue so they read as water at a glance.
//
// Like the severity palette these sit outside the Material 3 scheme — a lake is blue whatever the
// scheme — so each has an explicit light and dark value. The lakes are a flat fill without an edge
// line, so the fill itself shows the lake's shape: the ratio beside it is its WCAG 2.1 contrast
// against the Meadow map land (`MeadowLightColors.mapLand` 0xFFE3EBD2 / `MeadowDarkColors.mapLand`
// 0xFF1A281E), visibly apart yet quieter than any severity colour.
//
// The lakes are landmarks, not data: nothing on the map is identified by them alone, and no
// contrast is required of them. Keep them clearly blue and quieter than the severity colours, so a
// dot on a shore is never lost in a lake.

val mapWaterLight = Color(0xFF9CC3D6) // 1.53:1
val mapWaterDark = Color(0xFF2D5466) // 1.88:1
