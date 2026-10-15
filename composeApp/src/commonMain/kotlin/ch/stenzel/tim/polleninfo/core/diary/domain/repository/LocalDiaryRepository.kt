package ch.stenzel.tim.polleninfo.core.diary.domain.repository

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

/**
 * The answers recorded on this phone before they moved to the account — read-only, and read only by
 * the Diary chart until it loads the account's answers instead. Nothing writes it any more.
 */
interface LocalDiaryRepository {

    /** Every answer recorded on the device, sorted by date. */
    val entries: Flow<List<DiaryEntry>>
}
