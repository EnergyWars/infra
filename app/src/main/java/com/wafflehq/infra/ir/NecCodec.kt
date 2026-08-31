package com.wafflehq.infra.ir

object NecCodec {

    const val CODE_COUNT = 65536
    const val MAX_INDEX = CODE_COUNT - 1
    const val CARRIER_FREQUENCY_HZ = 38000

    private const val HEADER_MARK = 9000
    private const val HEADER_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1690
    private const val TRAILING_MARK = 560

    fun addressOf(index: Int): Int = (index shr 8) and 0xFF

    fun commandOf(index: Int): Int = index and 0xFF

    fun buildFrame(index: Int): IntArray {
        val address = addressOf(index)
        val command = commandOf(index)
        val bytes = intArrayOf(
            address,
            address.inv() and 0xFF,
            command,
            command.inv() and 0xFF,
        )

        val pattern = IntArray(2 + bytes.size * 8 * 2 + 1)
        var i = 0
        pattern[i++] = HEADER_MARK
        pattern[i++] = HEADER_SPACE
        for (byte in bytes) {
            for (bit in 0 until 8) {
                val isOne = (byte shr bit) and 1 == 1
                pattern[i++] = BIT_MARK
                pattern[i++] = if (isOne) ONE_SPACE else ZERO_SPACE
            }
        }
        pattern[i] = TRAILING_MARK
        return pattern
    }
}
