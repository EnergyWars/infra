package com.wafflehq.infra.ir

interface IrTransmitter {
    val hasEmitter: Boolean
    fun transmit(pattern: IntArray): Boolean
}
