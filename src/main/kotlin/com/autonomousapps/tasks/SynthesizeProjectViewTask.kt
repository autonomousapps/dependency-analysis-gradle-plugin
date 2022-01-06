package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.UsagesExclusions
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.intermediates.ExplodingAbi
import com.autonomousapps.model.intermediates.ExplodingBytecode
import com.autonomousapps.model.intermediates.ExplodingSourceCode
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.TreeSet
import javax.inject.Inject

@CacheableTask
abstract class SynthesizeProjectViewTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Synthesizes project usages information into a single view"
  }

  @get:Input
  abstract val projectPath: Property<String>

  /** May be null. */
  @get:Optional
  @get:Input
  abstract val buildType: Property<String>

  /** May be null. */
  @get:Optional
  @get:Input
  abstract val flavor: Property<String>

  @get:Input
  abstract val variant: Property<String>

  @get:Input
  abstract val kind: Property<SourceSetKind>

  /** [`DependencyGraphView`][DependencyGraphView] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val graph: RegularFileProperty

  /** [`Set<AnnotationProcessorDependency>`][com.autonomousapps.model.intermediates.AnnotationProcessorDependency] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val annotationProcessors: RegularFileProperty

  /** [`Set<ExplodingByteCode>`][ExplodingBytecode] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val explodedBytecode: RegularFileProperty

  /** [`Set<ExplodingSourceCode>`][ExplodingSourceCode] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val explodedSourceCode: RegularFileProperty

  /** [`UsagesExclusions`][com.autonomousapps.internal.UsagesExclusions] */
  @get:Optional
  @get:Input
  abstract val usagesExclusions: Property<String>

  /** [`Set<ExplodingAbi>`][ExplodingAbi] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val explodingAbi: RegularFileProperty

  /** [`Set<AndroidResSource>`][AndroidResSource] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val androidResSource: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(SynthesizeProjectViewWorkAction::class.java) {
      projectPath.set(this@SynthesizeProjectViewTask.projectPath)
      buildType.set(this@SynthesizeProjectViewTask.buildType)
      flavor.set(this@SynthesizeProjectViewTask.flavor)
      variant.set(this@SynthesizeProjectViewTask.variant)
      kind.set(this@SynthesizeProjectViewTask.kind)
      graph.set(this@SynthesizeProjectViewTask.graph)
      annotationProcessors.set(this@SynthesizeProjectViewTask.annotationProcessors)
      explodedBytecode.set(this@SynthesizeProjectViewTask.explodedBytecode)
      explodedSourceCode.set(this@SynthesizeProjectViewTask.explodedSourceCode)
      explodingAbi.set(this@SynthesizeProjectViewTask.explodingAbi)
      usagesExclusions.set(this@SynthesizeProjectViewTask.usagesExclusions)
      androidResSource.set(this@SynthesizeProjectViewTask.androidResSource)
      output.set(this@SynthesizeProjectViewTask.output)
    }
  }

  interface SynthesizeProjectViewParameters : WorkParameters {
    val projectPath: Property<String>

    /** May be null. */
    val buildType: Property<String>

    /** May be null. */
    val flavor: Property<String>
    val variant: Property<String>
    val kind: Property<SourceSetKind>
    val graph: RegularFileProperty
    val annotationProcessors: RegularFileProperty
    val explodedBytecode: RegularFileProperty
    val explodedSourceCode: RegularFileProperty
    val usagesExclusions: Property<String>

    // Optional
    val explodingAbi: RegularFileProperty
    val androidResSource: RegularFileProperty

    val output: RegularFileProperty
  }

  abstract class SynthesizeProjectViewWorkAction : WorkAction<SynthesizeProjectViewParameters> {

    private val builders = sortedMapOf<String, CodeSourceBuilder>()

    @Suppress("UnstableApiUsage") // Guava Graph
    override fun execute() {
      val output = parameters.output.getAndDelete()

      val graph = parameters.graph.fromJson<DependencyGraphView>()
      val explodedBytecode = parameters.explodedBytecode.fromJsonSet<ExplodingBytecode>()
      val explodingAbi = parameters.explodingAbi.fromNullableJsonSet<ExplodingAbi>().orEmpty()
      val explodedSourceCode = parameters.explodedSourceCode.fromJsonSet<ExplodingSourceCode>()
      val androidResSource = parameters.androidResSource.fromNullableJsonSet<AndroidResSource>().orEmpty()

      explodedBytecode.forEach { bytecode ->
        builders.merge(
          bytecode.className,
          CodeSourceBuilder(bytecode.className).apply {
            relativePath = bytecode.relativePath
            usedClasses.addAll(bytecode.usedClasses)
          },
          CodeSourceBuilder::concat
        )
      }
      explodingAbi.forEach { abi ->
        builders.merge(
          abi.className,
          CodeSourceBuilder(abi.className).apply {
            exposedClasses.addAll(abi.exposedClasses)
          },
          CodeSourceBuilder::concat
        )
      }
      explodedSourceCode.forEach { source ->
        builders.merge(
          source.className,
          CodeSourceBuilder(source.className).apply {
            imports.addAll(source.imports)
            kind = source.kind
            relativePath = source.relativePath
          },
          CodeSourceBuilder::concat
        )
      }

      val codeSource = builders.values.asSequence()
        .map { it.build() }
        .toSet()

      val projectCoordinates = ProjectCoordinates(parameters.projectPath.get())
      val classpath = graph.graph.nodes().asSequence().filterNot {
        it == projectCoordinates
      }.toSortedSet()
      val annotationProcessors = parameters.annotationProcessors.fromJsonSet<AnnotationProcessorDependency>()
        .mapToSet { it.coordinates }

      val usagesExclusions = parameters.usagesExclusions.orNull?.fromJson<UsagesExclusions>() ?: UsagesExclusions.NONE
      val projectVariant = ProjectVariant(
        coordinates = projectCoordinates,
        buildType = parameters.buildType.orNull,
        flavor = parameters.flavor.orNull,
        variant = parameters.variant.get(),
        kind = parameters.kind.get(),
        sources = TreeSet<Source>().also { sources ->
          codeSource.mapTo(sources) { it.excludeUsages(usagesExclusions) }
          androidResSource.mapTo(sources) { it.excludeUsages(usagesExclusions) }
        },
        classpath = classpath,
        annotationProcessors = annotationProcessors
      )

      output.writeText(projectVariant.toJson())
    }

    private fun CodeSource.excludeUsages(usagesExclusions: UsagesExclusions): CodeSource {
      return copy(
        usedClasses = usagesExclusions.excludeClassesFromSet(usedClasses),
        imports = usagesExclusions.excludeClassesFromSet(imports),
      )
    }

    private fun AndroidResSource.excludeUsages(usagesExclusions: UsagesExclusions): AndroidResSource {
      return copy(
        usedClasses = usagesExclusions.excludeClassesFromSet(usedClasses),
      )
    }
  }
}

private class CodeSourceBuilder(val className: String) {

  var relativePath: String? = null
  var kind: CodeSource.Kind = CodeSource.Kind.UNKNOWN
  val usedClasses = mutableSetOf<String>()
  val exposedClasses = mutableSetOf<String>()
  val imports = mutableSetOf<String>()

  fun concat(other: CodeSourceBuilder): CodeSourceBuilder {
    usedClasses.addAll(other.usedClasses)
    exposedClasses.addAll(other.exposedClasses)
    imports.addAll(other.imports)
    other.relativePath?.let { relativePath = it }
    kind = other.kind
    return this
  }

  fun build(): CodeSource {
    val relativePath = checkNotNull(relativePath) { "'relativePath' was null for $className" }
    return CodeSource(
      relativePath = relativePath,
      kind = kind,
      className = className,
      usedClasses = usedClasses,
      exposedClasses = exposedClasses,
      imports = imports
    )
  }
}
