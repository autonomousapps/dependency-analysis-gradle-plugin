// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.filterToClassFiles
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

abstract class AndroidClassesTask : DefaultTask() {

  /**
   * Part of the AGP contract for accessing class files. Will be empty for this task.
   *
   * @see <a href="https://github.com/android/gradle-recipes/blob/agp-8.2/getScopedArtifacts/build-logic/plugins/src/main/kotlin/CustomPlugin.kt#L55">Scoped Artifacts</a>
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val jars: ListProperty<RegularFile>

  /**
   * Part of the AGP contract for accessing class files. May be empty.
   *
   * @see <a href="https://github.com/android/gradle-recipes/blob/agp-8.2/getScopedArtifacts/build-logic/plugins/src/main/kotlin/CustomPlugin.kt#L55">Scoped Artifacts</a>
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dirs: ListProperty<Directory>

  protected fun androidClassFiles(): List<File> {
    return dirs.getOrElse(emptyList()).flatMap { it.asFileTree.files }.filterToClassFiles()
  }
}
