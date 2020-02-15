package com.dainv.hichat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dainv.hichat.di.AppComponent
import com.dainv.hichat.ui.main.MainActivity
import com.google.gson.Gson
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var context: Context

    @Inject
    lateinit var appComponent: AppComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        Timber.e("debug $gson")
        Timber.e("debug $appComponent")
    }



}