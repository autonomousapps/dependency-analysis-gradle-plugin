package com.autonomousapps

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

val MOSHI: Moshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(TypeAdapters())
        .build()
}

inline fun <reified T> getJsonAdapter(): JsonAdapter<T> {
    return MOSHI.adapter(T::class.java)
}

inline fun <reified T> getJsonListAdapter(): JsonAdapter<List<T>> {
    val type = newParameterizedType(List::class.java, T::class.java)
    return MOSHI.adapter(type)
}

inline fun <reified T> getJsonSetAdapter(): JsonAdapter<Set<T>> {
    val type = newParameterizedType(Set::class.java, T::class.java)
    return MOSHI.adapter(type)
}

inline fun <reified K, reified V> getJsonMapAdapter(): JsonAdapter<Map<K, V>> {
    val type = newParameterizedType(Map::class.java, K::class.java, V::class.java)
    return MOSHI.adapter(type)
}

inline fun <reified T> String.fromJson(): T {
    return getJsonAdapter<T>().fromJson(this)!!
}

inline fun <reified T> T.toJson(): String {
    return getJsonAdapter<T>().toJson(this)
}

inline fun <reified T> String.fromJsonList(): List<T> {
    return getJsonListAdapter<T>().fromJson(this)!!
}

inline fun <reified T> List<T>.toPrettyString(): String {
    return getJsonListAdapter<T>().indent("  ").toJson(this)
}

inline fun <reified T> Set<T>.toPrettyString(): String {
    return getJsonSetAdapter<T>().indent("  ").toJson(this)
}

inline fun <reified K, reified V> Map<K, V>.toPrettyString(): String {
    return getJsonMapAdapter<K, V>().indent("  ").toJson(this)
}

@Suppress("unused", "HasPlatformType")
internal class TypeAdapters {

    @ToJson fun fileToJson(file: File) = file.absolutePath
    @FromJson fun fileFromJson(absolutePath: String) = File(absolutePath)
}
