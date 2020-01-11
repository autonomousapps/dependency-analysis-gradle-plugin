@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import org.gradle.api.artifacts.component.ComponentIdentifier
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
     * Resolved version. Will be null for project dependencies.
     */
    val resolvedVersion: String? = null
) : Comparable<Dependency> {

    constructor(componentIdentifier: ComponentIdentifier) : this(
        identifier = componentIdentifier.asString(),
        resolvedVersion = componentIdentifier.resolvedVersion()
    )

    /*
     * These overrides all basically say that we don't care about the resolved version for our algorithms. End-users
     * might care, which is why we include it anyway.
     */

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
     * If false, a direct dependency (declared in the `dependencies {}` block). If true, a transitive dependency.
     */
    var isTransitive: Boolean? = null,
    /**
     * Physical artifact on disk; a jar file.
     */
    var file: File
) {
    constructor(componentIdentifier: ComponentIdentifier, file: File) : this(
        dependency = Dependency(componentIdentifier.asString(), componentIdentifier.resolvedVersion()),
        file = file
    )
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
     * onto the classpath (either unintentionally or by design). Unintentional leakage is usually the result of use of
     * the `compile` configuration (or Maven scope); cf the `api` configuration, which "leaks" by design.
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

data class ComponentWithInlineMembers(
    /**
     * A tuple of an `identifier` and a resolved version. See [Dependency].
     */
    val dependency: Dependency,
    /**
     * A set of imports that indicates a possible use of an inline member from this component ([dependency]).
     */
    val imports: Set<String>
) : Comparable<ComponentWithInlineMembers> {
    override fun compareTo(other: ComponentWithInlineMembers): Int = dependency.compareTo(other.dependency)
}
