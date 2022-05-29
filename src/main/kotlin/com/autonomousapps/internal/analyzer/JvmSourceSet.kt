package com.autonomousapps.internal.analyzer

import com.android.builder.model.SourceProvider
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal interface JvmSourceSet {
  val kind: SourceSetKind
  val name: String
  val jarTaskName: String
  val sourceCode: SourceDirectorySet

  /** E.g., `compileClasspath` or `testCompileClasspath` */
  val compileClasspathConfigurationName: String

  /** E.g., `runtimeClasspath` or `testRuntimeClasspath` */
  val runtimeClasspathConfigurationName: String

  val classesDirs: FileCollection
}

internal class JavaSourceSet(
  sourceSet: SourceSet,
  override val kind: SourceSetKind
) : JvmSourceSet {

  override val name: String = sourceSet.name
  override val jarTaskName: String = sourceSet.jarTaskName
  override val sourceCode: SourceDirectorySet = sourceSet.allJava // nb: this seems to work fine for Groovy
  override val compileClasspathConfigurationName: String = sourceSet.compileClasspathConfigurationName
  override val runtimeClasspathConfigurationName: String = sourceSet.runtimeClasspathConfigurationName

  override val classesDirs: FileCollection = sourceSet.output.classesDirs
}

internal class KotlinSourceSet(
  sourceSet: SourceSet,
  kotlinSourceSet: JbKotlinSourceSet,
  override val kind: SourceSetKind
) : JvmSourceSet {
  override val name: String = kotlinSourceSet.name
  override val jarTaskName: String = "jar"

  // TODO will this ignore Kotlin code in src/<foo>/java?
  override val sourceCode: SourceDirectorySet = kotlinSourceSet.kotlin

  override val compileClasspathConfigurationName: String =
    if (name != "main") "${name}CompileClasspath"
    else "compileClasspath"

  override val runtimeClasspathConfigurationName: String =
    if (name != "main") "${name}RuntimeClasspath"
    else "runtimeClasspath"

  override val classesDirs: FileCollection = sourceSet.output.classesDirs
}

/** All the relevant Java and Kotlin source sets for a given Android variant. */
internal class VariantSourceSet(
  val variant: Variant,
  val androidSourceSets: Set<SourceProvider> = emptySet(),
  /** E.g., `debugCompileClasspath` or `debugUnitTestCompileClasspath` */
  val compileClasspathConfigurationName: String,
  /** E.g., `debugRuntimeClasspath` or `debugUnitTestRuntimeClasspath` */
  val runtimeClasspathConfigurationName: String
)

internal fun SourceSet.java(): FileTree {
  return java.sourceDirectories.asFileTree.matching {
    include("**/*.java")
  }
}

internal fun JbKotlinSourceSet.kotlin(): FileTree {
  return kotlin.sourceDirectories.asFileTree.matching {
    include("**/*.kt")
  }
}

@Suppress("UnstableApiUsage") // GroovySourceDirectorySet
internal fun SourceSet.groovy(): FileTree? {
  return extensions.findByType<GroovySourceDirectorySet>()?.sourceDirectories?.asFileTree?.matching {
    include("**/*.groovy")
  }
}
