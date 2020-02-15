package com.dainv.hichat.di

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dagger.MapKey
import java.lang.annotation.Documented
import kotlin.reflect.KClass


/**
 * Created by DaiNV on 1/10/20.
 */
@Documented
@Target(
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ActivityKey(
   val value : KClass<*>
)