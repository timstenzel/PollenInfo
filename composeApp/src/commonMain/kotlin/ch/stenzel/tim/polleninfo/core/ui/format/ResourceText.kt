package ch.stenzel.tim.polleninfo.core.ui.format

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A sentence still to be put into words: the resource and its already formatted arguments.
 *
 * What a pure wording function returns when the sentence itself is a translation, so the choice of
 * sentence and its arguments are tested without composing anything — `StringResource` has value
 * equality. The screen resolves it with [resolve], suspending code with [load].
 */
data class ResourceText(val resource: StringResource, val args: List<String> = emptyList())

@Composable
fun ResourceText.resolve(): String = stringResource(resource, *args.toTypedArray())

suspend fun ResourceText.load(): String = getString(resource, *args.toTypedArray())
