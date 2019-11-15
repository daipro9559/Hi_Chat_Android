package com.dainv.hichat.di


import com.dainv.hichat.MainActivity

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ActivityModule {
    @ContributesAndroidInjector
    internal abstract fun mainActivity(): `fun`
}
