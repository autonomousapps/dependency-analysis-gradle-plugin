// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal interface JvmSourceSet {
  val sourceKind: SourceKind
  val name: String
  val jarTaskName: String
  val sourceCode: SourceDirectorySet

  /** E.g., `compileClasspath` or `testCompileClasspath` */
  val compileClasspathConfigurationName: String

  /** E.g., `runtimeClasspath` or `testRuntimeClasspath` */
  val runtimeClasspathConfigurationName: String

  val classesDirs: Provider<FileCollection>
}

internal class JavaSourceSet(
  project: Project,
  sourceSet: SourceSet,
  override val sourceKind: SourceKind,
) : JvmSourceSet {

  override val name: String = sourceSet.name
  override val jarTaskName: String = sourceSet.jarTaskName
  override val sourceCode: SourceDirectorySet = sourceSet.allJava // nb: this seems to work fine for Groovy
  override val compileClasspathConfigurationName: String = sourceSet.compileClasspathConfigurationName
  override val runtimeClasspathConfigurationName: String = sourceSet.runtimeClasspathConfigurationName

  override val classesDirs: Provider<FileCollection> = project.provider { sourceSet.output.classesDirs }
}

internal class KotlinSourceSet(
  project: Project,
  sourceSet: SourceSet,
  override val sourceKind: SourceKind,
) : JvmSourceSet {
  override val name: String = sourceSet.name
  override val jarTaskName: String = "jar"

  override val sourceCode: SourceDirectorySet = sourceSet.allSource

  override val compileClasspathConfigurationName: String =
    if (name != "main") "${name}CompileClasspath"
    else "compileClasspath"

  override val runtimeClasspathConfigurationName: String =
    if (name != "main") "${name}RuntimeClasspath"
    else "runtimeClasspath"

  override val classesDirs: Provider<FileCollection> = project.provider { sourceSet.output.classesDirs }
}

internal class KmpSourceSet(
  private val compilation: KotlinCompilation<*>,
) : JvmSourceSet {

  override val name: String = compilation.name
  override val jarTaskName: String = compilation.target.artifactsTaskName
  override val sourceKind: SourceKind = KmpSourceKind.of(compilation)
  override val sourceCode: SourceDirectorySet = compilation.defaultSourceSet.kotlin
  override val compileClasspathConfigurationName: String = compilation.compileDependencyConfigurationName
  override val runtimeClasspathConfigurationName: String = compilation.runtimeDependencyConfigurationName!!
  override val classesDirs: Provider<FileCollection> = compilation.project.provider { compilation.output.classesDirs }
}

internal fun SourceSet.java(): FileTree {
  return java.sourceDirectories.asFileTree.matching(Language.filterOf(Language.JAVA))
}

internal fun JbKotlinSourceSet.kotlin(): FileTree {
  return kotlin.sourceDirectories.asFileTree.matching(Language.filterOf(Language.KOTLIN))
}

internal fun SourceSet.groovy(): FileTree? {
  return extensions.findByType(GroovySourceDirectorySet::class.java)
    ?.sourceDirectories
    ?.asFileTree
    ?.matching(Language.filterOf(Language.GROOVY))
}
