package com.dainv.hichat.app

import android.app.Application
import timber.log.Timber

class HiChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
//        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
//        }
    }
}