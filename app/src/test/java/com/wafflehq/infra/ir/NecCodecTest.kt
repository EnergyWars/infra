package com.wafflehq.infra.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NecCodecTest {

    @Test
    fun `code count covers exactly 16 bits`() {
        assertEquals(65536, NecCodec.CODE_COUNT)
        assertEquals(65535, NecCodec.MAX_INDEX)
    }

    @Test
    fun `addressOf and commandOf split the index into two bytes`() {
        assertEquals(0, NecCodec.addressOf(0))
        assertEquals(0, NecCodec.commandOf(0))

        assertEquals(0, NecCodec.addressOf(255))
        assertEquals(255, NecCodec.commandOf(255))

        assertEquals(1, NecCodec.addressOf(256))
        assertEquals(0, NecCodec.commandOf(256))

        assertEquals(255, NecCodec.addressOf(65535))
        assertEquals(255, NecCodec.commandOf(65535))

        assertEquals(0x12, NecCodec.addressOf(0x1234))
        assertEquals(0x34, NecCodec.commandOf(0x1234))
    }

    @Test
    fun `frame has header, 32 data bits and a trailing mark`() {
        val frame = NecCodec.buildFrame(0)
        assertEquals(2 + 32 * 2 + 1, frame.size)
    }

    @Test
    fun `frame starts with the standard NEC header`() {
        val frame = NecCodec.buildFrame(0x1234)
        assertEquals(9000, frame[0])
        assertEquals(4500, frame[1])
    }

    @Test
    fun `frame ends with a trailing mark`() {
        val frame = NecCodec.buildFrame(0x1234)
        assertEquals(560, frame.last())
    }

    @Test
    fun `zero index encodes every bit as zero-space, including the inverted bytes`() {
        val frame = NecCodec.buildFrame(0)
        for (bitIndex in 0 until 32) {
            val mark = frame[2 + bitIndex * 2]
            val space = frame[2 + bitIndex * 2 + 1]
            assertEquals(560, mark)
            val isInvertedByte = bitIndex in 8 until 16 || bitIndex in 24 until 32
            val expectedSpace = if (isInvertedByte) 1690 else 560
            assertEquals(expectedSpace, space)
        }
    }

    @Test
    fun `every data bit mark is the standard NEC bit mark`() {
        val frame = NecCodec.buildFrame(0xABCD)
        for (bitIndex in 0 until 32) {
            assertEquals(560, frame[2 + bitIndex * 2])
        }
    }

    @Test
    fun `every space is either the zero-space or one-space duration`() {
        val frame = NecCodec.buildFrame(0xABCD)
        for (bitIndex in 0 until 32) {
            val space = frame[2 + bitIndex * 2 + 1]
            assertTrue(space == 560 || space == 1690)
        }
    }

    @Test
    fun `different indices produce different frames`() {
        val a = NecCodec.buildFrame(100)
        val b = NecCodec.buildFrame(101)
        assertTrue(!a.contentEquals(b))
    }

    @Test
    fun `hexOf encodes address, inverted address, command and inverted command`() {
        assertEquals("FF00FF00", NecCodec.hexOf(65535))
        assertEquals("00FF00FF", NecCodec.hexOf(0))
        assertEquals("12ED34CB", NecCodec.hexOf(0x1234))
    }

    @Test
    fun `indexFromHex reconstructs the index from address and command bytes`() {
        assertEquals(65535, NecCodec.indexFromHex("FF00FF00"))
        assertEquals(0, NecCodec.indexFromHex("00FF00FF"))
        assertEquals(0x1234, NecCodec.indexFromHex("12ED34CB"))
        assertEquals(0x1234, NecCodec.indexFromHex("12ed34cb".uppercase()))
    }

    @Test
    fun `indexFromHex rejects malformed input`() {
        assertEquals(null, NecCodec.indexFromHex("FF00FF"))
        assertEquals(null, NecCodec.indexFromHex("FF00FF0G"))
        assertEquals(null, NecCodec.indexFromHex(""))
    }

    @Test
    fun `hexOf and indexFromHex round-trip every byte boundary`() {
        val samples = listOf(0, 1, 255, 256, 65535, 0xABCD)
        for (index in samples) {
            assertEquals(index, NecCodec.indexFromHex(NecCodec.hexOf(index)))
        }
    }
}
