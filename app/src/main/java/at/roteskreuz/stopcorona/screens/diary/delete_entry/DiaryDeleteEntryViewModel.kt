package at.roteskreuz.stopcorona.screens.diary.delete_entry

import at.roteskreuz.stopcorona.model.repositories.DiaryRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles the user interaction.
 */
class DiaryDeleteEntryViewModel(
    appDispatchers: AppDispatchers,
    val diaryRepository: DiaryRepository
) : ScopedViewModel(appDispatchers) {

    fun deleteDiaryEntry(id: Long){
        diaryRepository.deleteDbDiaryEntryById(id)
    }

}