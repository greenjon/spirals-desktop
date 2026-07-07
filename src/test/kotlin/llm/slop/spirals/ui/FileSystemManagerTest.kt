package llm.slop.spirals.ui

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File
import kotlin.io.path.createTempDirectory

class FileSystemManagerTest {

    @AfterTest
    fun tearDown() {
        FileSystemManager.clearScanCache()
    }

    @Test
    fun testScanDirectoryRefreshesWhenDirectoryContentsChange() {
        val directory = createTempDirectory().toFile()

        assertEquals(emptyList(), FileSystemManager.scanDirectory(directory))

        File(directory, "first.lsd").writeText("{}")
        val firstScan = FileSystemManager.scanDirectory(directory)
        assertEquals(listOf("first"), firstScan.map { it.name })

        File(directory, "second.lsd").writeText("{}")
        val secondScan = FileSystemManager.scanDirectory(directory)
        assertEquals(listOf("first", "second"), secondScan.map { it.name })
    }
}
