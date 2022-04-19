package com.autonomousapps.internal.analyzer

import com.android.builder.model.SourceProvider
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal interface JvmSourceSet {
  val kind: SourceSetKind
  val name: String
  val jarTaskName: String
  val sourceCode: SourceDirectorySet

  /** E.g., `compileClasspath` or `testCompileClasspath` */
  val compileClasspathConfigurationName: String

  val javaCompileTaskName: String
  val kotlinCompileTaskName: String
}

internal class JavaSourceSet(
  sourceSet: SourceSet,
  override val kind: SourceSetKind
) : JvmSourceSet {
  override val name: String = sourceSet.name
  override val jarTaskName: String = sourceSet.jarTaskName
  override val sourceCode: SourceDirectorySet = sourceSet.allJava
  override val compileClasspathConfigurationName: String = sourceSet.compileClasspathConfigurationName

  override val javaCompileTaskName: String = sourceSet.compileJavaTaskName
  override val kotlinCompileTaskName: String =
    if (name != "main") "compile${name.capitalizeSafely()}Kotlin"
    else "compileKotlin"
}

internal class KotlinSourceSet(
  kotlinSourceSet: JbKotlinSourceSet,
  override val kind: SourceSetKind
) : JvmSourceSet {
  override val name: String = kotlinSourceSet.name
  override val jarTaskName: String = "jar"
  override val sourceCode: SourceDirectorySet = kotlinSourceSet.kotlin

  override val compileClasspathConfigurationName: String =
    if (name != "main") "${name}CompileClasspath"
    else "compileClasspath"

  override val javaCompileTaskName: String =
    if (name != "main") "compile${name.capitalizeSafely()}Java"
    else "compileJava"
  override val kotlinCompileTaskName: String =
    if (name != "main") "compile${name.capitalizeSafely()}Kotlin"
    else "compileKotlin"
}

/**
 * All the relevant Java and Kotlin source sets for a given Android variant.
 */
internal class VariantSourceSet(
  val variant: Variant,
  val androidSourceSets: Set<SourceProvider> = emptySet(),
  /** E.g., `debugCompileClasspath` or `debugUnitTestCompileClasspath` */
  val compileClasspathConfigurationName: String
)
