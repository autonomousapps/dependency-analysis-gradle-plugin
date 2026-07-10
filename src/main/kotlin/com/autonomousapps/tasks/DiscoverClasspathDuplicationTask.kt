// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.source.AndroidSourceKind
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.TreeSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CacheableTask
public abstract class DiscoverClasspathDuplicationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  internal fun withClasspathName(name: String) {
    description = "Discovers duplicates on the $name classpath"
    classpathName.set(name)
  }

  private lateinit var classpath: ArtifactCollection

  public fun setClasspath(artifacts: ArtifactCollection) {
    this.classpath = artifacts
  }

  @Classpath
  public fun getClasspath(): FileCollection = classpath.artifactFiles

  @get:Input
  public abstract val classpathName: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val syntheticProject: RegularFileProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val project = syntheticProject.fromJson<ProjectVariant>(compressed = true)
    val duplicates = ClasspathAnalyzer(project, classpathName.get(), classpath).duplicates()

    output.writeText(duplicates.toJson())
  }

  internal class ClasspathAnalyzer(
    private val project: ProjectVariant,
    private val classpathName: String,
    artifacts: ArtifactCollection,
  ) {

    // map of class files to dependencies that contain them
    private val duplicatesMap = newSetMultimap<String, Coordinates>()

    init {
      artifacts
        .filterNonGradle()
        .filter { it.file.name.endsWith(".jar") }
        .forEach(::inspectJar)
    }

    fun duplicates(): Set<DuplicateClass> {
      val isAndroid = project.sourceKind is AndroidSourceKind
      return duplicatesMap.asMap()
        // Find all class files provided by more than one dependency
        .filterValues { it.size > 1 }
        // R classes are duplicated by design; ignore them on Android.
        .filterKeys { !isAndroid || !isAndroidRClass(it) }
        // only warn about duplicates if it's about a class that's actually used.
        .filterKeys {
          val fqcn = it.replace('/', '.').removeSuffix(".class")
          fqcn in project.usedClasses || fqcn in project.usedAnnotationClassesBySrc
        }
        // filter out "duplicates" where the GAV is identical. This can be an issue with, say, Kotlin, which adds
        // variants of the same dependency with different capabilities. Not user-actionable.
        // E.g., "org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.20" is the GAV for both of:
        // 1. ModuleCoordinates(identifier=org.jetbrains.kotlin:kotlin-gradle-plugin-api, resolvedVersion=2.1.20, gradleVariantIdentification=GradleVariantIdentification(capabilities=[org.jetbrains.kotlin:kotlin-gradle-plugin-api], attributes={}))
        // 2. ModuleCoordinates(identifier=org.jetbrains.kotlin:kotlin-gradle-plugin-api, resolvedVersion=2.1.20, gradleVariantIdentification=GradleVariantIdentification(capabilities=[org.jetbrains.kotlin:kotlin-gradle-plugin-api-gradle85], attributes={}))
        .filterValues { coordinates -> coordinates.mapToSet { it.gav() }.size > 1 }
        .mapTo(TreeSet()) { (classReference, dependency) ->
          DuplicateClass(
            sourceKind = project.sourceKind,
            classpathName = classpathName,
            // java/lang/String.class -> java/lang/String
            className = classReference.removeSuffix(".class"),
            dependencies = dependency.toSortedSet(),
          )
        }
    }

    private fun inspectJar(artifact: ResolvedArtifactResult) {
      ZipFile(artifact.file).use { zip ->

        val coordinates = artifact.toCoordinates()

        // Create multimap of class name to [dependencies]
        zip.asSequenceOfClassFiles()
          .map(ZipEntry::getName)
          .forEach { duplicatesMap.put(it, coordinates) }
      }
    }
  }
}

/**
 * Returns true if [classFile] (a `/`-delimited class file name, e.g. `androidx/appcompat/R$id.class`) is an Android
 * `R` class. Android generates `R` and nested `R$*` classes into every module, so they're never actionable duplicates.
 */
internal fun isAndroidRClass(classFile: String): Boolean {
  val simpleName = classFile.removeSuffix(".class").substringAfterLast('/')
  return simpleName == "R" || simpleName.startsWith("R$")
}
