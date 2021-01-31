package at.roteskreuz.stopcorona.screens.base.dialog.datepicker

import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import org.threeten.bp.ZonedDateTime

class DatePickerFragmentDialogViewModel (
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository
) : ScopedViewModel(appDispatchers) {

    fun setDateOfInfection(date: ZonedDateTime) {
        reportingRepository.setDateOfInfection(date)
    }
}