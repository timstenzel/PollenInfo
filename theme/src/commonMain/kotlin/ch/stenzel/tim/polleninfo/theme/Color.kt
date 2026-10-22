package ch.stenzel.tim.polleninfo.theme

import androidx.compose.ui.graphics.Color

// The Meadow palette: meadow-cream backgrounds, forest green as the brand colour, sage for the quiet
// accents. The Figma tokens it comes from are named in the comments (file "PollenInfo Redesign",
// collections "Light" and "Dark"); roles Figma has no token for are derived to fit.
//
// The ratio beside a foreground colour is its WCAG 2.1 contrast against the background it is drawn
// on (named after the ratio; "bg" = background, "surface", "raised" = surfaceContainerLowest). The
// pairs that matter are pinned by `PaletteContrastTest` in :composeApp — change a value there first.

// Light
val primaryLight = Color(0xFF2E5B3B) // brand/primary
val onPrimaryLight = Color(0xFFFFFFFF) // 7.85:1 on primary
val primaryContainerLight = Color(0xFFCFE3BF) // brand/primary-container
val onPrimaryContainerLight = Color(0xFF15301E) // 10.45:1 on primaryContainer
val secondaryLight = Color(0xFF4E6352) // derived: sage, 6.21:1 on surface
val onSecondaryLight = Color(0xFFFFFFFF) // 6.5:1 on secondary
val secondaryContainerLight = Color(0xFFD6E6C2) // = wash
val onSecondaryContainerLight = Color(0xFF1C3022) // 10.67:1 on secondaryContainer
val tertiaryLight = Color(0xFF6B5310) // derived: pollen gold darkened for text, 6.99:1 on surface
val onTertiaryLight = Color(0xFFFFFFFF) // 7.32:1 on tertiary
val tertiaryContainerLight = Color(0xFFF3E2A6) // derived: pale pollen gold
val onTertiaryContainerLight = Color(0xFF2A2207) // 12.21:1 on tertiaryContainer
val errorLight = Color(0xFFB3261E) // 6.25:1 on surface
val onErrorLight = Color(0xFFFFFFFF) // 6.54:1 on error
val errorContainerLight = Color(0xFFF9DEDC)
val onErrorContainerLight = Color(0xFF8C1D18) // 7.17:1 on errorContainer
val backgroundLight = Color(0xFFF1F0E2) // bg/canvas
val onBackgroundLight = Color(0xFF17241B) // text/primary, 14.03:1 on bg
val surfaceLight = Color(0xFFFBFAF3) // surface/default
val onSurfaceLight = Color(0xFF17241B) // text/primary, 15.38:1 on surface, 16.1:1 on raised
val surfaceVariantLight = Color(0xFFE7E8D6) // = surface/sunken
val onSurfaceVariantLight = Color(0xFF556356) // text/secondary, 5.54:1 on bg, 6.07:1 on surface, 6.35:1 on raised
// Material's outline is a component edge (text fields, outlined buttons), which SC 1.4.11 wants at
// 3:1; Figma's outline/default is the hairline, so it becomes outlineVariant.
val outlineLight = Color(0xFF7D8A7C) // derived, 3.46:1 on surface, 3.16:1 on bg
val outlineVariantLight = Color(0xFFCBD0BA) // outline/default
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF17241B) // text/primary (snackbar)
val inverseOnSurfaceLight = Color(0xFFFBFAF3) // surface/default, 15.38:1 on inverseSurface
val inversePrimaryLight = Color(0xFFA6D5A0)
val surfaceDimLight = Color(0xFFDDDCCB) // derived
val surfaceBrightLight = Color(0xFFFBFAF3) // = surface
val surfaceContainerLowestLight = Color(0xFFFFFFFF) // surface/raised (cards)
val surfaceContainerLowLight = Color(0xFFF6F5EB) // derived, between surface and bg
val surfaceContainerLight = Color(0xFFF1F0E2) // derived, = bg
val surfaceContainerHighLight = Color(0xFFECECDC) // derived, between bg and sunken
val surfaceContainerHighestLight = Color(0xFFE7E8D6) // surface/sunken (tracks, skeletons, segments)

// Dark
val primaryDark = Color(0xFFA6D5A0) // brand/primary
val onPrimaryDark = Color(0xFF0D2614) // 9.69:1 on primary
val primaryContainerDark = Color(0xFF25442E) // brand/primary-container
val onPrimaryContainerDark = Color(0xFFCFE9C8) // 8.3:1 on primaryContainer
val secondaryDark = Color(0xFFB6CBB4) // derived: sage, 9.62:1 on surface
val onSecondaryDark = Color(0xFF213526) // 7.61:1 on secondary
val secondaryContainerDark = Color(0xFF1C3022) // = wash
val onSecondaryContainerDark = Color(0xFFD6E6C2) // 10.67:1 on secondaryContainer
val tertiaryDark = Color(0xFFF0CB5C) // derived: pollen gold, = accent
val onTertiaryDark = Color(0xFF2A2207) // 10.07:1 on tertiary
val tertiaryContainerDark = Color(0xFF4A3B10) // derived: deep pollen gold
val onTertiaryContainerDark = Color(0xFFF6E3B0) // 8.59:1 on tertiaryContainer
val errorDark = Color(0xFFF2B8B5) // 9.71:1 on surface
val onErrorDark = Color(0xFF601410) // 7.66:1 on error
val errorContainerDark = Color(0xFF8C1D18)
val onErrorContainerDark = Color(0xFFF9DEDC) // 7.17:1 on errorContainer
val backgroundDark = Color(0xFF0E1611) // bg/canvas
val onBackgroundDark = Color(0xFFE8EEDF) // text/primary, 15.53:1 on bg
val surfaceDark = Color(0xFF16211A) // surface/default
val onSurfaceDark = Color(0xFFE8EEDF) // text/primary, 14.0:1 on surface, 12.29:1 on raised
val surfaceVariantDark = Color(0xFF26332A) // derived
val onSurfaceVariantDark = Color(0xFFA7B4A5) // text/secondary, 8.51:1 on bg, 7.67:1 on surface, 6.73:1 on raised
val outlineDark = Color(0xFF87958A) // derived, 5.29:1 on surface (see outlineLight)
val outlineVariantDark = Color(0xFF34443A) // outline/default
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE8EEDF) // text/primary (snackbar)
val inverseOnSurfaceDark = Color(0xFF16211A) // surface/default, 14.0:1 on inverseSurface
val inversePrimaryDark = Color(0xFF2E5B3B)
val surfaceDimDark = Color(0xFF0E1611) // = bg
val surfaceBrightDark = Color(0xFF2A382E) // derived
// In the dark scheme a raised card is lighter and a sunken track darker than the surface, so the
// container roles do not grow monotonically lighter as Material's tonal ones do.
val surfaceContainerLowestDark = Color(0xFF1F2C23) // surface/raised (cards)
val surfaceContainerLowDark = Color(0xFF19251D) // derived
val surfaceContainerDark = Color(0xFF1C2820) // derived
val surfaceContainerHighDark = Color(0xFF1F2C23) // derived, = raised (dialogs, menus)
val surfaceContainerHighestDark = Color(0xFF0A110C) // surface/sunken (tracks, skeletons, segments)
