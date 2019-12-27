@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import java.io.File

// TODO these names could be better

/**
 * Basically a tuple of [identifier] and [resolvedVersion]. The latter will be null for project dependencies.
 */
data class Dependency(
    /**
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val identifier: String,
    /**
     * Resolved version. Will be null for project ([ComponentType.PROJECT]) dependencies.
     */
    val resolvedVersion: String? = null
) : Comparable<Dependency> {

    override fun compareTo(other: Dependency): Int = identifier.compareTo(other.identifier)

    override fun toString(): String {
        return if (resolvedVersion != null) {
            "$identifier:$resolvedVersion"
        } else {
            identifier
        }
    }

    /**
     * We only care about the [identifier] for equality comparisons.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dependency

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int = identifier.hashCode()
}

/**
 * Primarily used as a pointer to a [file] on disk; a physical artifact.
 */
data class Artifact(
    /**
     * A tuple of an `identifier` and a resolved version. See [Dependency].
     */
    val dependency: Dependency,
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
        dependency = Dependency(componentIdentifier.asString(), componentIdentifier.resolvedVersion()),
        componentType = ComponentType.of(componentIdentifier),
        file = file
    )
}

/**
 * TODO Currently only used in the artifacts report. Uncertain value.
 */
enum class ComponentType {
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

/**
 * A library or project, along with the set of classes declared by this component.
 */
data class Component(
    /**
     * A tuple of an `identifier` and a resolved version. See [Dependency].
     */
    val dependency: Dependency,
    /**
     * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
     */
    val isTransitive: Boolean,
    /**
     * The classes declared by this library.
     */
    val classes: Set<String>
) : Comparable<Component> {
    override fun compareTo(other: Component): Int = dependency.compareTo(other.dependency)
}

/**
 * Represents a "mis-used" transitive dependency. The [dependency] represents the unique name
 * (see [Dependency.identifier]), and the [usedTransitiveClasses] are the class members of the dependency that are used
 * directly (which "shouldn't" be).
 */
data class TransitiveComponent(
    /**
     * A tuple of an `identifier` and a resolved version. See [Dependency].
     */
    val dependency: Dependency,
    /**
     * These are class members of this dependency that are used directly by the project in question. They have leaked
     * onto the classpath.
     */
    val usedTransitiveClasses: Set<String>
)

/**
 * Represents a dependency ([Dependency.identifier]) that is declared in the `dependencies {}` block of a build script.
 * This dependency is unused and has zero or more transitive dependencies that _are_ used ([usedTransitiveDependencies])
 */
data class UnusedDirectComponent(
    /**
     * A tuple of an `identifier` and a resolved version. See [Dependency].
     */
    val dependency: Dependency,
    /**
     * If this direct dependency has any transitive dependencies that are used, they will be in this set.
     *
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val usedTransitiveDependencies: MutableSet<Dependency>
)
