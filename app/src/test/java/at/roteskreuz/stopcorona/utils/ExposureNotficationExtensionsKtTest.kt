package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import org.junit.Assert.*
import org.junit.Test
import org.threeten.bp.Instant

class ExposureNotficationExtensionsKtTest {

    private val YESTERDAY = Instant.now().minusDays(1)

    private val THRESHOLD = 100

    private val RED_YESTERDAY: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochMilli())
        .setTransmissionRiskLevel(WarningType.RED.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD)
        .build()

    private val RED_TWO_DAYS_AGO: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochMilli())
        .setTransmissionRiskLevel(WarningType.RED.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD)
        .build()

    private val SHORT_YELLOW_TWO_DAYS_AGO: ExposureInformation = ExposureInformation.ExposureInformationBuilder()
        .setDateMillisSinceEpoch(YESTERDAY.toEpochMilli())
        .setTransmissionRiskLevel(WarningType.YELLOW.transmissionRiskLevel)
        .setTotalRiskScore(THRESHOLD / 2)
        .build()

    @Test
    fun `no exposures lead to no exposure dates`() {
        val listUnderTest: List<ExposureInformation> = emptyList()

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNull(dates.firstYellowDay)
        assertNull(dates.firstRedDay)
    }

    @Test
    fun `a single yellow exposure information leads to a the yellow date on that day`() {
        val listUnderTest: List<ExposureInformation> = arrayListOf(SHORT_YELLOW_TWO_DAYS_AGO)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNull(dates.firstYellowDay)
        assertNull(dates.firstRedDay)
    }

    @Test
    fun `two individual yellow exposures, individually not relevant must lead to a combined yellow warning`() {
        val listUnderTest: List<ExposureInformation> = arrayListOf(SHORT_YELLOW_TWO_DAYS_AGO, SHORT_YELLOW_TWO_DAYS_AGO)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assertNotNull(dates.firstYellowDay)
        assertTrue(dates.firstYellowDay.areOnTheSameUtcDay(YESTERDAY))
        assertNull(dates.firstRedDay)
    }

    @Test
    fun `detect a single red exposure day`() {

        val listUnderTest: List<ExposureInformation> = arrayListOf(RED_YESTERDAY)

        val dates = listUnderTest.extractLatestRedAndYellowContactDate(THRESHOLD)

        assert(dates.firstYellowDay == null)
        assertNotNull(dates.firstRedDay)
        assert(dates.firstRedDay.areOnTheSameUtcDay(YESTERDAY))
    }

    @Test
    fun `detect the latest red day irrelevant of the order of the exposures in the list of exposures`() {

        val listUnderTest: List<ExposureInformation> = arrayListOf(RED_TWO_DAYS_AGO, RED_YESTERDAY)
        val listUnderTest2: List<ExposureInformation> = arrayListOf(RED_YESTERDAY, RED_YESTERDAY)

        arrayListOf(listUnderTest, listUnderTest2).forEach {
            val dates = it.extractLatestRedAndYellowContactDate(THRESHOLD)

            assert(dates.firstYellowDay == null)
            assertNotNull(dates.firstRedDay)
            assert(dates.firstRedDay.areOnTheSameUtcDay(YESTERDAY))
        }
    }
}