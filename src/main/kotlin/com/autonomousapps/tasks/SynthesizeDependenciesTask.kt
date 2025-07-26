// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.Capability
import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.FlatDependency
import com.autonomousapps.model.internal.IncludedBuildDependency
import com.autonomousapps.model.internal.ModuleDependency
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.ProjectDependency
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.model.internal.intermediates.AndroidAssetDependency
import com.autonomousapps.model.internal.intermediates.AndroidManifestDependency
import com.autonomousapps.model.internal.intermediates.AndroidResDependency
import com.autonomousapps.model.internal.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.internal.intermediates.DependencyView
import com.autonomousapps.model.internal.intermediates.InlineMemberDependency
import com.autonomousapps.model.internal.intermediates.NativeLibDependency
import com.autonomousapps.model.internal.intermediates.ServiceLoaderDependency
import com.autonomousapps.model.internal.intermediates.TypealiasDependency
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
public abstract class SynthesizeDependenciesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    description = "Re-synthesize dependencies from analysis"
  }

  @get:Internal
  public abstract val inMemoryCache: Property<InMemoryCache>

  /** Needed to disambiguate other projects that might have otherwise identical inputs. */
  @get:Input
  public abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val compileDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val physicalArtifacts: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodedJars: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val inlineMembers: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val typealiases: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val serviceLoaders: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val annotationProcessors: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val nativeLibs: RegularFileProperty

  /*
   * Android-specific and therefore optional.
   */

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val manifestComponents: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidRes: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidAssets: RegularFileProperty

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(SynthesizeDependenciesWorkAction::class.java) {
      inMemoryCache.set(this@SynthesizeDependenciesTask.inMemoryCache)
      compileDependencies.set(this@SynthesizeDependenciesTask.compileDependencies)
      physicalArtifacts.set(this@SynthesizeDependenciesTask.physicalArtifacts)
      explodedJars.set(this@SynthesizeDependenciesTask.explodedJars)
      inlineMembers.set(this@SynthesizeDependenciesTask.inlineMembers)
      typealiases.set(this@SynthesizeDependenciesTask.typealiases)
      serviceLoaders.set(this@SynthesizeDependenciesTask.serviceLoaders)
      annotationProcessors.set(this@SynthesizeDependenciesTask.annotationProcessors)
      nativeLibs.set(this@SynthesizeDependenciesTask.nativeLibs)
      manifestComponents.set(this@SynthesizeDependenciesTask.manifestComponents)
      androidRes.set(this@SynthesizeDependenciesTask.androidRes)
      androidAssets.set(this@SynthesizeDependenciesTask.androidAssets)
      outputDir.set(this@SynthesizeDependenciesTask.outputDir)
    }
  }

  public interface SynthesizeDependenciesParameters : WorkParameters {
    public val inMemoryCache: Property<InMemoryCache>
    public val compileDependencies: RegularFileProperty
    public val physicalArtifacts: RegularFileProperty
    public val explodedJars: RegularFileProperty
    public val inlineMembers: RegularFileProperty
    public val typealiases: RegularFileProperty
    public val serviceLoaders: RegularFileProperty
    public val annotationProcessors: RegularFileProperty
    public val nativeLibs: RegularFileProperty

    // Android-specific and therefore optional
    public val manifestComponents: RegularFileProperty
    public val androidRes: RegularFileProperty
    public val androidAssets: RegularFileProperty

    public val outputDir: DirectoryProperty
  }

  public abstract class SynthesizeDependenciesWorkAction : WorkAction<SynthesizeDependenciesParameters> {

    private val builders = sortedMapOf<Coordinates, DependencyBuilder>()

    override fun execute() {
      val outputDir = parameters.outputDir

      val dependencies = parameters.compileDependencies.fromJson<CoordinatesContainer>().coordinates
      val physicalArtifacts = parameters.physicalArtifacts.fromJsonSet<PhysicalArtifact>()
      // val explodedJars = parameters.explodedJars.fromJsonSet<ExplodedJar>() // TODO(tsr): gzip
      val explodedJars = parameters.explodedJars.gzipDecompress<ExplodedJar>() // TODO(tsr): gzip
      val inlineMembers = parameters.inlineMembers.fromJsonSet<InlineMemberDependency>()
      val typealiases = parameters.typealiases.fromJsonSet<TypealiasDependency>()
      val serviceLoaders = parameters.serviceLoaders.fromJsonSet<ServiceLoaderDependency>()
      val annotationProcessors = parameters.annotationProcessors.fromJsonSet<AnnotationProcessorDependency>()
      val nativeLibs = parameters.nativeLibs.fromNullableJsonSet<NativeLibDependency>()
      // Android-specific and therefore optional
      val manifestComponents = parameters.manifestComponents.fromNullableJsonSet<AndroidManifestDependency>()
      val androidRes = parameters.androidRes.fromNullableJsonSet<AndroidResDependency>()
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
          it.coordinates == dependency
          // TODO(tsr): including the following in the check is too aggressive and can lead to failures in the
          //  ProjectVariant::dependencies function when looking for a file. This can happen, I think, when a dependency
          //  is specified as an external dependency but ends up getting resolved as a local project dependency instead.
          //  The simplest thing is to include the json file for this dep twice. Wastes some disk space (etc), but
          //  solves the problem. I doubt this is the best solution to the problem.
          // If the dependency is pointing at a project, there might already be an artifact
          // stored under matching IncludedBuildCoordinates.
          // || (it.coordinates is IncludedBuildCoordinates
          // && dependency.identifier == it.coordinates.resolvedProject.identifier
          // && dependency.gradleVariantIdentification.variantMatches(it.coordinates.resolvedProject))
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
      merge(nativeLibs)
      merge(manifestComponents)
      merge(androidRes)
      merge(androidAssets)

      // Write every dependency to its own file in the output directory
      builders.values.asSequence()
        .map { it.build() }
        .forEach { dependency ->
          val coordinates = dependency.coordinates
          outputDir.file(coordinates.toFileName()).get().asFile.bufferWriteJson(dependency)
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
    val files: MutableSet<File> = sortedSetOf()

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
      val capabilities: Map<String, Capability> = capabilities.associateBy { it.javaClass.canonicalName }.toSortedMap()
      return when (coordinates) {
        is ProjectCoordinates -> ProjectDependency(coordinates, capabilities, files)
        is ModuleCoordinates -> ModuleDependency(coordinates, capabilities, files)
        is FlatCoordinates -> FlatDependency(coordinates, capabilities, files)
        is IncludedBuildCoordinates -> IncludedBuildDependency(coordinates, capabilities, files)
      }
    }
  }
}
