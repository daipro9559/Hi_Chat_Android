package com.dainv.hichat.di

import android.app.Application
import com.dainv.hichat.app.HiChatApplication
import com.dainv.hichat.di.modules.ActivityModule
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
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): AppComponent
    }

    fun inject(hiChatApplication: HiChatApplication)
}