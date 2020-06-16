package at.roteskreuz.stopcorona.model.entities.session

import org.junit.Test;

class DbFullBatchPartTest {

    @Test
    fun getIntervalStart() {
        val part1 = DbFullBatchPart(
            token= "egal",
            batchNumber = 1,
            intervalStart = 1,
            path = "/"
        )
        val part2 = DbFullBatchPart(
            token= "egal",
            batchNumber = 2,
            intervalStart = 1,
            path = "/"
        )
        val unsortedList = arrayListOf(part2,part1)
        val sorted = unsortedList.sortedWith( compareBy{ it.batchNumber })
        assert(sorted[0].batchNumber < sorted[1].batchNumber, { "sorting failed" })
    }
}