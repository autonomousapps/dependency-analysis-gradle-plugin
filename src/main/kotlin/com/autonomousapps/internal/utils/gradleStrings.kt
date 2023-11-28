package com.autonomousapps.internal.utils

import com.autonomousapps.internal.GradleVersions
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
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.capabilities.Capability
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier

/** Converts this [ResolvedDependencyResult] to group-artifact-version (GAV) coordinates in a tuple of (GA, V?). */
internal fun ResolvedDependencyResult.toCoordinates(): Coordinates =
  compositeRequest() ?: selected.id.wrapInIncludedBuildCoordinates(resolvedVariant)

/** If this is a composite substitution, returns it as such. We care about the request as well as the result. */
private fun ResolvedDependencyResult.compositeRequest(): IncludedBuildCoordinates? {
  val gradleVariantIdentification = resolvedVariant.toGradleVariantIdentification()
  if (!selected.selectionReason.isCompositeSubstitution) return null
  val requestedModule = requested as? ModuleComponentSelector ?: return null

  val requested = ModuleCoordinates(
    identifier = requestedModule.moduleIdentifier.toString(),
    resolvedVersion = requestedModule.version,
    gradleVariantIdentification = gradleVariantIdentification
  )
  val resolved = ProjectCoordinates(
    identifier = (selected.id as ProjectComponentIdentifier).identityPath(),
    gradleVariantIdentification = gradleVariantIdentification,
    buildPath = (selected.id as ProjectComponentIdentifier).build.let {
      if (GradleVersions.isAtLeastGradle82) it.buildPath else @Suppress("DEPRECATION") it.name
    }
  )

  return IncludedBuildCoordinates.of(requested, resolved)
}

private fun ProjectComponentIdentifier.identityPath(): String {
  return (this as? DefaultProjectComponentIdentifier)?.identityPath?.toString()
    ?: error("${toCoordinates(GradleVariantIdentification.EMPTY)} is not a DefaultProjectComponentIdentifier")
}

internal fun ResolvedArtifactResult.toCoordinates(): Coordinates {
  return id.componentIdentifier.wrapInIncludedBuildCoordinates(variant)
}

private fun ComponentIdentifier.wrapInIncludedBuildCoordinates(variant: ResolvedVariantResult?): Coordinates {
  val variantIdentification = variant.toGradleVariantIdentification()
  val resolved = toCoordinates(variantIdentification)

  // No resolved variant, so there are no capabilities to extract the components coordinates from
  if (variant == null) return resolved

  // Doesn't resolve to a project, so can't be an included build. Return as-is.
  if (resolved !is ProjectCoordinates) return resolved

  // Module may have been resolved from an included build. Construct IncludedBuildCoordinates if possible.
  // This is a very naive heuristic. Doesn't work for Gradle < 7.2, where capabilities is empty.
  val projectName = (variant.owner as ProjectComponentIdentifier).projectName
  val requested = variant.capabilities.find { it.name.startsWith(projectName) }?.let { c ->
    c.version?.let { v ->
      ModuleCoordinates(
        identifier = "${c.group}:$projectName",
        resolvedVersion = v,
        gradleVariantIdentification = variantIdentification
      )
    }
  } ?: return resolved

  return IncludedBuildCoordinates.of(
    requested = requested,
    resolvedProject = resolved
  )
}

/** Returns the [coordinates][Coordinates] of the root of [this][Configuration]. */
internal fun Configuration.rootCoordinates(): Coordinates = incoming.resolutionResult.root.rootCoordinates()

/** Returns the [coordinates][Coordinates] of the root of [this][ResolvedComponentResult]. */
internal fun ResolvedComponentResult.rootCoordinates(): Coordinates {
  return id
    // For the root, the 'GradleVariantIdentification' is always empty as there is only one root (which we match later)
    .toCoordinates(GradleVariantIdentification(setOf("ROOT"), emptyMap()))
}

/** Converts this [ComponentIdentifier] to group-artifact-version (GAV) coordinates in a tuple of (GA, V?). */
private fun ComponentIdentifier.toCoordinates(gradleVariantIdentification: GradleVariantIdentification): Coordinates {
  val identifier = toIdentifier()
  return when (this) {
    is ProjectComponentIdentifier -> {
      val buildPath = if (GradleVersions.isAtLeastGradle82) build.buildPath else @Suppress("DEPRECATION") build.name
      ProjectCoordinates(identifier, gradleVariantIdentification, buildPath)
    }

    is ModuleComponentIdentifier -> {
      resolvedVersion()?.let { resolvedVersion ->
        ModuleCoordinates(identifier, resolvedVersion, gradleVariantIdentification)
      } ?: FlatCoordinates(identifier)
    }

    else -> FlatCoordinates(identifier)
  }
}

/**
 * Convert this [ComponentIdentifier] to a group-artifact identifier, such as "org.jetbrains.kotlin:kotlin-stdlib" in
 * the case of an external module, or a project identifier, such as ":foo:bar", in the case of an internal module.
 */
private fun ComponentIdentifier.toIdentifier(): String = when (this) {
  is ProjectComponentIdentifier -> projectPath
  is ModuleComponentIdentifier -> {
    // flat JAR/AAR files have no group. I don't trust that, if absent, it will be blank rather
    // than null.
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
 */
private fun ComponentIdentifier.resolvedVersion(): String? = when (this) {
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
internal fun DependencySet.toIdentifiers(): Set<Pair<String, GradleVariantIdentification>> = mapNotNullToSet {
  it.toIdentifier()
}

internal fun Dependency.toCoordinates(): Coordinates? {
  val identifier = toIdentifier() ?: return null
  return when (this) {
    is ProjectDependency -> ProjectCoordinates(identifier.first, identifier.second)
    is ModuleDependency -> {
      resolvedVersion()?.let { resolvedVersion ->
        ModuleCoordinates(identifier.first, resolvedVersion, identifier.second)
      } ?: FlatCoordinates(identifier.first)
    }

    else -> FlatCoordinates(identifier.first)
  }
}

/**
 * Given a [Dependency] retrieved from a [Configuration], return it as a
 * pair of 'identifier' and 'GradleVariantIdentification'
 */
internal fun Dependency.toIdentifier(): Pair<String, GradleVariantIdentification>? = when (this) {
  is ProjectDependency -> {
    val identifier = dependencyProject.path
    Pair(identifier.intern(), targetGradleVariantIdentification())
  }

  is ModuleDependency -> {
    // flat JAR/AAR files have no group.
    val identifier = if (group != null) "${group}:${name}" else name
    Pair(identifier.intern(), targetGradleVariantIdentification())
  }

  is FileCollectionDependency -> {
    // Note that this only gets the first file in the collection, ignoring the rest.
    when (files) {
      is ConfigurableFileCollection -> {
        (files as ConfigurableFileCollection).from.firstOrNull()
          ?.let { first ->
            // https://github.com/gradle/gradle/pull/26317
            val firstFile = if (first is Array<*>) {
              first.firstOrNull()
            } else {
              first
            }

            // Handle weirdness that seems to come from KGP? Unclear. See comments from
            // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/997#issuecomment-1826627186
            when (firstFile) {
              is Function0<*> -> null // "() -> Any?"
              is Provider<*> -> null  // "property 'destinationDirectory'"
              else -> firstFile?.toString()?.substringAfterLast('/')
            }
          }?.let {
            Pair(it.intern(), GradleVariantIdentification.EMPTY)
          }
      }

      is ConfigurableFileTree -> files.firstOrNull()?.name?.let {
        Pair(it.intern(), GradleVariantIdentification.EMPTY)
      }

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

/**
 * Returns the 'capabilities' and 'attributes' that are used by Gradle to determine which variant in the other
 * module this dependency points at. Next to the 'main' variant, this can be for example, testFixtures() or
 * another Feature Variant. This function reads the required capabilities and attributes DIRECTLY declared on
 * a dependency (testFixtures(...) and platform(...) are shortcut notations that add a capability/attribute).
 *
 * See Gradle user manual:
 * - Capabilities: https://docs.gradle.org/current/userguide/component_capabilities.html
 * - Feature Variants: https://docs.gradle.org/current/userguide/feature_variants.html
 *   Feature Variants use Capabilities to distinguish different variants (source sets) of a project.
 * - Test Fixtures: https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures
 *   'testFixtures' is a Feature Variant added and configured by the 'java-test-fixtures' plugin.
 */
internal fun Dependency.targetGradleVariantIdentification() = when (this) {
  is ModuleDependency -> GradleVariantIdentification(
    requestedCapabilities.map { it.toGA() }.toSet(),
    attributes.keySet().associate { it.name to attributes.getAttribute(it).toString() }
  )

  else -> GradleVariantIdentification.EMPTY
}

/**
 * Return the 'capabilities' of a selected variant. Will be used to determine if a declaration
 * will (most likely) result in resolving to a certain variant of a component.
 * Attributes of the resolved variant are not taken into account. Attributes are used for exclusive
 * selection. Which means there can only be one variant in the graph and not multiple variants with different
 * attributes (and the same capability). Hence, we do not need to distinguish variants based on attributes (only by
 * capabilities).
 */
internal fun ResolvedVariantResult?.toGradleVariantIdentification(): GradleVariantIdentification {
  if (this == null) return GradleVariantIdentification.EMPTY

  return GradleVariantIdentification(
    capabilities.map { it.toGA() }.toSet(), emptyMap()
  )
}

private fun Capability.toGA() = "$group:$name".intern()
