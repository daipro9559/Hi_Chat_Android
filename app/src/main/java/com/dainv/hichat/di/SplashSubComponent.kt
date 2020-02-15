package com.dainv.hichat.di

import com.dainv.hichat.di.modules.SplashActivityModule
import com.dainv.hichat.ui.SplashActivity
import dagger.BindsInstance
import dagger.Subcomponent
import dagger.android.AndroidInjector


/**
 * Created by DaiNV on 1/9/20.
 */

@Subcomponent(modules = [SplashActivityModule::class])
interface SplashSubComponent : AndroidInjector<SplashActivity> {
    @Subcomponent.Factory
    interface Factory : AndroidInjector.Factory<SplashActivity> {
        override fun create(instance: SplashActivity?): AndroidInjector<SplashActivity> {
        }
    }


}