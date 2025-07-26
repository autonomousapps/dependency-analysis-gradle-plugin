// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.UsagesExclusions
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.*
import com.autonomousapps.model.internal.intermediates.AnnotationProcessorDependency
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingAbi
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingBytecode
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingSourceCode
import com.autonomousapps.model.source.SourceKind
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
public abstract class SynthesizeProjectViewTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Synthesizes project usages information into a single view"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  /** May be null. */
  @get:Optional
  @get:Input
  public abstract val buildType: Property<String>

  /** May be null. */
  @get:Optional
  @get:Input
  public abstract val flavor: Property<String>

  @get:Input
  public abstract val variant: Property<String>

  @get:Input
  public abstract val sourceKind: Property<SourceKind>

  /** [`DependencyGraphView`][DependencyGraphView] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val graph: RegularFileProperty

  /** [`Set<AnnotationProcessorDependency>`][com.autonomousapps.model.internal.intermediates.AnnotationProcessorDependency] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val annotationProcessors: RegularFileProperty

  /** [`Set<ExplodingByteCode>`][ExplodingBytecode] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodedBytecode: RegularFileProperty

  /** [`Set<ExplodingSourceCode>`][ExplodingSourceCode] */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodedSourceCode: RegularFileProperty

  /** [`UsagesExclusions`][com.autonomousapps.internal.UsagesExclusions] */
  @get:Optional
  @get:Input
  public abstract val usagesExclusions: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val excludedIdentifiers: RegularFileProperty

  /** [`Set<ExplodingAbi>`][ExplodingAbi] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodingAbi: RegularFileProperty

  /** [`Set<AndroidResSource>`][AndroidResSource] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidResSource: RegularFileProperty

  /** [`Set<AndroidResSource>`][AndroidResSource] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidResSourceRuntime: RegularFileProperty

  /** [`Set<AndroidAssetSource>`][AndroidAssetSource] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidAssetsSource: RegularFileProperty

  /**
   * A string representing the fully-qualified class name (FQCN) of the test instrumentation runner if (1) this is an
   * Android project and (2) a test instrumentation runner is declared. (May be null.)
   */
  @get:Optional
  @get:Input
  public abstract val testInstrumentationRunner: Property<String>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(SynthesizeProjectViewWorkAction::class.java) {
      projectPath.set(this@SynthesizeProjectViewTask.projectPath)
      buildType.set(this@SynthesizeProjectViewTask.buildType)
      flavor.set(this@SynthesizeProjectViewTask.flavor)
      sourceKind.set(this@SynthesizeProjectViewTask.sourceKind)
      graph.set(this@SynthesizeProjectViewTask.graph)
      annotationProcessors.set(this@SynthesizeProjectViewTask.annotationProcessors)
      explodedBytecode.set(this@SynthesizeProjectViewTask.explodedBytecode)
      explodedSourceCode.set(this@SynthesizeProjectViewTask.explodedSourceCode)
      explodingAbi.set(this@SynthesizeProjectViewTask.explodingAbi)
      excludedIdentifiers.set(this@SynthesizeProjectViewTask.excludedIdentifiers)
      usagesExclusions.set(this@SynthesizeProjectViewTask.usagesExclusions)
      androidResSource.set(this@SynthesizeProjectViewTask.androidResSource)
      androidResSourceRuntime.set(this@SynthesizeProjectViewTask.androidResSourceRuntime)
      androidAssetsSource.set(this@SynthesizeProjectViewTask.androidAssetsSource)
      testInstrumentationRunner.set(this@SynthesizeProjectViewTask.testInstrumentationRunner)
      output.set(this@SynthesizeProjectViewTask.output)
    }
  }

  public interface SynthesizeProjectViewParameters : WorkParameters {
    public val projectPath: Property<String>

    /** May be null. */
    public val buildType: Property<String>

    /** May be null. */
    public val flavor: Property<String>
    public val sourceKind: Property<SourceKind>
    public val graph: RegularFileProperty
    public val annotationProcessors: RegularFileProperty
    public val explodedBytecode: RegularFileProperty
    public val explodedSourceCode: RegularFileProperty
    public val excludedIdentifiers: RegularFileProperty
    public val usagesExclusions: Property<String>

    // Optional
    public val explodingAbi: RegularFileProperty
    public val androidResSource: RegularFileProperty
    public val androidResSourceRuntime: RegularFileProperty
    public val androidAssetsSource: RegularFileProperty
    public val testInstrumentationRunner: Property<String>

    public val output: RegularFileProperty
  }

  public abstract class SynthesizeProjectViewWorkAction : WorkAction<SynthesizeProjectViewParameters> {

    private val builders = sortedMapOf<String, CodeSourceBuilder>()

    @Suppress("UnstableApiUsage") // Guava Graph
    override fun execute() {
      val output = parameters.output.getAndDelete()

      val graph = parameters.graph.fromJson<DependencyGraphView>()
      val explodedBytecode = parameters.explodedBytecode.fromJsonSet<ExplodingBytecode>()
      val explodingAbi = parameters.explodingAbi.fromNullableJsonSet<ExplodingAbi>()
      val explodedSourceCode = parameters.explodedSourceCode.fromJsonSet<ExplodingSourceCode>()
      val androidResSource = parameters.androidResSource.fromNullableJsonSet<AndroidResSource>()
      val androidResSourceRuntime = parameters.androidResSourceRuntime.fromNullableJsonSet<AndroidResSource>()
      val androidAssetsSource = parameters.androidAssetsSource.fromNullableJsonSet<AndroidAssetSource>()
      val testInstrumentationRunner = parameters.testInstrumentationRunner.orNull
      val excludedIdentifiers = parameters.excludedIdentifiers.fromJsonSet<ExcludedIdentifier>()

      explodedBytecode.forEach { bytecode ->
        builders.merge(
          bytecode.className,
          CodeSourceBuilder(className = bytecode.className).apply {
            relativePath = bytecode.relativePath
            superClass = bytecode.superClass
            interfaces.addAll(bytecode.interfaces)
            nonAnnotationClasses.addAll(bytecode.nonAnnotationClasses)
            annotationClasses.addAll(bytecode.annotationClasses)
            invisibleAnnotationClasses.addAll(bytecode.invisibleAnnotationClasses)
            //   // TODO(tsr): flatten into a single set? Do we need the map?
            //   // Merge the two maps
            //   bytecode.binaryClassAccesses.forEach { (className, memberAccesses) ->
            //     binaryClassAccesses.merge(className, memberAccesses.toMutableSet()) { acc, inc ->
            //       acc.apply { addAll(inc) }
            //     }
            //   }
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
        // relativePath will be null for synthetic classes, like R class files
        .filterNot { it.relativePath == null }
        .map { it.build() }
        .toSet()

      val projectCoordinates = ProjectCoordinates(
        parameters.projectPath.get(),
        GradleVariantIdentification.EMPTY
      )
      val ignoreSelfDependencies = parameters.buildType.isPresent // ignore on Android
      val classpath = graph.graph.nodes().asSequence().filterNot {
        ignoreSelfDependencies && it is IncludedBuildCoordinates && it.resolvedProject.identifier == projectCoordinates.identifier
      }.toSortedSet()
      val annotationProcessors = parameters.annotationProcessors.fromJsonSet<AnnotationProcessorDependency>()
        .mapToSet { it.coordinates }
      val usagesExclusions = parameters.usagesExclusions.orNull?.fromJson<UsagesExclusions>() ?: UsagesExclusions.NONE

      val sources = TreeSet<Source>().also { sources ->
        codeSource.mapTo(sources) { it.excludeUsages(usagesExclusions) }
        androidResSource.mapTo(sources) { it.excludeUsages(usagesExclusions) }
        sources.addAll(androidAssetsSource)
      }
      val runtimeSources = androidResSourceRuntime.mapToOrderedSet { it.excludeUsages(usagesExclusions) }

      val projectVariant = ProjectVariant(
        coordinates = projectCoordinates,
        buildType = parameters.buildType.orNull?.intern(),
        flavor = parameters.flavor.orNull?.intern(),
        sourceKind = parameters.sourceKind.get(),
        sources = sources.efficient(),
        runtimeSources = runtimeSources.efficient(),
        classpath = classpath.efficient(),
        annotationProcessors = annotationProcessors.efficient(),
        testInstrumentationRunner = testInstrumentationRunner?.intern(),
        excludedIdentifiers = excludedIdentifiers,
      )

      output.bufferWriteJson(projectVariant)
    }

    private fun CodeSource.excludeUsages(usagesExclusions: UsagesExclusions): CodeSource {
      return copy(
        usedNonAnnotationClasses = usagesExclusions.excludeClassesFromSet(usedNonAnnotationClasses),
        usedAnnotationClasses = usagesExclusions.excludeClassesFromSet(usedAnnotationClasses),
        usedInvisibleAnnotationClasses = usagesExclusions.excludeClassesFromSet(usedInvisibleAnnotationClasses),
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
  var superClass: String? = null
  val interfaces = sortedSetOf<String>()
  val nonAnnotationClasses = sortedSetOf<String>()
  val annotationClasses = sortedSetOf<String>()
  val invisibleAnnotationClasses = sortedSetOf<String>()
  val exposedClasses = sortedSetOf<String>()
  val imports = sortedSetOf<String>()
  // val binaryClassAccesses = mutableMapOf<String, MutableSet<MemberAccess>>()

  fun concat(other: CodeSourceBuilder): CodeSourceBuilder {
    other.relativePath?.let { relativePath = it }
    other.superClass?.let { superClass = it }
    interfaces.addAll(other.interfaces)
    nonAnnotationClasses.addAll(other.nonAnnotationClasses)
    annotationClasses.addAll(other.annotationClasses)
    invisibleAnnotationClasses.addAll(other.invisibleAnnotationClasses)
    exposedClasses.addAll(other.exposedClasses)
    imports.addAll(other.imports)
    kind = other.kind
    return this
  }

  fun build(): CodeSource {
    val relativePath = checkNotNull(relativePath) { "'relativePath' was null for $className" }
    return CodeSource(
      relativePath = relativePath,
      superClass = superClass,
      interfaces = interfaces.efficient(),
      kind = kind,
      className = className,
      usedNonAnnotationClasses = nonAnnotationClasses.efficient(),
      usedAnnotationClasses = annotationClasses.efficient(),
      usedInvisibleAnnotationClasses = invisibleAnnotationClasses.efficient(),
      exposedClasses = exposedClasses.efficient(),
      imports = imports.efficient(),
      // binaryClassAccesses = binaryClassAccesses,
    )
  }
}
