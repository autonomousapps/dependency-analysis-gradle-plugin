// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.intermediates.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class SynthesizeDependenciesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Re-synthesize dependencies from analysis"
  }

  @get:Internal
  abstract val inMemoryCache: Property<InMemoryCache>

  /** Needed to disambiguate other projects that might have otherwise identical inputs. */
  @get:Input
  abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val compileDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val physicalArtifacts: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val explodedJars: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val inlineMembers: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val typealiases: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val serviceLoaders: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val annotationProcessors: RegularFileProperty

  /*
   * Android-specific and therefore optional.
   */

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val manifestComponents: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val androidRes: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val nativeLibs: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val androidAssets: RegularFileProperty

  // TODO: Maybe this should not be an OutputDirectory anymore, but just Internal? Since multiple tasks will write to it
  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  /** A simplified representation of [outputDir] for task snapshotting purposes only. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(SynthesizeDependenciesWorkAction::class.java) {
      inMemoryCache.set(this@SynthesizeDependenciesTask.inMemoryCache)
      compileDependencies.set(this@SynthesizeDependenciesTask.compileDependencies)
      physicalArtifacts.set(this@SynthesizeDependenciesTask.physicalArtifacts)
      explodedJars.set(this@SynthesizeDependenciesTask.explodedJars)
      inlineMembers.set(this@SynthesizeDependenciesTask.inlineMembers)
      typealiases.set(this@SynthesizeDependenciesTask.typealiases)
      serviceLoaders.set(this@SynthesizeDependenciesTask.serviceLoaders)
      annotationProcessors.set(this@SynthesizeDependenciesTask.annotationProcessors)
      manifestComponents.set(this@SynthesizeDependenciesTask.manifestComponents)
      androidRes.set(this@SynthesizeDependenciesTask.androidRes)
      nativeLibs.set(this@SynthesizeDependenciesTask.nativeLibs)
      androidAssets.set(this@SynthesizeDependenciesTask.androidAssets)
      outputDir.set(this@SynthesizeDependenciesTask.outputDir)
      output.set(this@SynthesizeDependenciesTask.output)
    }
  }

  interface SynthesizeDependenciesParameters : WorkParameters {
    val inMemoryCache: Property<InMemoryCache>
    val compileDependencies: RegularFileProperty
    val physicalArtifacts: RegularFileProperty
    val explodedJars: RegularFileProperty
    val inlineMembers: RegularFileProperty
    val typealiases: RegularFileProperty
    val serviceLoaders: RegularFileProperty
    val annotationProcessors: RegularFileProperty

    // Android-specific and therefore optional
    val manifestComponents: RegularFileProperty
    val androidRes: RegularFileProperty
    val nativeLibs: RegularFileProperty
    val androidAssets: RegularFileProperty

    val outputDir: DirectoryProperty
    val output: RegularFileProperty
  }

  abstract class SynthesizeDependenciesWorkAction : WorkAction<SynthesizeDependenciesParameters> {

    private val builders = sortedMapOf<Coordinates, DependencyBuilder>()

    override fun execute() {
      val outputDir = parameters.outputDir
      val output = parameters.output.getAndDelete()

      val dependencies = parameters.compileDependencies.fromJson<CoordinatesContainer>().coordinates
      val physicalArtifacts = parameters.physicalArtifacts.fromJsonSet<PhysicalArtifact>()
      val explodedJars = parameters.explodedJars.fromJsonSet<ExplodedJar>()
      val inlineMembers = parameters.inlineMembers.fromJsonSet<InlineMemberDependency>()
      val typealiases = parameters.typealiases.fromJsonSet<TypealiasDependency>()
      val serviceLoaders = parameters.serviceLoaders.fromJsonSet<ServiceLoaderDependency>()
      val annotationProcessors = parameters.annotationProcessors.fromJsonSet<AnnotationProcessorDependency>()
      // Android-specific and therefore optional
      val manifestComponents = parameters.manifestComponents.fromNullableJsonSet<AndroidManifestDependency>()
      val androidRes = parameters.androidRes.fromNullableJsonSet<AndroidResDependency>()
      val nativeLibs = parameters.nativeLibs.fromNullableJsonSet<NativeLibDependency>()
      val androidAssets = parameters.androidAssets.fromNullableJsonSet<AndroidAssetDependency>()

      physicalArtifacts.forEach { artifact ->
        builders.merge(
          artifact.coordinates,
          DependencyBuilder(artifact.coordinates).apply { files.add(artifact.file) },
          DependencyBuilder::concat
        )
      }

      // A dependency can appear in the graph even though it's just a .pom (.module) file. E.g., kotlinx-coroutines-core.
      // This is a fallback so all such dependencies have a file written to disk.
      dependencies.forEach { dependency ->
        // Do not add dependencies that are already known again
        val coordinatesAlreadyKnown = builders.values.any {
          it.coordinates == dependency || (
            // If the dependency is pointing at a project, there might already be an artifact
            // stored under matching IncludedBuildCoordinates.
            it.coordinates is IncludedBuildCoordinates
              && dependency.identifier == it.coordinates.resolvedProject.identifier
              && dependency.gradleVariantIdentification.variantMatches(it.coordinates.resolvedProject)
            )
        }
        if (!coordinatesAlreadyKnown) {
          builders.merge(
            dependency,
            DependencyBuilder(dependency),
            DependencyBuilder::concat
          )
        }
      }

      merge(explodedJars)
      merge(inlineMembers)
      merge(typealiases)
      merge(serviceLoaders)
      merge(annotationProcessors)
      merge(manifestComponents)
      merge(androidRes)
      merge(nativeLibs)
      merge(androidAssets)

      // Write every dependency to its own file in the output directory
      builders.values.asSequence()
        .map { it.build() }
        .forEach { dependency ->
          val coordinates = dependency.coordinates
          val file = outputDir.file(coordinates.toFileName()).get().asFile
          if (!file.exists()) {
            file.bufferWriteJson(dependency)
          }

          // This is the task output for snapshotting purposes
          output.appendText("${coordinates.gav()}\n")
        }
    }

    private fun <T : DependencyView<T>> merge(dependencies: Set<T>) {
      dependencies.forEach {
        builders.merge(
          it.coordinates,
          DependencyBuilder(it.coordinates).apply { capabilities.addAll(it.toCapabilities()) },
          DependencyBuilder::concat
        )
      }
    }
  }

  private class DependencyBuilder(val coordinates: Coordinates) {

    val capabilities: MutableList<Capability> = mutableListOf()
    val files: MutableList<File> = mutableListOf()

    fun concat(other: DependencyBuilder): DependencyBuilder {
      files.addAll(other.files)
      other.capabilities.forEach { otherCapability ->
        val existing = capabilities.find { it.javaClass.canonicalName == otherCapability.javaClass.canonicalName }
        if (existing != null) {
          val merged = existing.merge(otherCapability)
          capabilities.remove(existing)
          capabilities.add(merged)
        } else {
          capabilities.add(otherCapability)
        }
      }
      return this
    }

    fun build(): Dependency {
      val capabilities: Map<String, Capability> = capabilities.associateBy { it.javaClass.canonicalName }
      return when (coordinates) {
        is ProjectCoordinates -> ProjectDependency(coordinates, capabilities, files)
        is ModuleCoordinates -> ModuleDependency(coordinates, capabilities, files)
        is FlatCoordinates -> FlatDependency(coordinates, capabilities, files)
        is IncludedBuildCoordinates -> IncludedBuildDependency(coordinates, capabilities, files)
      }
    }
  }
}
