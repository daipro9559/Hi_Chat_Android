package com.dainv.hichat.di

import android.app.Application
import com.dainv.hichat.app.HiChatApplication
import com.dainv.hichat.di.modules.ActivityModule
import com.dainv.hichat.di.modules.SplashActivityModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [AppModule::class
        , AndroidInjectionModule::class
        , AndroidSupportInjectionModule::class
        , ActivityModule::class
        ]
)
interface AppComponent {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): AppComponent
    }

    fun inject(hiChatApplication: HiChatApplication)

    fun  splashSubcomponentFactory(@BindsInstance test :String):  SplashSubComponent.Factory
}