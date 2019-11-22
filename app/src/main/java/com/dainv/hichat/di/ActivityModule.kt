package com.dainv.hichat.di


import android.telecom.Call
import com.dainv.hichat.CallActivity
import com.dainv.hichat.MainActivity

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module()
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun callActivity(): CallActivity
}
