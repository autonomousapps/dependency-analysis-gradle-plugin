// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.FileCollection

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

  private const val CONFIGURATION_NAME = "dependencyAnalysisKotlinMetadata"

  /** Returns the (idempotently created) resolvable configuration as a [FileCollection] suitable for a task input. */
  fun of(project: Project): FileCollection {
    project.configurations.findByName(CONFIGURATION_NAME)?.let { return it }

    return project.configurations.create(CONFIGURATION_NAME) { c ->
      c.isCanBeResolved = true
      c.isCanBeConsumed = false
      c.description = "kotlin-metadata-jvm, supplied to classloader-isolated DAGP workers."
      c.defaultDependencies { deps ->
        val dep = project.dependencies.create(
          "org.jetbrains.kotlin:kotlin-metadata-jvm:${BuildConfig.KOTLIN_METADATA_VERSION}"
        ) as ExternalModuleDependency
        // The stdlib is inherited from DAGP's own (capped) classpath; don't drag a newer one onto the worker classpath.
        dep.exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib"))
        deps.add(dep)
      }
    }
  }
}
