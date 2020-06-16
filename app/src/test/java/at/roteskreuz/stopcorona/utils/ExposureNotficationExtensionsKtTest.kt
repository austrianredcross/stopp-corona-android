package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import org.junit.Test

import org.junit.Assert.*
import org.threeten.bp.ZonedDateTime

class ExposureNotficationExtensionsKtTest {

    private val YESTERDAY = ZonedDateTime.now().minusDays(1)

    private val THRESHOLD = 100

    private val RED_YESTERDAY: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochSecond() * 1000)
        .setTransmissionRiskLevel(WarningType.RED.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD)
        .build()

    private val RED_TWO_DAYS_AGO: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochSecond() * 1000)
        .setTransmissionRiskLevel(WarningType.RED.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD)
        .build()

    private val SHORT_YELLOW_TWO_DAYS_AGO: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochSecond() * 1000)
        .setTransmissionRiskLevel(WarningType.YELLOW.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD / 2)
        .build()

    @Test
    fun testEmpty() {
        val listUnderTest: List<ExposureInformation> = emptyList()

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNull(dates.firstYellowDay)
        assertNull(dates.firstRedDay)
    }

    @Test
    fun ignoreSingleUnriskyYellow() {
        val listUnderTest: List<ExposureInformation> = arrayListOf(SHORT_YELLOW_TWO_DAYS_AGO)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNull(dates.firstYellowDay)
        assertNull(dates.firstRedDay)
    }

    @Test
    fun twoUnriskyTriggerYellowWarning() {
        val listUnderTest: List<ExposureInformation> = arrayListOf(SHORT_YELLOW_TWO_DAYS_AGO, SHORT_YELLOW_TWO_DAYS_AGO)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNotNull(dates.firstYellowDay)
        assertTrue(dates.firstYellowDay!!.areOnTheSameDay(YESTERDAY))
        assertNull(dates.firstRedDay)
    }

    @Test
    fun detectSimpleRed() {

        val listUnderTest: List<ExposureInformation> = arrayListOf(RED_YESTERDAY)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assert(dates.firstYellowDay == null)
        assertNotNull(dates.firstRedDay)
        assert(dates.firstRedDay!!.areOnTheSameDay(YESTERDAY))
    }

    @Test
    fun detectTwoRed_detectLatest_orderIrrelevant() {

        val listUnderTest: List<ExposureInformation> = arrayListOf(RED_TWO_DAYS_AGO, RED_YESTERDAY)
        val listUnderTest2: List<ExposureInformation> = arrayListOf(RED_YESTERDAY, RED_YESTERDAY)

        arrayListOf(listUnderTest, listUnderTest2).forEach {
            val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

            assert(dates.firstYellowDay == null)
            assertNotNull(dates.firstRedDay)
            assert(dates.firstRedDay!!.areOnTheSameDay(YESTERDAY))
        }
    }
}