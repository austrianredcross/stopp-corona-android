package at.roteskreuz.stopcorona.screens.debug.scheduling

import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.model.workers.ExposureMatchingWorker
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles the user interaction and provides data for [SchedulingObserverFragment].
 */
class SchedulingObserverViewModel(
    appDispatchers: AppDispatchers,
    private val workManager: WorkManager
) : ScopedViewModel(appDispatchers) {

    fun observeExposureMatchingState(): LiveData<MutableList<WorkInfo>> = ExposureMatchingWorker.observeState(workManager)
}
