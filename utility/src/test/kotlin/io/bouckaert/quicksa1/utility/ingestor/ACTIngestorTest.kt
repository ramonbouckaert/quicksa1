package io.bouckaert.quicksa1.utility.ingestor

import io.bouckaert.quicksa1.shared.db.enums.BlockType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ACTIngestorTest {

    @Test
    fun `parses block type`() {
        assertEquals(
            BlockType.RESIDENTIAL,
            ACTIngestor.parseBlockType("RZ1: SUBURBAN", false)
        )
        assertEquals(
            BlockType.PARK,
            ACTIngestor.parseBlockType("PRZ1: URBAN OPEN SPACE", false)
        )
        assertEquals(
            BlockType.COMMUNITY,
            ACTIngestor.parseBlockType("CF: COMMUNITY FACILITIES", false)
        )
        assertEquals(
            BlockType.COMMERCIAL,
            ACTIngestor.parseBlockType("CZ3: SERVICES ZONE", false)
        )
        assertEquals(
            BlockType.WATER,
            ACTIngestor.parseBlockType("PRZ1: URBAN OPEN SPACE", true)
        )
    }
}