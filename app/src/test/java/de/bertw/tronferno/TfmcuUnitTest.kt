package de.bertw.tronferno

import org.junit.Assert.*
import org.junit.Test

public class TfmcuUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    @Throws(Exception::class)
    fun parseTimerJson_isCorrect() {
       val td = ferParseReceivedTimerJson("""
            {"g":2,"m":3,"f":"mRSA","daily":"0712-"}
        """.trimIndent())
        assertEquals(2,td.g)
        assertEquals(3,td.m)
        assertEquals("0712-",td.daily)
        assertTrue(td.hasAstro)
        assertTrue(td.random)
        assertFalse(td.manual)
        assertEquals("",td.weekly)

        val tdJson = td.toJson()
        assertEquals("""
            "auto":{"g":2,"m":3,"f":"imSR","astro":0,"daily":"0712-"}
        """.trimIndent(),tdJson)
    }

    @Test
    @Throws(Exception::class)
    fun parseTimerJson2_isCorrect() {
        val td = ferParseReceivedTimerJson2("""
            {"auto":{"auto23":{"f":"mRSA","daily":"0712-"}}}
        """.trimIndent())
        assertEquals(2,td.g)
        assertEquals(3,td.m)
        assertEquals("0712-",td.daily)
        assertTrue(td.hasAstro)
        assertTrue(td.random)
        assertFalse(td.manual)
        assertEquals("",td.weekly)

        val tdJson = td.toJson()
        assertEquals("""
            "auto":{"g":2,"m":3,"f":"imSR","astro":0,"daily":"0712-"}
        """.trimIndent(),tdJson)
    }


    @Test
    @Throws(Exception::class)
    fun getSettings_isCorrect() {
        val ts = TfmcuMcuSettings(geta=arrayOf("longitude", "latitude", "tz", "wlan-ssid"))
        assertEquals("""
            "config":{"longitude":"?","latitude":"?","tz":"?","wlan-ssid":"?"}
        """.trimIndent(), ts.toJson())
        assertEquals("""
            {"config":{"longitude":"?","latitude":"?","tz":"?","wlan-ssid":"?"}}
        """.trimIndent(), ts.toString())
    }


    @Test
    @Throws(Exception::class)
    fun parsePosJson_isCorrect() {
        val positions = TfmcuPositions()
        ferParseReceivedPositionJson(positions,"""
            {"from":"tfmcu","pct":{"21":51,"11":100,"22":100,"12":89,"13":35,"15":35}}
        """.trimIndent())
        assertEquals(51,positions.getPos(2,1))
        assertEquals(100,positions.getPos(1,1))
        assertEquals(100,positions.getPos(2,2))
        assertEquals(89,positions.getPos(1,2))
        assertEquals(35,positions.getPos(1,3))
        assertEquals(35,positions.getPos(1,5))

        ferParseReceivedPositionJson(positions,"""
            {"from":"tfmcu","pct":{"21":30}}
        """.trimIndent())
        assertEquals(30,positions.getPos(2,1))
        assertEquals(100,positions.getPos(1,1))
        assertEquals(100,positions.getPos(2,2))
        assertEquals(89,positions.getPos(1,2))
        assertEquals(35,positions.getPos(1,3))
        assertEquals(35,positions.getPos(1,5))
    }
}


