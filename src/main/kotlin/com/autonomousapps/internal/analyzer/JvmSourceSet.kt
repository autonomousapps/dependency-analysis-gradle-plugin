package com.autonomousapps.internal.analyzer

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet as JbKotlinSourceSet

internal interface JvmSourceSet {
  val name: String
  val jarTaskName: String
  val sourceCode: SourceDirectorySet
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