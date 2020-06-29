package at.roteskreuz.stopcorona.utils

import org.junit.Test

import org.junit.Assert.*
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class DateTimeExtensionsKtTest {

    @Test
    fun millisUntilTheStartOfTheNextUtcDay() {

        val todayNoon: ZonedDateTime = ZonedDateTime.now()
            .withZoneSameLocal(ZoneId.of("UTC+2"))
            .withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        // 12 hours + 2 hours offset 
        assertEquals((12+2)*60*60*1000,todayNoon.millisUntilTheStartOfTheNextUtcDay())

    }
}