package at.roteskreuz.stopcorona.screens.diary.new_entry

import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEvent
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryLocation
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryPerson
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryPublicTransport
import at.roteskreuz.stopcorona.model.repositories.ContactEntry
import at.roteskreuz.stopcorona.model.repositories.DiaryRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.ValidationError
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.validateNotEmpty
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

class DiaryNewEntryViewModel(
    appDispatchers: AppDispatchers,
    private val diaryRepository: DiaryRepository
) : ScopedViewModel(appDispatchers) {

    var selectedDay: LocalDate? = null
    var selectedContactEntry: ContactEntry = ContactEntry.Person
    var publicTransportTime: LocalTime? = null
    var eventStart: LocalTime? = null
    var eventEnd: LocalTime? = null

    private val validationResultSubject = BehaviorSubject.create<ValidationResult>()

    private var description: String? = null

    fun setDescription(description: String?) {
        this.description = description
    }

    private var personNotes: String? = null

    fun setPersonNotes(personNotes: String?) {
        this.personNotes = personNotes
    }

    private var locationTimeOfDay: String? = null

    fun setLocationTimeOfDay(locationTimeOfDay: String?) {
        this.locationTimeOfDay = locationTimeOfDay
    }

    private var publicTransportStart: String? = null

    fun setPublicTransportStart(publicTransportStart: String?) {
        this.publicTransportStart = publicTransportStart
    }

    private var publicTransportEnd: String? = null

    fun setPublicTransportEnd(publicTransportEnd: String?) {
        this.publicTransportEnd = publicTransportEnd
    }

    fun updatePublicTransportTime(publicTransportTime: LocalTime) {
        diaryRepository.setPublicTransportTime(publicTransportTime)
    }

    fun updateEventStart(eventStart: LocalTime) {
        diaryRepository.setEventStart(eventStart)
    }

    fun updateEventEnd(eventEnd: LocalTime) {
        diaryRepository.setEventEnd(eventEnd)
    }

    fun validate(onSaved: () -> Unit) {
        val description = this.description
        val validationResult = ValidationResult(validateNotEmpty(description))
        if (validationResult.isValid()) {
            description?.let {
                when (selectedContactEntry) {
                    ContactEntry.Person -> {
                        val dbPerson = DbDiaryPerson(fullName = description, notes = personNotes)
                        selectedDay?.let {
                            diaryRepository.insertPerson(dbPerson, it)
                            onSaved()
                        }
                    }
                    ContactEntry.Location -> {
                        val dbLocation =
                            DbDiaryLocation(
                                locationName = description,
                                timeOfDay = locationTimeOfDay
                            )
                        selectedDay?.let {
                            diaryRepository.insertLocation(dbLocation, it)
                            onSaved()
                        }
                    }
                    ContactEntry.PublicTransport -> {
                        val dbPublicTransport = DbDiaryPublicTransport(
                            description = description,
                            startLocation = publicTransportStart,
                            endLocation = publicTransportEnd,
                            startTime = publicTransportTime
                        )
                        selectedDay?.let {
                            diaryRepository.insertPublicTransport(
                                dbPublicTransport,
                                it
                            )
                            onSaved()
                        }
                    }
                    ContactEntry.Event -> {
                        val dbEvent = DbDiaryEvent(
                            description = description,
                            startTime = eventStart,
                            endTime = eventEnd
                        )
                        selectedDay?.let {
                            diaryRepository.insertEvent(dbEvent, it)
                            onSaved()
                        }
                    }
                }
            }
        }
        validationResultSubject.onNext(validationResult)
    }

    fun updateSelectedContactEntry(contactEntry: ContactEntry) {
        diaryRepository.setSelectedContactEntry(contactEntry)
    }

    fun observeSelectedContactEntry(): Observable<ContactEntry> {
        return diaryRepository.observeSelectedContactEntry()
    }

    fun observeValidationResult(): Observable<ValidationResult> {
        return validationResultSubject
    }

    fun observePublicTransportTime(): Observable<LocalTime> {
        return diaryRepository.observePublicTransportTime()
    }

    fun observeEventStart(): Observable<LocalTime> {
        return diaryRepository.observeEventStart()
    }

    fun observeEventEnd(): Observable<LocalTime> {
        return diaryRepository.observeEventEnd()
    }
}

/**
 * Describes the result of the description field.
 */
data class ValidationResult(
    val description: ValidationError?
) {

    fun isValid(): Boolean {
        return description == null
    }
}