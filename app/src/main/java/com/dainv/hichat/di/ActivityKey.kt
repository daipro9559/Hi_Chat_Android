package com.dainv.hichat.di

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dagger.MapKey
import kotlin.reflect.KClass


/**
 * Created by DaiNV on 1/10/20.
 */
@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ActivityKey(
   val valaue : KClass<out AppCompatActivity>
)