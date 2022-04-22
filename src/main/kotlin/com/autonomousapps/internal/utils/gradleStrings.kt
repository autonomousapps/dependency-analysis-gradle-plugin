package com.autonomousapps.internal.utils

import com.autonomousapps.model.*
import org.gradle.api.GradleException
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Category
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier

/** Converts this [ResolvedDependencyResult] to group-artifact-version (GAV) coordinates in a tuple of (GA, V?). */
internal fun ResolvedDependencyResult.toCoordinates(): Coordinates {
  return compositeRequest() ?: selected.id.toCoordinates()
}

/** If this is a composite substitution, returns it as such. We care about the request as well as the result. */
private fun ResolvedDependencyResult.compositeRequest(): IncludedBuildCoordinates? {
  if (!selected.selectionReason.isCompositeSubstitution) return null
  val requestedModule = requested as? ModuleComponentSelector ?: return null

  val requested = ModuleCoordinates(
    identifier = requestedModule.moduleIdentifier.toString(),
    resolvedVersion = requestedModule.version
  )
  val resolved = ProjectCoordinates((selected.id as ProjectComponentIdentifier).identityPath())

  return IncludedBuildCoordinates.of(requested, resolved)
}

private fun ProjectComponentIdentifier.identityPath(): String {
  return (this as? DefaultProjectComponentIdentifier)?.identityPath?.toString()
    ?: error("${toCoordinates()} is not a DefaultProjectComponentIdentifier")
}

internal fun ResolvedArtifactResult.toCoordinates(): Coordinates {
  val resolved = id.componentIdentifier.toCoordinates()

  // Doesn't resolve to a project, so can't be an included build. Return as-is.
  if (resolved !is ProjectCoordinates) return resolved

  // may be a composite substitution
  val identity = ProjectCoordinates((id.componentIdentifier as ProjectComponentIdentifier).identityPath())

  // Identity path matches project path, so we assume this isn't resolved from an included build, and return as-is.
  if (resolved == identity) return resolved

  // At this point, we think this module has resolved from an included build.

  // This is a very naive heuristic. Doesn't work for Gradle < 7.2, where capabilities is empty.
  val requested = variant.capabilities.firstOrNull()?.let { c ->
    c.version?.let { v ->
      ModuleCoordinates(
        identifier = "${c.group}:${c.name}",
        resolvedVersion = v
      )
    }
  } ?: return resolved

  return IncludedBuildCoordinates.of(
    requested = requested,
    resolvedProject = identity
  )
}

/** Returns the [coordinates][Coordinates] of the root of [this][Configuration]. */
internal fun Configuration.rootCoordinates(): Coordinates {
  return incoming
    .resolutionResult
    .root
    .id
    .toCoordinates()
}

/** Converts this [ComponentIdentifier] to group-artifact-version (GAV) coordinates in a tuple of (GA, V?). */
private fun ComponentIdentifier.toCoordinates(): Coordinates {
  val identifier = toIdentifier()
  return when (this) {
    is ProjectComponentIdentifier -> ProjectCoordinates(identifier)
    is ModuleComponentIdentifier -> {
      resolvedVersion()?.let { resolvedVersion ->
        ModuleCoordinates(identifier, resolvedVersion)
      } ?: FlatCoordinates(identifier)
    }
    else -> FlatCoordinates(identifier)
  }
}

/**
 * Convert this [ComponentIdentifier] to a group-artifact identifier, such as "org.jetbrains.kotlin:kotlin-stdlib" in
 * the case of an external module, or a project identifier, such as ":foo:bar", in the case of an internal module.
 *
 * TODO: should be private, but still in use by legacy code.
 */
internal fun ComponentIdentifier.toIdentifier(): String = when (this) {
  is ProjectComponentIdentifier -> projectPath
  is ModuleComponentIdentifier -> {
    // flat JAR/AAR files have no group. I don't trust that, if absent, it will be blank rather
    // than null.
    @Suppress("UselessCallOnNotNull")
    if (moduleIdentifier.group.isNullOrBlank()) moduleIdentifier.name
    else moduleIdentifier.toString()
  }
  // e.g. "Gradle API"
  is OpaqueComponentIdentifier -> displayName
  // for a file dependency
  is OpaqueComponentArtifactIdentifier -> displayName
  else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
}.intern()

/**
 * Gets the resolved version from this [ComponentIdentifier]. Note that this may be different from the version
 * requested.
 *
 * TODO: should be private, but still in use by legacy code.
 */
internal fun ComponentIdentifier.resolvedVersion(): String? = when (this) {
  is ProjectComponentIdentifier -> null
  is ModuleComponentIdentifier -> {
    // flat JAR/AAR files have no version, but rather than null, it's empty.
    version.ifBlank { null }
  }
  // e.g. "Gradle API"
  is OpaqueComponentIdentifier -> null
  // for a file dependency
  is OpaqueComponentArtifactIdentifier -> null
  else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
}?.intern()

/**
 * Given [Configuration.getDependencies], return this dependency set as a set of identifiers, per
 * [ComponentIdentifier.toIdentifier].
 */
internal fun DependencySet.toIdentifiers(): Set<Pair<String, Boolean>> = mapNotNullToSet {
  it.toIdentifier()
}

internal fun Dependency.toCoordinates(): Coordinates? {
  val identifier = toIdentifier() ?: return null
  return when (this) {
    is ProjectDependency -> ProjectCoordinates(identifier.first)
    is ModuleDependency -> {
      resolvedVersion()?.let { resolvedVersion ->
        ModuleCoordinates(identifier.first, resolvedVersion)
      } ?: FlatCoordinates(identifier.first)
    }
    else -> FlatCoordinates(identifier.first)
  }
}

/**
 * Given a [Dependency] retrieved from a [Configuration], return it as an identifier, per
 * [ComponentIdentifier.toIdentifier].
 */
internal fun Dependency.toIdentifier(): Pair<String, Boolean>? = when (this) {
  is ProjectDependency -> {
    val identifier = dependencyProject.path
    val notMainFeature = isDependencyToJavaPlatform() || isDependencyToNonMainFeature()
    Pair(identifier.intern(), notMainFeature)
  }
  is ModuleDependency -> {
    // flat JAR/AAR files have no group.
    val identifier = if (group != null) "${group}:${name}" else name
    val notMainFeature = isDependencyToJavaPlatform() || isDependencyToNonMainFeature()
    Pair(identifier.intern(), notMainFeature)
  }
  is FileCollectionDependency -> {
    // Note that this only gets the first file in the collection, ignoring the rest.
    when (files) {
      is ConfigurableFileCollection -> (files as? ConfigurableFileCollection)?.from?.let { from ->
        from.firstOrNull()?.toString()?.substringAfterLast("/")
      }?.let { Pair(it.intern(), false) }
      is ConfigurableFileTree -> files.firstOrNull()?.name?.let { Pair(it.intern(), false) }
      else -> null
    }
  }
  // Don't have enough information, so ignore it. Please note that a `FileCollectionDependency` is
  // also a `SelfResolvingDependency`, but not all `SelfResolvingDependency`s are
  // `FileCollectionDependency`s.
  is SelfResolvingDependency -> null
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}

internal fun Dependency.resolvedVersion(): String? = when (this) {
  is ProjectDependency -> null
  is ModuleDependency -> {
    // flat JAR/AAR files have no version, but rather than null, it's empty.
    version?.ifBlank { null }
  }
  is FileCollectionDependency -> null
  is SelfResolvingDependency -> null
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}?.intern()

private fun Dependency.isDependencyToJavaPlatform(): Boolean = when (this) {
  is ModuleDependency -> {
    val category = attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
    category?.name == Category.REGULAR_PLATFORM || category?.name == Category.ENFORCED_PLATFORM
  }
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}

/**
 * Return true if the dependency does not point at the "main" variant of the other module.
 * Other variants are for example testFixtures() or Feature Variants with different capabilities.
 *
 * See Gradle user manual:
 * - Capabilities: https://docs.gradle.org/current/userguide/component_capabilities.html
 * - Feature Variants: https://docs.gradle.org/current/userguide/feature_variants.html
 *   Feature Variants use Capabilities to distinguish different variants (source sets) of a project.
 * - Test Fixtures: https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures
 *   'testFixtures' is a Feature Variant added and configured by the 'java-test-fixtures' plugin.
 */
private fun Dependency.isDependencyToNonMainFeature(): Boolean = when (this) {
  is ModuleDependency -> {
    requestedCapabilities.isNotEmpty()
  }
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}
