package com.wafflehq.infra.di

import com.wafflehq.infra.ir.ConsumerIrTransmitter
import com.wafflehq.infra.ir.IrTransmitter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class IrModule {

    @Binds
    abstract fun bindIrTransmitter(impl: ConsumerIrTransmitter): IrTransmitter
}
