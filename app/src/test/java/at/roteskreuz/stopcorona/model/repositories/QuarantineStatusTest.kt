package at.roteskreuz.stopcorona.model.repositories

import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

/**
 * Assuming on the 22nd of June we´re quarantined for the next 7 days:
 * 22.06. - Tag 0 (der zählt nicht) -> noch 8 Tage
23.06. - Tag 1-> noch 7 Tage
24.06. - Tag 2-> noch 6 Tage
25.06. - Tag 3-> noch 5 Tage
26.06. - Tag 4-> noch 4 Tage
27.06. - Tag 5-> noch 3 Tage
28.06. - Tag 6 -> noch 2 Tage
29.06. - Tag 7 -> noch 1 Tag
 */
class QuarantineStatusTest {

    @Test
    fun `days until end of quarantine today needs to be 1`() {
        val endsToday = ZonedDateTime.now().plusHours(1)
        val limited = QuarantineStatus.Jailed.Limited(end = endsToday, bySelfYellowDiagnosis = null, byRedWarning = null, byYellowWarning = endsToday)

        assertEquals(limited.daysUntilEnd(), 1)
    }

    @Test
    fun `7 days of quarantine actually show as 8 days`() {
        val endsToday = ZonedDateTime.now().plusDays(7)
        val limited = QuarantineStatus.Jailed.Limited(end = endsToday, bySelfYellowDiagnosis = null, byRedWarning = null, byYellowWarning = endsToday)

        assertEquals(limited.daysUntilEnd(), 8)
    }
}

