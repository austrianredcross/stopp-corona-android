package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.db.dao.DiaryDao
import at.roteskreuz.stopcorona.model.entities.diary.*
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.asDbObservable
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing diary content.
 */
interface DiaryRepository {

    fun insertPerson(person: DbDiaryPerson, date: LocalDate)

    fun insertLocation(location: DbDiaryLocation, date: LocalDate)

    fun insertPublicTransport(publicTransport: DbDiaryPublicTransport, date: LocalDate)

    fun insertEvent(event: DbDiaryEvent, date: LocalDate)

    fun observePersons() : Observable<List<DbDiaryPerson>>

    fun observeDbDiaryEntry(): Observable<List<DbDiaryEntry>>

    fun observeDbDiaryEntryForDate(date: LocalDate): Observable<List<DbDiaryEntry>>

    fun observeDbDiaryEntryForDateWrapper(date: LocalDate): Observable<List<DbDiaryEntryWrapper>>

    suspend fun getDbDiaryEntryForDateWrapper(date: LocalDate): List<DbDiaryEntryWrapper>

    fun deleteDbDiaryEntryById(id: Long)

    fun observeLocations() : Observable<List<DbDiaryLocation>>

    fun observePublicTransports() : Observable<List<DbDiaryPublicTransport>>

    fun observeEvents() : Observable<List<DbDiaryEvent>>

    fun setSelectedContactEntry(contactEntry: ContactEntry)

    fun observeSelectedContactEntry(): Observable<ContactEntry>

    fun setPublicTransportTime(publicTransportTime: LocalTime)

    fun observePublicTransportTime(): Observable<LocalTime>

    fun setEventStart(eventStart: LocalTime)

    fun observeEventStart(): Observable<LocalTime>

    fun setEventEnd(eventEnd: LocalTime)

    fun observeEventEnd(): Observable<LocalTime>
}

class DiaryRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val diaryDao: DiaryDao
) : DiaryRepository, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    private val selectedContactEntrySubject = NonNullableBehaviorSubject<ContactEntry>(ContactEntry.Person)
    private val publicTransportTimeSubject = BehaviorSubject.create<LocalTime>()
    private val eventStartSubject = BehaviorSubject.create<LocalTime>()
    private val eventEndSubject = BehaviorSubject.create<LocalTime>()

    override fun setSelectedContactEntry(contactEntry: ContactEntry) {
        selectedContactEntrySubject.onNext(contactEntry)
    }

    override fun observeSelectedContactEntry(): Observable<ContactEntry> {
        return selectedContactEntrySubject
    }

    override fun setPublicTransportTime(publicTransportTime: LocalTime) {
        publicTransportTimeSubject.onNext(publicTransportTime)
    }

    override fun observePublicTransportTime(): Observable<LocalTime> {
        return publicTransportTimeSubject
    }

    override fun setEventStart(eventStart: LocalTime) {
        eventStartSubject.onNext(eventStart)
    }

    override fun observeEventStart(): Observable<LocalTime> {
        return eventStartSubject
    }

    override fun setEventEnd(eventEnd: LocalTime) {
        eventEndSubject.onNext(eventEnd)
    }

    override fun observeEventEnd(): Observable<LocalTime> {
        return eventEndSubject
    }
    override fun insertPerson(person: DbDiaryPerson, date: LocalDate) {
        launch(appDispatchers.Default) {
            try {
                val personId = diaryDao.insertPerson(person)
                val diaryEntry = DbDiaryEntry(date = date, personId = personId,type = DiaryEntryType.PERSON)
                diaryDao.insertDiaryEntry(diaryEntry)
            } catch (ex: Exception) {
                Timber.e(ex, "insert person failed")
            }
        }
    }

    override fun insertLocation(location: DbDiaryLocation, date: LocalDate) {
        launch(appDispatchers.Default) {
            try {
                val locationId = diaryDao.insertLocation(location)
                val diaryEntry = DbDiaryEntry(date = date, locationId = locationId,type = DiaryEntryType.LOCATION)
                diaryDao.insertDiaryEntry(diaryEntry)
            } catch (ex: Exception) {
                Timber.e(ex, "insert location failed")
            }
        }
    }

    override fun insertPublicTransport(publicTransport: DbDiaryPublicTransport, date: LocalDate) {
        launch(appDispatchers.Default) {
            try {
                val publicTransportId = diaryDao.insertPublicTransport(publicTransport)
                val diaryEntry = DbDiaryEntry(date = date, publicTransportId = publicTransportId,type = DiaryEntryType.PUBLIC_TRANSPORT)
                diaryDao.insertDiaryEntry(diaryEntry)
            } catch (ex: Exception) {
                Timber.e(ex, "insert public transport failed")
            }
        }
    }

    override fun insertEvent(event: DbDiaryEvent, date: LocalDate) {
        launch(appDispatchers.Default) {
            try {
                val eventId = diaryDao.insertEvent(event)
                val diaryEntry = DbDiaryEntry(date = date, eventId = eventId,type = DiaryEntryType.EVENT)
                diaryDao.insertDiaryEntry(diaryEntry)
            } catch (ex: Exception) {
                Timber.e(ex, "insert event failed")
            }
        }
    }

    override fun observePersons(): Observable<List<DbDiaryPerson>> {
        return diaryDao.observePersons().asDbObservable()
    }

    override fun observeDbDiaryEntry(): Observable<List<DbDiaryEntry>> {
        return diaryDao.observeDbDiaryEntry().asDbObservable()
    }

    override fun observeDbDiaryEntryForDate(date: LocalDate): Observable<List<DbDiaryEntry>> {
        return diaryDao.observeDbDiaryEntryForDate(date).asDbObservable()
    }

    override fun observeDbDiaryEntryForDateWrapper(date: LocalDate): Observable<List<DbDiaryEntryWrapper>> {
        return diaryDao.observeDbDiaryEntryForDateWrapper(date).asDbObservable()
    }

    override suspend fun getDbDiaryEntryForDateWrapper(date: LocalDate): List<DbDiaryEntryWrapper> {
        return diaryDao.getDDiaryEntryForDateWrapper(date)
    }

    override fun deleteDbDiaryEntryById(id: Long) {
        launch(appDispatchers.Default) {
            try {
                diaryDao.deleteDbDiaryEntryById(id)
            } catch (ex: Exception) {
                Timber.e(ex, "delete entry failed")
            }
        }
    }

    override fun observeLocations(): Observable<List<DbDiaryLocation>> {
        return diaryDao.observeLocations().asDbObservable()
    }

    override fun observePublicTransports(): Observable<List<DbDiaryPublicTransport>> {
        return diaryDao.observePublicTransport().asDbObservable()
    }

    override fun observeEvents(): Observable<List<DbDiaryEvent>> {
        return diaryDao.observeEvents().asDbObservable()
    }
}
sealed class ContactEntry {

    object Person : ContactEntry()

    object Location : ContactEntry()

    object PublicTransport : ContactEntry()

    object Event : ContactEntry()
}