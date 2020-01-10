package com.dainv.hichat.di.modules

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.dainv.hichat.di.ActivityKey
import com.dainv.hichat.di.SplashSubComponent
import com.dainv.hichat.ui.SplashActivity
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap


/**
 * Created by DaiNV on 1/9/20.
 */

@Suppress("unused")
@Module(subcomponents = [SplashSubComponent::class])
abstract class ActivityModule {

    @Binds
    @IntoMap
    @ActivityKey(SplashActivity::class)
    abstract fun bindYourAndroidInjectorFactory(factory: SplashSubComponent.Builder): AndroidInjector.Factory<out Activity>
}