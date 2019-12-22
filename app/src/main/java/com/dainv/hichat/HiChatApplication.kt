package com.dainv.hichat

import android.app.Activity
import android.app.Application
import com.dainv.hichat.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class HiChatApplication :Application(), HasAndroidInjector {
    override fun androidInjector(): AndroidInjector<Any> {
        return dispatchingActivity
    }

    @Inject
    lateinit var dispatchingActivity: DispatchingAndroidInjector<Any>
    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
        DaggerAppComponent.builder()
            .application(this)
            .build()
            .inject(this)
    }


}