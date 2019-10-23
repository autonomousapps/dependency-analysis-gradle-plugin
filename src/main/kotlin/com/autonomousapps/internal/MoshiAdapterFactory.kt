package com.autonomousapps.internal

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal val MOSHI: Moshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

internal inline fun <reified T> getJsonAdapter(): JsonAdapter<T> {
    return MOSHI.adapter(T::class.java)
}

internal inline fun <reified T> getJsonListAdapter(): JsonAdapter<List<T>> {
    val type = newParameterizedType(List::class.java, T::class.java)
    return MOSHI.adapter(type)
}

internal inline fun <reified T> String.fromJson(): T {
    return getJsonAdapter<T>().fromJson(this)!!
}

internal inline fun <reified T> T.toJson(): String {
    return getJsonAdapter<T>().toJson(this)
}

internal inline fun <reified T> String.fromJsonList(): List<T> {
    return getJsonListAdapter<T>().fromJson(this)!!
}

internal inline fun <reified T> List<T>.toPrettyString(): String {
    return getJsonListAdapter<T>().indent("  ").toJson(this)
}
