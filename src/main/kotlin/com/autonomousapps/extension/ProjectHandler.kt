@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   projectProperties {
 *     // (Optional) Specify whether to print advice in typesafe-project-accessors format
 *     // Example: project(":foo:bar:baz") vs. projects.foo.bar.baz
 *     useTypesafeProjectAccessors(true)
 *   }
 * }
 * ```
 */
abstract class ProjectHandler @Inject constructor(objects: ObjectFactory) {

    val useTypesafeProjectAccessors: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    fun useTypesafeProjectAccessors(enable: Boolean) {
        useTypesafeProjectAccessors.set(enable)
    }
}
