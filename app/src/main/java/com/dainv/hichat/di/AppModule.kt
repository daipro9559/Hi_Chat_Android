package com.dainv.hichat.di

import android.app.Application
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [])
class AppModule{

    @Provides
    @Singleton
    fun provideContext(app: Application) = app.applicationContext

    @Provides
    @Singleton
    fun provideGson() = Gson()

}