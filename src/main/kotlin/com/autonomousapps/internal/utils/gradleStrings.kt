package com.autonomousapps.internal.utils

import org.gradle.api.GradleException
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier

/**
 * Convert this [ComponentIdentifier] to a group-artifact identifier, such as "org.jetbrains.kotlin:kotlin-stdlib" in
 * the case of an external module, or a project identifier, such as ":foo:bar", in the case of an internal module.
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
internal fun DependencySet.toIdentifiers(
  metadataSink: MutableMap<String, Boolean> = mutableMapOf()
): Set<String> = mapNotNullToSet {
  it.toIdentifier(metadataSink)
}

/**
 * Given a [Dependency] retrieved from a [Configuration], return it as an identifier, per
 * [ComponentIdentifier.toIdentifier].
 */
internal fun Dependency.toIdentifier(
  metadataSink: MutableMap<String, Boolean> = mutableMapOf()
): String? = when (this) {
  is ProjectDependency -> {
    val identifier = dependencyProject.path
    if (isJavaPlatform()) metadataSink[identifier] = true
    identifier
  }
  is ModuleDependency -> {
    // flat JAR/AAR files have no group.
    val identifier = if (group != null) "${group}:${name}" else name
    if (isJavaPlatform()) metadataSink[identifier] = true
    identifier
  }
  is FileCollectionDependency -> {
    // Note that this only gets the first file in the collection, ignoring the rest.
    when (files) {
      is ConfigurableFileCollection -> (files as? ConfigurableFileCollection)?.from?.let { from ->
        from.firstOrNull()?.toString()?.substringAfterLast("/")
      }
      is ConfigurableFileTree -> files.firstOrNull()?.name
      else -> null
    }
  }
  // Don't have enough information, so ignore it. Please note that a `FileCollectionDependency` is
  // also a `SelfResolvingDependency`, but not all `SelfResolvingDependency`s are
  // `FileCollectionDependency`s.
  is SelfResolvingDependency -> null
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}?.intern()

private fun Dependency.isJavaPlatform(): Boolean = when (this) {
  is ProjectDependency -> dependencyProject.pluginManager.hasPlugin("java-platform")
  is ModuleDependency -> {
    val category = attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
    category?.name == Category.REGULAR_PLATFORM || category?.name == Category.ENFORCED_PLATFORM
  }
  else -> throw GradleException("Unknown Dependency subtype: \n$this\n${javaClass.name}")
}
