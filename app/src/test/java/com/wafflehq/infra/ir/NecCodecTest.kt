package com.wafflehq.infra.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `frame transmits each hex byte most significant bit first`() {
        val index = NecCodec.indexFromHex("20DF10EF")!!
        val frame = NecCodec.buildFrame(index)
        val expectedBits = "20DF10EF"
            .chunked(2)
            .joinToString("") { it.toInt(16).toString(2).padStart(8, '0') }
        val actualBits = (0 until 32).joinToString("") { bitIndex ->
            if (frame[2 + bitIndex * 2 + 1] == 1690) "1" else "0"
        }
        assertEquals(expectedBits, actualBits)
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

    @Test
    fun `extended code count covers 16-bit address times 8-bit command`() {
        assertEquals(65536 * 256, NecCodec.EXTENDED_CODE_COUNT)
        assertEquals(65536 * 256 - 1, NecCodec.EXTENDED_MAX_INDEX)
        assertEquals(NecCodec.CODE_COUNT, NecCodec.codeCount(extended = false))
        assertEquals(NecCodec.EXTENDED_CODE_COUNT, NecCodec.codeCount(extended = true))
        assertEquals(NecCodec.MAX_INDEX, NecCodec.maxIndex(extended = false))
        assertEquals(NecCodec.EXTENDED_MAX_INDEX, NecCodec.maxIndex(extended = true))
    }

    @Test
    fun `extended addressOf reads a full 16-bit address`() {
        assertEquals(0xABCD, NecCodec.addressOf(0xABCD12, extended = true))
        assertEquals(0x12, NecCodec.commandOf(0xABCD12))

        assertEquals(0xFFFF, NecCodec.addressOf(NecCodec.EXTENDED_MAX_INDEX, extended = true))
        assertEquals(0xFF, NecCodec.commandOf(NecCodec.EXTENDED_MAX_INDEX))
    }

    @Test
    fun `extended hexOf keeps the address free of a checksum byte`() {
        assertEquals("ABCD12ED", NecCodec.hexOf(0xABCD12, extended = true))
        assertEquals("FFFFFF00", NecCodec.hexOf(NecCodec.EXTENDED_MAX_INDEX, extended = true))
        assertEquals("000000FF", NecCodec.hexOf(0, extended = true))
    }

    @Test
    fun `extended indexFromHex reconstructs the 16-bit address and command`() {
        assertEquals(0xABCD12, NecCodec.indexFromHex("ABCD12ED", extended = true))
        assertEquals(NecCodec.EXTENDED_MAX_INDEX, NecCodec.indexFromHex("FFFFFF00", extended = true))
        assertEquals(0, NecCodec.indexFromHex("000000FF", extended = true))
    }

    @Test
    fun `extended hexOf and indexFromHex round-trip every byte boundary`() {
        val samples = listOf(0, 1, 255, 256, 65535, 65536, 0xABCD12, NecCodec.EXTENDED_MAX_INDEX)
        for (index in samples) {
            assertEquals(index, NecCodec.indexFromHex(NecCodec.hexOf(index, extended = true), extended = true))
        }
    }

    @Test
    fun `transmission appends repeat codes on the 108 ms NEC period`() {
        val frame = NecCodec.buildFrame(0x1234)
        val transmission = NecCodec.buildTransmission(0x1234, repeats = 2)

        assertEquals(frame.size + 1 + 3 + 1 + 3, transmission.size)
        assertTrue(transmission.copyOfRange(0, frame.size).contentEquals(frame))

        val gapAfterFrame = transmission[frame.size]
        assertEquals(108_000, frame.sum() + gapAfterFrame)

        val firstRepeat = transmission.copyOfRange(frame.size + 1, frame.size + 4)
        assertTrue(firstRepeat.contentEquals(intArrayOf(9000, 2250, 560)))

        val gapAfterRepeat = transmission[frame.size + 4]
        assertEquals(108_000, firstRepeat.sum() + gapAfterRepeat)

        val secondRepeat = transmission.copyOfRange(frame.size + 5, frame.size + 8)
        assertTrue(secondRepeat.contentEquals(intArrayOf(9000, 2250, 560)))
        assertEquals(560, transmission.last())
    }

    @Test
    fun `transmission without repeats is the plain frame`() {
        assertTrue(NecCodec.buildTransmission(0x1234, repeats = 0).contentEquals(NecCodec.buildFrame(0x1234)))
    }

    @Test
    fun `transmission gap stays positive for the longest possible frame`() {
        val transmission = NecCodec.buildTransmission(NecCodec.EXTENDED_MAX_INDEX, extended = true, repeats = 1)
        assertTrue(transmission.all { it > 0 })
    }

    @Test
    fun `default transmission uses the configured repeat count`() {
        val frame = NecCodec.buildFrame(0)
        assertEquals(frame.size + NecCodec.REPEAT_COUNT * 4, NecCodec.buildTransmission(0).size)
    }

    @Test
    fun `requiresExtended detects an address byte that is not inverted`() {
        assertTrue(NecCodec.requiresExtended("020250AF"))
        assertTrue(NecCodec.requiresExtended("ABCD12ED"))
        assertFalse(NecCodec.requiresExtended("20DF10EF"))
        assertFalse(NecCodec.requiresExtended("00FF00FF"))
        assertFalse(NecCodec.requiresExtended("020250"))
        assertFalse(NecCodec.requiresExtended("0202G0AF"))
    }

    @Test
    fun `extended buildFrame does not invert the address bytes`() {
        val frame = NecCodec.buildFrame(0xABCD12, extended = true)
        assertEquals(2 + 32 * 2 + 1, frame.size)

        fun byteAt(byteIndex: Int): Int {
            var value = 0
            for (bit in 0 until 8) {
                val space = frame[2 + (byteIndex * 8 + bit) * 2 + 1]
                if (space == 1690) value = value or (1 shl (7 - bit))
            }
            return value
        }

        assertEquals(0xAB, byteAt(0))
        assertEquals(0xCD, byteAt(1))
        assertEquals(0x12, byteAt(2))
        assertEquals(0x12.inv() and 0xFF, byteAt(3))
    }
}
