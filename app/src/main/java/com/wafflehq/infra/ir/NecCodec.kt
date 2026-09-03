package com.wafflehq.infra.ir

object NecCodec {

    const val CODE_COUNT = 65536
    const val MAX_INDEX = CODE_COUNT - 1
    const val EXTENDED_CODE_COUNT = CODE_COUNT * 256
    const val EXTENDED_MAX_INDEX = EXTENDED_CODE_COUNT - 1
    const val CARRIER_FREQUENCY_HZ = 38000

    const val REPEAT_COUNT = 2
    const val FRAME_PERIOD_US = 108_000
    const val TYPICAL_FRAME_DURATION_MS: Long = FRAME_PERIOD_US / 1000L * REPEAT_COUNT + 12L

    private const val HEADER_MARK = 9000
    private const val HEADER_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1690
    private const val TRAILING_MARK = 560
    private const val REPEAT_MARK = 9000
    private const val REPEAT_SPACE = 2250

    fun codeCount(extended: Boolean): Int = if (extended) EXTENDED_CODE_COUNT else CODE_COUNT

    fun maxIndex(extended: Boolean): Int = codeCount(extended) - 1

    fun addressOf(index: Int, extended: Boolean = false): Int =
        if (extended) (index shr 8) and 0xFFFF else (index shr 8) and 0xFF

    fun commandOf(index: Int): Int = index and 0xFF

    fun hexOf(index: Int, extended: Boolean = false): String {
        val command = commandOf(index)
        return if (extended) {
            val address = addressOf(index, extended = true)
            "%04X%02X%02X".format(address, command, command.inv() and 0xFF)
        } else {
            val address = addressOf(index)
            "%02X%02X%02X%02X".format(
                address,
                address.inv() and 0xFF,
                command,
                command.inv() and 0xFF,
            )
        }
    }

    fun indexFromHex(hex: String, extended: Boolean = false): Int? {
        if (hex.length != 8) return null
        val bytes = hex.chunked(2).map { it.toIntOrNull(16) ?: return null }
        return if (extended) {
            val address = (bytes[0] shl 8) or bytes[1]
            (address shl 8) or bytes[2]
        } else {
            (bytes[0] shl 8) or bytes[2]
        }
    }

    fun requiresExtended(hex: String): Boolean {
        if (hex.length != 8) return false
        val bytes = hex.chunked(2).map { it.toIntOrNull(16) ?: return false }
        return bytes[1] != bytes[0].inv() and 0xFF
    }

    fun buildFrame(index: Int, extended: Boolean = false): IntArray {
        val command = commandOf(index)
        val bytes = if (extended) {
            val address = addressOf(index, extended = true)
            intArrayOf((address shr 8) and 0xFF, address and 0xFF, command, command.inv() and 0xFF)
        } else {
            val address = addressOf(index)
            intArrayOf(address, address.inv() and 0xFF, command, command.inv() and 0xFF)
        }

        val pattern = IntArray(2 + bytes.size * 8 * 2 + 1)
        var i = 0
        pattern[i++] = HEADER_MARK
        pattern[i++] = HEADER_SPACE
        for (byte in bytes) {
            for (bit in 7 downTo 0) {
                val isOne = (byte shr bit) and 1 == 1
                pattern[i++] = BIT_MARK
                pattern[i++] = if (isOne) ONE_SPACE else ZERO_SPACE
            }
        }
        pattern[i] = TRAILING_MARK
        return pattern
    }

    fun buildTransmission(index: Int, extended: Boolean = false, repeats: Int = REPEAT_COUNT): IntArray {
        val frame = buildFrame(index, extended)
        if (repeats <= 0) return frame
        val repeatCode = intArrayOf(REPEAT_MARK, REPEAT_SPACE, TRAILING_MARK)
        val pattern = ArrayList<Int>(frame.size + repeats * (repeatCode.size + 1))
        frame.forEach { pattern.add(it) }
        pattern.add(FRAME_PERIOD_US - frame.sum())
        for (n in 0 until repeats) {
            repeatCode.forEach { pattern.add(it) }
            if (n < repeats - 1) pattern.add(FRAME_PERIOD_US - repeatCode.sum())
        }
        return pattern.toIntArray()
    }
}
