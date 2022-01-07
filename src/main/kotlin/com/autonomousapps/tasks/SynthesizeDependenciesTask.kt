package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.toJson
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
  abstract val graphView: RegularFileProperty

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

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(SynthesizeDependenciesWorkAction::class.java) {
      inMemoryCache.set(this@SynthesizeDependenciesTask.inMemoryCache)
      graphView.set(this@SynthesizeDependenciesTask.graphView)
      physicalArtifacts.set(this@SynthesizeDependenciesTask.physicalArtifacts)
      explodedJars.set(this@SynthesizeDependenciesTask.explodedJars)
      inlineMembers.set(this@SynthesizeDependenciesTask.inlineMembers)
      serviceLoaders.set(this@SynthesizeDependenciesTask.serviceLoaders)
      annotationProcessors.set(this@SynthesizeDependenciesTask.annotationProcessors)
      manifestComponents.set(this@SynthesizeDependenciesTask.manifestComponents)
      androidRes.set(this@SynthesizeDependenciesTask.androidRes)
      nativeLibs.set(this@SynthesizeDependenciesTask.nativeLibs)
      outputDir.set(this@SynthesizeDependenciesTask.outputDir)
    }
  }
}

interface SynthesizeDependenciesParameters : WorkParameters {
  val inMemoryCache: Property<InMemoryCache>
  val graphView: RegularFileProperty
  val physicalArtifacts: RegularFileProperty
  val explodedJars: RegularFileProperty
  val inlineMembers: RegularFileProperty
  val serviceLoaders: RegularFileProperty
  val annotationProcessors: RegularFileProperty

  // Android-specific and therefore optional
  val manifestComponents: RegularFileProperty
  val androidRes: RegularFileProperty
  val nativeLibs: RegularFileProperty

  val outputDir: DirectoryProperty
}

abstract class SynthesizeDependenciesWorkAction : WorkAction<SynthesizeDependenciesParameters> {

  private val builders = sortedMapOf<Coordinates, DependencyBuilder>()

  override fun execute() {
    val outputDir = parameters.outputDir

    val graphView = parameters.graphView.fromJson<DependencyGraphView>()
    val physicalArtifacts = parameters.physicalArtifacts.fromJsonSet<PhysicalArtifact>()
    val explodedJars = parameters.explodedJars.fromJsonSet<ExplodedJar>()
    val inlineMembers = parameters.inlineMembers.fromJsonSet<InlineMemberDependency>()
    val serviceLoaders = parameters.serviceLoaders.fromJsonSet<ServiceLoaderDependency>()
    val annotationProcessors = parameters.annotationProcessors.fromJsonSet<AnnotationProcessorDependency>()
    // Android-specific and therefore optional
    val manifestComponents = parameters.manifestComponents.fromNullableJsonSet<AndroidManifestDependency>().orEmpty()
    val androidRes = parameters.androidRes.fromNullableJsonSet<AndroidResDependency>().orEmpty()
    val nativeLibs = parameters.nativeLibs.fromNullableJsonSet<NativeLibDependency>().orEmpty()

    physicalArtifacts.forEach { artifact ->
      builders.merge(
        artifact.coordinates,
        DependencyBuilder(artifact.coordinates).apply { file = artifact.file },
        DependencyBuilder::concat
      )
    }

    // A dependency can appear in the graph even though it's just a .pom (.module) file. E.g., kotlinx-coroutines-core.
    // This is a fallback so all such dependencies have a file written to disk.
    graphView.nodes.forEach { node ->
      builders.merge(
        node,
        DependencyBuilder(node),
        DependencyBuilder::concat
      )
    }

    merge(explodedJars)
    merge(inlineMembers)
    merge(serviceLoaders)
    merge(annotationProcessors)
    merge(manifestComponents)
    merge(androidRes)
    merge(nativeLibs)

    // Write every dependency to its own file in the output directory
    builders.values.asSequence()
      .map { it.build() }
      .forEach { dependency ->
        outputDir.file(dependency.coordinates.toFileName()).get().asFile.writeText(dependency.toJson())
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
  var file: File? = null

  fun concat(other: DependencyBuilder): DependencyBuilder {
    other.file?.let { file = it }
    capabilities.addAll(other.capabilities)
    return this
  }

  fun build(): Dependency {
    val capabilities: Map<String, Capability> = capabilities.associateBy { it.javaClass.canonicalName }
    return when (coordinates) {
      is ProjectCoordinates -> ProjectDependency(coordinates, capabilities, file)
      is ModuleCoordinates -> ModuleDependency(coordinates, capabilities, file)
      is FlatCoordinates -> FlatDependency(coordinates, capabilities, file)
    }
  }
}
