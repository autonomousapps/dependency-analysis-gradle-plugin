@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import java.io.File

internal data class Artifact(
    /**
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val identifier: String,
    /**
     * Library (e.g., downloaded from jcenter) or a project ("module" in a multi-module project).
     */
    val componentType: ComponentType,
    /**
     * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
     */
    var isTransitive: Boolean? = null,
    /**
     * Physical artifact on disk; a jar file.
     */
    var file: File? = null
) {

    constructor(componentIdentifier: ComponentIdentifier, file: File? = null) : this(
        identifier = componentIdentifier.asString(),
        componentType = ComponentType.of(componentIdentifier),
        file = file
    )
}

private fun ComponentIdentifier.asString(): String {
    return when (this) {
        is ProjectComponentIdentifier -> projectPath
        is ModuleComponentIdentifier -> moduleIdentifier.toString()
        else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}")
    }
}

internal enum class ComponentType {
    /**
     * A 3rd-party dependency.
     */
    LIBRARY,
    /**
     * A project dependency, aka a "module" in a multi-module or multi-project build.
     */
    PROJECT;

    companion object {
        fun of(componentIdentifier: ComponentIdentifier) = when (componentIdentifier) {
            is ModuleComponentIdentifier -> LIBRARY
            is ProjectComponentIdentifier -> PROJECT
            else -> throw GradleException("'This shouldn't happen'")
        }
    }
}

// TODO misnamed. Can also be a Project.
internal data class Library(
    /**
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val identifier: String,
    /**
     * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
     */
    val isTransitive: Boolean,
    /**
     * The classes declared by this library.
     */
    val classes: List<String> // TODO Set
) : Comparable<Library> {

    override fun compareTo(other: Library): Int {
        return identifier.compareTo(other.identifier)
    }
}

/**
 * Represents a "mis-used" transitive dependency. The [identifier] is the unique name, and the [usedTransitiveClasses]
 * are the class members of the dependency that are used directly (which shouldn't be).
 */
internal data class TransitiveDependency(
    /**
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val identifier: String,
    /**
     * These are class members of this dependency that are used directly by the project in question. They have leaked
     * onto the classpath.
     */
    val usedTransitiveClasses: List<String> // TODO Set
)