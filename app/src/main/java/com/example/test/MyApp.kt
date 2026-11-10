package com.example.test

import android.app.Application
import com.example.test.data.api.CurrencyExchangeService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var currencyExchangeService: CurrencyExchangeService

    override fun onCreate() {
        super.onCreate()
        // Prefetch currency rates in the background so the first screen that
        // shows converted amounts does not have to wait on the network.
        currencyExchangeService.warmUp()
    }
}
