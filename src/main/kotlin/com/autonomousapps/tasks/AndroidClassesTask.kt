// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.filterToClassFiles
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/**
 * Encodes the AGP contract for accessing artifacts from the current project, in this case class files.
 *
 * @see <a href="https://github.com/android/gradle-recipes/blob/agp-8.2/getScopedArtifacts/build-logic/plugins/src/main/kotlin/CustomPlugin.kt#L55">Scoped Artifacts</a>
 * @see [com.autonomousapps.internal.analyzer.AndroidSources]
 */
@CacheableTask
public abstract class AndroidClassesTask : DefaultTask() {

  /** Will be empty for this task. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val jars: ListProperty<RegularFile>

  /** May be empty. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dirs: ListProperty<Directory>

  /** Must be called during the execution phase. */
  protected fun androidClassFiles(): List<File> {
    return dirs.getOrElse(emptyList()).flatMap { it.asFileTree.files }.filterToClassFiles()
  }
}
