package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.diary.*
import io.reactivex.Flowable
import org.threeten.bp.LocalDate

/**
 * Dao to manage [DbDiaryPerson] and [DbDiaryLocation] for the diary.
 */
@Dao
abstract class DiaryDao {

    @Insert
    abstract suspend fun insertPerson(diaryPerson: DbDiaryPerson) : Long

    @Insert
    abstract suspend fun insertLocation(diaryLocation: DbDiaryLocation) : Long

    @Insert
    abstract suspend fun insertPublicTransport(diaryPublicTransport: DbDiaryPublicTransport) : Long

    @Insert
    abstract suspend fun insertEvent(diaryEvent: DbDiaryEvent) : Long

    @Insert
    abstract suspend fun insertDiaryEntry(diaryEntry: DbDiaryEntry) : Long

    @Query("SELECT * FROM diary_person")
    abstract fun observePersons() : Flowable<List<DbDiaryPerson>>

    @Query("SELECT * FROM diary_location")
    abstract fun observeLocations() : Flowable<List<DbDiaryLocation>>

    @Query("SELECT * FROM diary_public_transport")
    abstract fun observePublicTransport() : Flowable<List<DbDiaryPublicTransport>>

    @Query("SELECT * FROM diary_event")
    abstract fun observeEvents() : Flowable<List<DbDiaryEvent>>

    @Query("SELECT * FROM diary_person WHERE id = :id")
    abstract suspend fun getPersonById(id: Long) : DbDiaryPerson

    @Query("SELECT * FROM diary_location WHERE id = :id")
    abstract suspend fun getLocationById(id: Long) : DbDiaryLocation

    @Query("SELECT * FROM diary_public_transport WHERE id = :id")
    abstract suspend fun getPublicTransportById(id: Long) : DbDiaryPublicTransport

    @Query("SELECT * FROM diary_event WHERE id = :id")
    abstract suspend fun getEventById(id: Long) : DbDiaryEvent


    @Query("SELECT * FROM diary_entry")
    abstract fun observeDbDiaryEntry() : Flowable<List<DbDiaryEntry>>

    @Query("SELECT * FROM diary_entry WHERE date = :date")
    abstract fun observeDbDiaryEntryForDate(date: LocalDate) : Flowable<List<DbDiaryEntry>>

    @Query("SELECT * FROM diary_entry WHERE date = :date")
    abstract fun observeDbDiaryEntryForDateWrapper(date: LocalDate) : Flowable<List<DbDiaryEntryWrapper>>

    @Query("SELECT * FROM diary_entry WHERE date = :date")
    abstract suspend fun getDDiaryEntryForDateWrapper(date: LocalDate) : List<DbDiaryEntryWrapper>

    @Query("DELETE FROM diary_entry WHERE id = :id")
    abstract fun deleteDbDiaryEntryById(id: Long)

}