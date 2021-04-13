package at.roteskreuz.stopcorona.screens.diary


import android.content.Context
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntryWrapper
import at.roteskreuz.stopcorona.model.entities.diary.DiaryEntryType
import at.roteskreuz.stopcorona.model.repositories.DiaryRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.string
import io.reactivex.Observable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

class DiaryViewModel(
    appDispatchers: AppDispatchers,
    private val diaryRepository: DiaryRepository
) : ScopedViewModel(appDispatchers) {

    fun observeDbDiaryEntry(): Observable<List<DbDiaryEntry>> {
        return diaryRepository.observeDbDiaryEntry()
    }

    fun observeDbDiaryEntryWrapper(date: LocalDate): Observable<List<DbDiaryEntryWrapper>> {
        return diaryRepository.observeDbDiaryEntryForDateWrapper(date)
    }

    suspend fun getDbDiaryEntryForDateWrapper(date: LocalDate): List<DbDiaryEntryWrapper> {
        return diaryRepository.getDbDiaryEntryForDateWrapper(date)
    }

    fun deleteDiaryEntry(id: Long) {
        diaryRepository.deleteDbDiaryEntryById(id)
    }

    fun getText(
        context:Context,
        dates: MutableMap<LocalDate, MutableList<DbDiaryEntry>>,
        onTextLoaded: (text: String) -> Unit
    ) {
        launch {
            var text = ""
            dates.forEach { date ->
                if (date.value.isNotEmpty()) {
                    text += date.key
                    text += ": "
                    val entries = async { getDbDiaryEntryForDateWrapper(date.key) }
                    entries.await().forEach { entryWrapper ->
                        when (entryWrapper.dbDiaryEntry.type) {
                            DiaryEntryType.PERSON -> {
                                text += entryWrapper.dbPersonEntity?.fullName
                                entryWrapper.dbPersonEntity?.notes?.let { notes ->
                                    text += ", "
                                    text += notes
                                }
                            }
                            DiaryEntryType.LOCATION -> {
                                text += entryWrapper.dbLocationEntity?.locationName
                                entryWrapper.dbLocationEntity?.timeOfDay?.let { timeOfDay ->
                                    text += ", "
                                    text += timeOfDay
                                }
                            }
                            DiaryEntryType.PUBLIC_TRANSPORT -> {
                                text += entryWrapper.dbDiaryPublicTransportEntity?.description
                                entryWrapper.dbDiaryPublicTransportEntity?.startLocation?.let { startLocation ->
                                    text += ", "
                                    text += startLocation
                                }
                                entryWrapper.dbDiaryPublicTransportEntity?.endLocation?.let { endLocation ->
                                    text += ", "
                                    text += endLocation
                                }
                                entryWrapper.dbDiaryPublicTransportEntity?.startTime?.let { startTime ->
                                    text += ", "
                                    text += startTime.format(
                                        context.string(R.string.diary_time_format)
                                    ) + " " + context.string(R.string.diary_hour)
                                }
                            }
                            DiaryEntryType.EVENT -> {
                                text += entryWrapper.dbEventEntity?.description
                                entryWrapper.dbEventEntity?.startTime?.let { startTime ->
                                    text += ", "
                                    text += startTime.format(
                                        context.string(R.string.diary_time_format)
                                    ) + " " + context.string(R.string.diary_hour)
                                }
                                entryWrapper.dbEventEntity?.endTime?.let { endTime ->
                                    text += ", "
                                    text += endTime.format(
                                        context.string(R.string.diary_time_format)
                                    ) + " " + context.string(R.string.diary_hour)
                                }
                            }
                        }
                        text += ";\n"
                    }
                    text += "\n"
                }
            }
            onTextLoaded(text)
        }
    }
}