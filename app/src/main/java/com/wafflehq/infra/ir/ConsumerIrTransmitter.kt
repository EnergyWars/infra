package com.wafflehq.infra.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumerIrTransmitter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : IrTransmitter {

    private val manager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    override val hasEmitter: Boolean
        get() = manager?.hasIrEmitter() == true

    override fun transmit(pattern: IntArray): Boolean {
        val mgr = manager ?: return false
        if (!mgr.hasIrEmitter()) return false
        mgr.transmit(NecCodec.CARRIER_FREQUENCY_HZ, pattern)
        return true
    }
}
