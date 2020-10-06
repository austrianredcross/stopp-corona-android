package at.rotekreuz.stopcorona.hms

import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CalculateTodayEpochMinutesTest {

    @Test
    fun testTodayEpochIsStartOfDay() {
        val seconds = LocalDate.now().atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        Assert.assertTrue(seconds % 86400L == 0L)
    }

}