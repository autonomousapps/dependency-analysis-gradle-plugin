package com.autonomousapps.internal.analyzer

import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal interface JvmSourceSet {
  val name: String
  val jarTaskName: String
  val sourceCode: SourceDirectorySet

  fun asFiles(project: Project): FileCollection =
    project.layout.files(sourceCode.sourceDirectories)
}

internal class JavaSourceSet(sourceSet: SourceSet) : JvmSourceSet {
  override val name: String = sourceSet.name
  override val jarTaskName: String = sourceSet.jarTaskName
  override val sourceCode: SourceDirectorySet = sourceSet.allJava
}

internal class KotlinSourceSet(kotlinSourceSet: JbKotlinSourceSet) : JvmSourceSet {
  override val name: String = kotlinSourceSet.name
  override val jarTaskName: String = "jar"
  override val sourceCode: SourceDirectorySet = kotlinSourceSet.kotlin
}

/**
 * All the relevant Java and Kotlin source sets for a given Android variant.
 */
internal class VariantSourceSet(
  val androidSourceSets: Set<SourceProvider> = emptySet(),
  val kotlinSourceSets: Set<JbKotlinSourceSet>? = null
)
