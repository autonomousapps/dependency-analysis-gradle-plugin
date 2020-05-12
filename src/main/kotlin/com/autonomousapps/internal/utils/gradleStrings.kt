package com.autonomousapps.internal.utils

import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

internal fun ComponentIdentifier.asString(): String {
  return when (this) {
    is ProjectComponentIdentifier -> projectPath.intern()
    is ModuleComponentIdentifier -> moduleIdentifier.toString().intern()
    else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
  }.intern()
}

internal fun ComponentIdentifier.resolvedVersion(): String? {
  return when (this) {
    is ProjectComponentIdentifier -> null
    is ModuleComponentIdentifier -> version
    else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}, named $this")
  }?.intern()
}

internal fun DependencySet.toIdentifiers(): Set<String> = mapNotNullToSet {
  when (it) {
    is ProjectDependency -> it.dependencyProject.path.intern()
    is ModuleDependency -> "${it.group}:${it.name}".intern()
    // Don't have enough information, so ignore it
    is SelfResolvingDependency -> null
    else -> throw GradleException("Unknown Dependency subtype: \n$it\n${it.javaClass.name}")
  }?.intern()
}
