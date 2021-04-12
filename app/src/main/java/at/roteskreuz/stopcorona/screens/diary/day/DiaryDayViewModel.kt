package at.roteskreuz.stopcorona.screens.diary.day


import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntryWrapper
import at.roteskreuz.stopcorona.model.repositories.DiaryRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable
import org.threeten.bp.LocalDate

class DiaryDayViewModel(
    appDispatchers: AppDispatchers,
    private val diaryRepository: DiaryRepository
) : ScopedViewModel(appDispatchers) {

    fun observeDiaryEntryForDate(date: LocalDate): Observable<List<DbDiaryEntryWrapper>> {
        return diaryRepository.observeDbDiaryEntryForDateWrapper(date)
    }
}
