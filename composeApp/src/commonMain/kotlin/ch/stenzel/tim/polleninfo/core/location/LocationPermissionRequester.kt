package ch.stenzel.tim.polleninfo.core.location

import androidx.compose.runtime.Composable

/**
 * Asks the user for approximate location, answering [onResult] with whether it was granted.
 *
 * **Why this is a composable and not part of [CoarseLocationProvider]:** Android's permission
 * request needs an `ActivityResultLauncher`, which is scoped to an activity and can only be obtained
 * from a composition. A Koin-injected class holds the *application* context and therefore cannot
 * prompt at all. Splitting the prompt from the lookup is what keeps the ViewModel free of platform
 * APIs: the screen prompts, then hands the resulting boolean to
 * `StationPicker.onPermissionResult`.
 *
 * If the permission is already granted, [onResult] is answered `true` without showing anything.
 */
@Composable
expect fun rememberCoarseLocationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit
