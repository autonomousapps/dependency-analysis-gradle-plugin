// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.artifacts.dependencyScopeConfiguration
import com.autonomousapps.artifacts.resolvableConfiguration
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency

/**
 * The classpath supplied to DAGP's classloader-isolated workers that read Kotlin metadata (`FindKotlinMagicTask`,
 * `AbiAnalysisTask`, `ExplodeJarTask`).
 *
 * `kotlin-metadata-jvm` is deliberately NOT bundled into DAGP's shaded jar: its classes carry a newer Kotlin
 * binary-metadata version (2.2.0+) that would break Kotlin DSL script compilation on the oldest supported Gradle (see
 * [issue 1671](https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1671)). Instead it is resolved
 * from the consuming build's repositories and added to a `classLoaderIsolation` worker classpath at execution time. The
 * isolated worker classloader already inherits DAGP's own classpath (work-action classes, relocated ASM, the bundled
 * `kotlin-stdlib`), so only `kotlin-metadata-jvm` itself needs to be added here.
 *
 * NOTE: consuming builds with locked-down repositories must make `org.jetbrains.kotlin:kotlin-metadata-jvm` resolvable.
 */
internal object KotlinMetadataClasspath {

  private const val DEPENDENCY_SCOPE = "dependencyAnalysisKotlinMetadata"
  private const val RESOLVABLE = "${DEPENDENCY_SCOPE}Classpath"

  /**
   * Registers the dependency-scope and resolvable configurations for this project. Must be called once at plugin-apply
   * time, before any task that calls [of] is registered.
   */
  fun register(project: Project) {
    // https://docs.gradle.org/9.7.0-rc-1/userguide/compatibility.html
    val kotlinMetadataVersion = if (GradleVersions.isAtLeastGradle970) {
      "2.4.0"
    } else if (GradleVersions.isAtLeastGradle940) {
      "2.3.0"
    } else if (GradleVersions.isAtLeastGradle811) {
      BuildConfig.KOTLIN_METADATA_VERSION
    } else {
      error("Unsupported Gradle version: '${GradleVersions.current}'. Expected at least '${GradleVersions.minGradleVersion}'.")
    }

    val scope = project.dependencyScopeConfiguration(DEPENDENCY_SCOPE)
    scope.configure { configuration ->
      configuration.defaultDependencies { deps ->
        val dep = project.dependencies.create(
          "org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinMetadataVersion"
        ) as ExternalModuleDependency
        // The stdlib is inherited from DAGP's own (capped) classpath; don't drag a newer one onto the worker classpath.
        dep.exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib"))
        deps.add(dep)
      }
    }
    project.resolvableConfiguration(RESOLVABLE, scope) { c ->
      c.description = "kotlin-metadata-jvm, supplied to classloader-isolated DAGP workers."
    }
  }

  /** Returns the resolvable configuration as a lazy provider suitable for a task input. */
  fun of(project: Project): NamedDomainObjectProvider<out Configuration> {
    return project.configurations.named(RESOLVABLE)
  }
}
