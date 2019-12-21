@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty

private const val ANDROID_LIB_VARIANT_DEFAULT = "debug"
internal const val JAVA_LIB_SOURCE_SET_DEFAULT = "main"

open class DependencyAnalysisExtension(objects: ObjectFactory) {

    internal val theVariants: SetProperty<String> = objects.setProperty()

    private val fallbacks: SetProperty<String> = objects.setProperty()

    init {
        theVariants.convention(listOf(ANDROID_LIB_VARIANT_DEFAULT, JAVA_LIB_SOURCE_SET_DEFAULT))
        fallbacks.set(listOf(ANDROID_LIB_VARIANT_DEFAULT, JAVA_LIB_SOURCE_SET_DEFAULT))
    }

    fun setVariants(vararg v: String) {
        theVariants.set(v.toSet())
    }

    fun getFallbacks() = theVariants.get() + fallbacks.get()
}
