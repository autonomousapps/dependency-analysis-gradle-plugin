// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.graph.Graphs.parents
import com.autonomousapps.graph.Graphs.reachableNodesMatching
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.binary.BinaryCompatibilityChecker
import com.autonomousapps.internal.graph.supers.SuperNode
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.*
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.internal.intermediates.DependencyTraceReport.Kind
import com.autonomousapps.model.internal.intermediates.DependencyTraceReports
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.visitor.GraphViewReader
import com.autonomousapps.visitor.GraphViewVisitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
public abstract class ComputeUsagesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Computes actual dependency usage"
  }

  @get:Input
  public abstract val checkSuperClasses: Property<Boolean>

  @get:Input
  public abstract val checkBinaryCompat: Property<Boolean>

  @get:Input
  public abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val graph: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val graphRuntime: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val declarations: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  public abstract val dependencies: DirectoryProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val syntheticProject: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val binaryClasses: RegularFileProperty

  @get:Input
  public abstract val kapt: Property<Boolean>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val duplicateClassesReports: ListProperty<RegularFile>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(ComputeUsagesAction::class.java) {
      it.checkSuperClasses.set(checkSuperClasses)
      it.checkBinaryCompat.set(checkBinaryCompat)

      it.buildPath.set(buildPath)
      it.graph.set(graph)
      it.graphRuntime.set(graphRuntime)
      it.declarations.set(declarations)
      it.dependencies.set(dependencies)
      it.syntheticProject.set(syntheticProject)
      it.binaryClasses.set(binaryClasses)
      it.kapt.set(kapt)
      it.duplicateClassesReports.set(duplicateClassesReports)
      it.output.set(output)
    }
  }

  public interface ComputeUsagesParameters : WorkParameters {
    public val checkSuperClasses: Property<Boolean>
    public val checkBinaryCompat: Property<Boolean>

    public val buildPath: Property<String>
    public val graph: RegularFileProperty
    public val graphRuntime: RegularFileProperty
    public val declarations: RegularFileProperty
    public val dependencies: DirectoryProperty
    public val syntheticProject: RegularFileProperty
    public val binaryClasses: RegularFileProperty
    public val kapt: Property<Boolean>
    public val duplicateClassesReports: ListProperty<RegularFile>
    public val output: RegularFileProperty
  }

  public abstract class ComputeUsagesAction : WorkAction<ComputeUsagesParameters> {

    private val buildPath = parameters.buildPath.get()
    private val graph = parameters.graph.fromJson<DependencyGraphView>()
    private val graphRuntime = parameters.graphRuntime.fromJson<DependencyGraphView>()
    private val declarations = parameters.declarations.fromJsonSet<Declaration>()
    private val project = parameters.syntheticProject.fromJson<ProjectVariant>(compressed = true)
    private val dependencies = project.dependencies(parameters.dependencies.get())
    private val duplicateClasses =
      parameters.duplicateClassesReports.get().asSequence()
        .flatMap { it.fromJsonSet<DuplicateClass>() }
        .toSortedSet()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reader = GraphViewReader(
        project = project,
        dependencies = dependencies,
        graph = graph,
        graphRuntime = graphRuntime,
        declarations = declarations,
        duplicateClasses = duplicateClasses,
        binaryClassesProvider = { parameters.binaryClasses.fromJsonSet(compressed = true) }
      )
      val visitor = GraphVisitor(
        project = project,
        buildPath = buildPath,
        kapt = parameters.kapt.get(),
        checkSuperClasses = parameters.checkSuperClasses.get(),
        checkBinaryCompat = parameters.checkBinaryCompat.get(),
      )
      reader.accept(visitor)

//      output.bufferWriteJson(visitor.report)
      val reports = visitor.reportsBuilder.reports
      output.bufferWriteJsonSet(reports)
    }
  }
}

private class GraphVisitor(
  project: ProjectVariant,
  private val buildPath: String,
  private val kapt: Boolean,
  private val checkSuperClasses: Boolean,
  private val checkBinaryCompat: Boolean,
) : GraphViewVisitor {

  companion object {
    val logger = getLogger<ComputeUsagesTask>()
  }

//  val report: DependencyTraceReport get() = reportBuilder.build()
//  val reports: Set<DependencyTraceReport> get() {
//    return reportBuilders.values.mapTo(sortedSetOf()) { it.build() }
//  }

  private val primarySourceKind = project.sourceKind
  val reportsBuilder = DependencyTraceReports(buildType = project.buildType, flavor = project.flavor)

//  private val reportBuilder = DependencyTraceReport.Builder(
//    buildType = project.buildType,
//    flavor = project.flavor,
//    sourceKind = project.sourceKind,
//  )
//  private val primaryReportBuilder = DependencyTraceReport.Builder(
//    buildType = project.buildType,
//    flavor = project.flavor,
//    sourceKind = project.sourceKind,
//  )

//  private val reportBuilders = mutableMapOf<SourceKind, DependencyTraceReport.Builder>(project.sourceKind to primaryReportBuilder)

  override fun visit(excludedIdentifier: ExcludedIdentifier, context: GraphViewVisitor.Context) {
    val coordinates = Coordinates.of(excludedIdentifier.identifier)
//    reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Excluded
//    reportBuilder[coordinates, Kind.DEPENDENCY] = Bucket.NONE
    reportsBuilder.put(context.project.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Excluded)
    reportsBuilder.put(coordinates, Kind.DEPENDENCY, Bucket.NONE)
  }

  override fun visit(dependency: Dependency, context: GraphViewVisitor.Context) {
    val dependencyCoordinates = dependency.coordinates

    var isAccessedByReflection = false
    var isAnnotationProcessor = false
    var isAnnotationProcessorCandidate = false
    var isApiCandidate = false
    var isImplCandidate = false
    var isImplByImportCandidate = false
    var isUnusedCandidate = false
    var isLintJar = false
    var isCompileOnlyCandidate = false
    var isAnnotationCandidate = false
    var isRuntimeAndroid = false
//    var hasReferencedExceptionType = false
    var usesTestInstrumentationRunner = false
    var usesResBySource = false
    var usesResByResCompileTime = false
    var usesResByResRuntime = false
    var usesAssets = false
    var usesConstant = false
    var usesInlineMember = false
    var hasServiceLoader = false
    var hasSecurityProvider = false
    var hasNativeLib = false

    dependency.capabilities.values.forEach { capability ->
      when (capability) {
        is AndroidLinterCapability -> {
          isLintJar = capability.isLintJar
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.LintJar.of(capability.lintRegistry)
          reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.LintJar.of(capability.lintRegistry))
        }

        is AndroidManifestCapability -> isRuntimeAndroid = isRuntimeAndroid(dependencyCoordinates, capability)
        is AndroidAssetCapability -> usesAssets = usesAssets(dependencyCoordinates, capability, context)
        is AndroidResCapability -> {
          usesResBySource = usesResBySource(dependencyCoordinates, capability, context)

          val result = usesResByRes(dependencyCoordinates, capability, context)
          usesResByResCompileTime = result.usesForCompileTime
          usesResByResRuntime = result.usesForRuntime
        }

        is AnnotationProcessorCapability -> {
          isAnnotationProcessor = true
          isAnnotationProcessorCandidate = usesAnnotationProcessor(dependencyCoordinates, capability, context)
        }

        is ClassCapability -> {
          if (checkBinaryCompat) {
            checkBinaryCompatibility(dependencyCoordinates, context)
          }

          // We want to track this in addition to tracking one of the below, so it's not part of the same if/else-if
          // chain.
          if (containsAndroidTestInstrumentationRunner(dependencyCoordinates, capability, context)) {
            usesTestInstrumentationRunner = true
          }

          if (isAbi(dependencyCoordinates, capability, context)) {
            isApiCandidate = true
          } else if (isImplementation(dependencyCoordinates, capability, context)) {
            isImplCandidate = true
          } else if (isImported(dependencyCoordinates, capability, context)) {
            isImplByImportCandidate = true
          } else if (isForMissingSuperclass(dependencyCoordinates, context)) {
            isImplCandidate = true
          } else if (usesAnnotation(dependencyCoordinates, capability, context)) {
            isAnnotationCandidate = true
          } else {
            isUnusedCandidate = true
          }
        }

        is ConstantCapability -> {
          // usesConstantByBytecode() is a better test, so we use that first, falling back to usesConstantByImport()
          // only if necessary.
          usesConstant = usesConstantByBytecode(dependencyCoordinates, capability, context)
          usesConstant = usesConstant || usesConstantByImport(dependencyCoordinates, capability, context)
        }

        is ExceptionCapability -> {
          // TODO(tsr): this feature is buggy and really complex to get right for limited utility.
          //hasReferencedExceptionType = isForMissingRuntimeException(dependencyCoordinates, capability, context)
        }

        is InferredCapability -> {
          if (capability.isAnnotations) {
            //reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.Annotations()
            reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.Annotations())
          }
          isCompileOnlyCandidate = capability.isAnnotations

          if (capability.reflectiveAccesses.isNotEmpty()) {
//            reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.Reflection(capability.reflectiveAccesses)
            reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.Reflection(capability.reflectiveAccesses))
            isUnusedCandidate = false
            isAccessedByReflection = true
          }
        }

        is InlineMemberCapability -> usesInlineMember = usesInlineMember(dependencyCoordinates, capability, context)

        is TypealiasCapability -> {
          if (isImplementation(dependencyCoordinates, capability, context)) {
            isImplCandidate = true
            isUnusedCandidate = false
          }
        }

        is ServiceLoaderCapability -> {
          val providers = capability.providerClasses
          hasServiceLoader = providers.isNotEmpty()
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.ServiceLoader(providers)
          reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.ServiceLoader(providers))
        }

        is NativeLibCapability -> {
          val fileNames = capability.fileNames
          hasNativeLib = fileNames.isNotEmpty()
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.NativeLib(fileNames)
          reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.NativeLib(fileNames))
        }

        is SecurityProviderCapability -> {
          val providers = capability.securityProviders
          hasSecurityProvider = providers.isNotEmpty()
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.SecurityProvider(providers)
          reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.SecurityProvider(providers))
        }
      }
    }

    // TODO KMP dependencies only contain metadata (pom/module files). This is good evidence of a facade and could be
    //  used for smarter detection of same.
    //  An example are KMP facades that resolve to -jvm artifacts
    if (dependency.capabilities.isEmpty()) {
      isUnusedCandidate = true
    }

    // this is not mutually exclusive with other buckets. E.g., Lombok is both an annotation processor and a "normal"
    // dependency. See LombokSpec.
    if (isAnnotationProcessorCandidate) {
//      reportBuilder[dependencyCoordinates, Kind.ANNOTATION_PROCESSOR] = Bucket.ANNOTATION_PROCESSOR
      reportsBuilder.put(dependencyCoordinates, Kind.ANNOTATION_PROCESSOR, Bucket.ANNOTATION_PROCESSOR)
    } else if (isAnnotationProcessor) {
      // unused annotation processor
//      reportBuilder[dependencyCoordinates, Kind.ANNOTATION_PROCESSOR] = Bucket.NONE
      reportsBuilder.put(dependencyCoordinates, Kind.ANNOTATION_PROCESSOR, Bucket.NONE)
    }

    /*
     * The order below is critically important.
     */

    if (isApiCandidate) {
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.API
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.API)
    } else if (isImplCandidate) {
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)
    } else if (isCompileOnlyCandidate) {
      // TODO compileOnlyApi? Only relevant for java-library projects
      // compileOnly candidates are not also unused candidates. Some annotations are not detectable by bytecode
      // analysis (SOURCE retention), and possibly not by source parsing either (they could be in the same package), so
      // we don't suggest removing such dependencies.
      isUnusedCandidate = false
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.COMPILE_ONLY)
    } else if (isImplByImportCandidate) {
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)
    } else if (isAnnotationCandidate) {
      isUnusedCandidate = false
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.COMPILE_ONLY)
    } else if (isAccessedByReflection) {
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)
    }
//    else if (hasReferencedExceptionType) {
//      isUnusedCandidate = false
//      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
//    }
    else if (noRealCapabilities(dependency)) {
      isUnusedCandidate = true
    }

    if (isUnusedCandidate) {
      // These weren't detected by direct presence in bytecode, but (in some cases) via source analysis. We can say less
      // about them, so we dump them into `implementation` or `runtimeOnly`.
      when {
        usesResBySource -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesResByResCompileTime -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesResByResRuntime -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        usesConstant -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesInlineMember -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.IMPL)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        isLintJar -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        isRuntimeAndroid -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        usesTestInstrumentationRunner -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        usesAssets -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasServiceLoader -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasSecurityProvider -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasNativeLib -> reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.RUNTIME_ONLY)//reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        else -> {
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.Unused
//          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.NONE
          reportsBuilder.put(context.project.getPrimarySourceKind(), dependencyCoordinates, Kind.DEPENDENCY, Reason.Unused)
          reportsBuilder.put(dependencyCoordinates, Kind.DEPENDENCY, Bucket.NONE)
        }
      }
    }
  }

  /**
   * If this [dependency] has no capabilities, or just a single capability that meets these requirements:
   * 1. The only capability is a [NativeLibCapability]
   * 2. The only capability is an [InferredCapability] where [InferredCapability.isAnnotations] is false
   *    (that is, it's not a compile-only candidate).
   */
  private fun noRealCapabilities(dependency: Dependency): Boolean {
    if (dependency.capabilities.isEmpty()) return true

    // TODO(tsr): this doesn't match the kdoc. Is this wrong or is the kdoc wrong? I think this is wrong, but no tests
    //  are failing...
    val single = dependency.capabilities.values.singleOrNull { it is InferredCapability || it is NativeLibCapability }

    return (single as? InferredCapability)?.isAnnotations == false
      || single is NativeLibCapability
  }

  private fun isRuntimeAndroid(coordinates: Coordinates, capability: AndroidManifestCapability): Boolean {
    val components = capability.componentMap
    val activities = components[AndroidManifestCapability.Component.ACTIVITY]?.also {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.activities(it)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.RuntimeAndroid.activities(it))
    }
    val providers = components[AndroidManifestCapability.Component.PROVIDER]?.also {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.providers(it)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.RuntimeAndroid.providers(it))
    }
    val receivers = components[AndroidManifestCapability.Component.RECEIVER]?.also {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.receivers(it)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.RuntimeAndroid.receivers(it))
    }
    val services = components[AndroidManifestCapability.Component.SERVICE]?.also {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.services(it)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.RuntimeAndroid.services(it))
    }

    return activities != null || providers != null || receivers != null || services != null
  }

  private fun isAbi(
    coordinates: Coordinates,
    capability: ClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val exposedClasses = context.project.exposedClasses.asSequence().filter { exposedClass ->
      exposedClass.name in capability.classes
    }.toSortedSet()

    return if (exposedClasses.isNotEmpty()) {
      reportsBuilder.put(exposedClasses.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Abi(exposedClasses.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  // TODO move
  private fun Set<ClassReference>.getPrimarySourceKind(): SourceKind = getPrimarySourceKind(primarySourceKind)

  private fun isImplementation(
    coordinates: Coordinates,
    capability: ClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val implClasses = (context.project.implementationClasses.asSequence().filter { implClass ->
      implClass.name in capability.classes
    } + context.project.usedNonAnnotationClassesWithinVisibleAnnotationBySrc.asSequence()
      .filter { (nonAnnotationClass, enclosingAnnotation) ->
        nonAnnotationClass.name in capability.classes && enclosingAnnotation.name !in capability.classes
      }.map { it.key }).toSortedSet()

    return if (implClasses.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Impl(implClasses)
      reportsBuilder.put(implClasses.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Impl(implClasses.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  /**
   * This checks if [coordinates] provides a symbol for a superclass missing from THIS module's source code. It does NOT
   * check if [coordinates] (or any dependency) has superclasses missing from the compile classpath.
   */
  private fun isForMissingSuperclass(
    coordinates: Coordinates,
    context: GraphViewVisitor.Context,
  ): Boolean {
    if (!checkSuperClasses) return false

    val superGraph = context.superGraph
    val externalSupers = context.project.externalSupers

    // TODO(tsr): this has no special KMP common source set support
    // collect all the dependencies associated with external supers
    // nb: we start by iterating over `supergraph.nodes()`, and then filtering, as that is _far more efficient_
    // then iterating over `externalSupers` and then calling `supergraph.nodes()` repeatedly: I have observed graphs
    // with hundreds of thousands of nodes.
    val requiredExternalClasses = superGraph
      .reachableNodesMatching { superNode -> superNode.className in externalSupers.mapToSet { it.name } }
      .mapNotNull { superNode ->
        val deps = superNode.deps.filterToOrderedSet { dep ->
          // If dep has just one parent and it's the root, then we must retain that edge
          val graph = context.graph.graph
          graph.parents(dep).singleOrNull { it == graph.root() } != null
        }

        if (deps.isNotEmpty()) {
          SuperNode(superNode.className).apply { this.deps += deps }
        } else {
          null
        }
      }
      // filter for the nodes associated with _this_ dependency
      .filter { node -> coordinates in node.deps }
      .map { node -> node.className }
      .toSortedSet()

    return if (requiredExternalClasses.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ImplSuper(requiredExternalClasses)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.ImplSuper(requiredExternalClasses))
      true
    } else {
      false
    }
  }

  private fun usesAnnotation(
    coordinates: Coordinates,
    capability: ClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val annoClasses = context.project.usedAnnotationClassesBySrc.asSequence().filter { annoClass ->
      annoClass.name in capability.classes
    }.toSortedSet()

    return if (annoClasses.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Annotation(annoClasses)
      reportsBuilder.put(annoClasses.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Annotation(annoClasses.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  private fun checkBinaryCompatibility(
    coordinates: Coordinates,
    context: GraphViewVisitor.Context,
  ) {
    val binaryClasses = context.binaryClasses[coordinates] ?: error("No binary classes for '${coordinates.identifier}'.")
    val result = BinaryCompatibilityChecker(coordinates, binaryClasses, context).result ?: return

    if (!result.isBinaryCompatible) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.BinaryIncompatible(result.relevantMemberAccesses, result.nonMatchingBinaryClasses)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.BinaryIncompatible(result.relevantMemberAccesses, result.nonMatchingBinaryClasses))
    }
  }

  private fun isImplementation(
    coordinates: Coordinates,
    typealiasCapability: TypealiasCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val fqnTypealiases = typealiasCapability.typealiases.flatMapToOrderedSet { ta ->
      ta.typealiases.flatMap {
        listOf(
          "${ta.packageName}.${it.name}",
          "${ta.alternatePackageName}.${it.name}",
        )
      }
    }

    val usedClasses = context.project.usedClasses.asSequence().filter { usedClass ->
      usedClass.name in fqnTypealiases
    }.toSortedSet()

    val imports = context.project.imports.asSequence().filter { import ->
      import.name in fqnTypealiases
    }.toSortedSet()

    val used = usedClasses + imports

    return if (used.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Typealias(used)
      reportsBuilder.put(used.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Typealias(used.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  private fun isImported(
    coordinates: Coordinates,
    capability: ClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val imports = context.project.imports.asSequence().filter { import ->
      import.name in capability.classes
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Imported(imports)
      reportsBuilder.put(imports.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Imported(imports.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  private fun containsAndroidTestInstrumentationRunner(
    coordinates: Coordinates,
    capability: ClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val testInstrumentationRunner = context.project.testInstrumentationRunner ?: return false

    return if (testInstrumentationRunner in capability.classes) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.TestInstrumentationRunner(testInstrumentationRunner)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.TestInstrumentationRunner(testInstrumentationRunner))
      true
    } else {
      false
    }
  }

  private fun usesConstantByBytecode(
    coordinates: Coordinates,
    capability: ConstantCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    // This test is slightly wrong:
    // 1. Inferred constants are missing the name of the constant, meaning we can only match by value and descriptor,
    //    leading to false positives.
    // 2. Not only that, but there is certainly a 1-to-many relationship between value/descriptor on the consumer side
    //    and constants on the producer side.
    val inferredUsages = context.project.inferredConstants
    val usedConstants = capability.constants.asSequence()
      .flatMap { it.value }
      .filter { candidate ->
        val descriptor = candidate.descriptor
        val value = candidate.value

        inferredUsages.any { usage ->
          descriptor == usage.descriptor && value == usage.value
        }
      }
      .toSortedSet()

    val candidateImports = capability.constants.asSequence()
      .map { (fqcn, _) -> fqcn.replace('$', '.') }
      .toSet()

    // This test is slightly wrong in a couple of ways:
    // 1. Something might be imported but not used.
    // 2. The thing being imported might be an annotation, and those are generally compileOnly
    // 3. The way this test is used in Reason would result in misleading output about _how_ the dep is being used
    val usedImports = context.project.imports.asSequence()
      .filter { import -> import.name in candidateImports }
      .toSet()

    // Because both of the above tests are "slightly wrong," we combine them and hope their deficiencies cancel each
    // other out. The import test should ensure we don't have a false positive and declare an unused dependency as used.
    // The ldc test will restrict the import heuristic, allowing us to report more meaningful (inferred) usage
    // information.
    return if (usedImports.isNotEmpty() && usedConstants.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ConstantBytecode(usedConstants)
      // TODO(tsr): we get the primary source kind from `usedImports`, but Reason uses `usedConstants`.
      // TODO: I need to double-check if we can simplify constant detection for Kotlin
      reportsBuilder.put(usedImports.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.ConstantBytecode(usedConstants))
      true
    } else {
      false
    }
  }

  private fun usesConstantByImport(
    coordinates: Coordinates,
    capability: ConstantCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    fun optionalStarImport(fqcn: String): List<String> {
      return if (fqcn.contains(".")) {
        listOf("${fqcn.substringBeforeLast('.')}.*")
      } else {
        // "fqcn" is not in a package, and so contains no dots
        // a star import makes no sense in this context
        emptyList()
      }
    }

    // "Companion" is highly suggestive of a Kotlin dependency, in which case constant imports look like
    // ```
    // import com.foo.Companion.CONSTANT
    // ```
    fun optionalCompanionImport(names: Set<String>, fqcn: String): List<String> {
      return if ("Companion" in names) {
        names.map { name -> "$fqcn.Companion.$name" }
      } else {
        emptyList()
      }
    }

    val ktFiles = capability.ktFiles
    val candidateImports = capability.constants.asSequence()
      .flatMap { (fqcn, fields) ->
        val names = fields.mapToOrderedSet { it.name }

        val ktPrefix = ktFiles
          .find { ktFile -> ktFile.fqcn == fqcn }
          ?.name
          ?.let { name -> fqcn.removeSuffix(name) }

        val ktImports = names.mapNotNull { name -> ktPrefix?.let { "$it$name" } }

        ktImports +
          listOf("$fqcn.*") +
          optionalStarImport(fqcn) +
          names.map { name -> "$fqcn.$name" } +
          optionalCompanionImport(names, fqcn)
      }
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/687
      .map { import -> import.replace('$', '.') }
      .toSet()

    val imports = context.project.imports.asSequence()
      .filter { import -> import.name in candidateImports }
      .toSortedSet()

    return if (imports.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ConstantImport(imports)
      reportsBuilder.put(imports.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.ConstantImport(imports.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

//  private fun isForMissingRuntimeException(
//    coordinates: Coordinates,
//    capability: ExceptionCapability,
//    context: GraphViewVisitor.Context,
//  ): Boolean {
//    // TODO(tsr): extract this to a testable function
//    val codeSource = context.project.codeSource
//    if (codeSource.isEmpty()) {
//      // Nothing to do here. We don't have any code, so we can't be referring to any exception types, by definition.
//      // This is an optimization.
//      return false
//    }
//
//    // These are all the exception types referenced by `this` dependency.
//    val referencedExceptions = capability.exceptions.values.flatten().toSet()
//
//    // Ideally this would be a single item, but it could be multiple if we have duplicates on the classpath.
//    val exceptionProviders: Map<Coordinates, Set<String>> = context.dependencies
//      // We only care about dependencies that have classes
//      .mapSecondNotNull { dependency -> dependency.coordinates to dependency.findCapability<BinaryClassCapability>() }
//      // Find the dependencies that have a class that matches any exception referenced by `this` dependency.
//      .mapNotNull { (coordinates, binaryClass) ->
//        val relevantTypes = binaryClass.classes.filterToOrderedSet { it in referencedExceptions }
//        if (relevantTypes.isEmpty()) {
//          null
//        } else {
//          coordinates to relevantTypes
//        }
//      }
//      .toMap()
//
//    if (exceptionProviders.isEmpty()) {
//      // Nothing to do here.
//      return false
//    }
//
//    // These are all the dependencies of this project that use the exception types provided by `coordinates`.
//    val users: Map<Coordinates, Map<String, Set<String>>> = context.dependencies.asSequence()
//      // The dependency can see all its own exceptions
//      .filterNot { dependency -> dependency.coordinates == coordinates }
//      // Don't scan the exception providers, that doesn't make sense and leads to incorrect results
//      .filterNot { dependency -> dependency.coordinates in exceptionProviders.keys }
//      // If the dependency's runtime graph can reach `this` dependency, then we can skip analysis
//      .filterNot { dependency ->
//        val graph = context.graphRuntime.graph
//        if (dependency.coordinates in graph.nodes()) {
//          graph
//            .reachableNodes(dependency.coordinates)
//            .contains(coordinates)
//        } else {
//          false
//        }
//      }
//      .mapSecondNotNull { dependency -> dependency.coordinates to dependency.findCapability<ExceptionCapability>() }
//      .associateNotNull { (coordinates, exceptionCapability) ->
//        val classToExceptionType = exceptionCapability.exceptions
//          .mapNotNull { (className, types) ->
//            val relevantTypes = types.filterToOrderedSet { type -> type in referencedExceptions }
//            if (relevantTypes.isEmpty()) {
//              null
//            } else {
//              className to relevantTypes
//            }
//          }
//          // It only matters if the code of `this` project refers to a class that itself references a relevant type
//          .filter { (className, _) -> codeSource.any { source -> className in source.usedNonAnnotationClasses } }
//          .toMap()
//
//        if (classToExceptionType.isNotEmpty()) {
//          // The specific exception types this dependency references
//          coordinates.maybeProjectCoordinates(buildPath) to classToExceptionType
//        } else {
//          null
//        }
//      }
//
//    return if (users.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Exceptions(users)
//      true
//    } else {
//      false
//    }
//  }

  /** Returns `true` if `capability.assets` is not empty and if the project uses `android.content.res.AssetManager`. */
  private fun usesAssets(
    coordinates: Coordinates,
    capability: AndroidAssetCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val usesAssets = capability.assets.isNotEmpty()
      && "android.content.res.AssetManager" in context.project.usedNonAnnotationClassesBySrc.mapToSet { it.name }

    return if (usesAssets) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Asset(capability.assets)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.Asset(capability.assets))
      true
    } else {
      false
    }
  }

  private fun usesResBySource(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val projectImports = context.project.imports
    val imports = listOf(capability.rImport, capability.rImport.removeSuffix("R") + "*").asSequence()
      .filter { import -> import in projectImports.mapToSet { it.name } }
      .toSortedSet()

    return if (imports.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResBySrc(imports)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.ResBySrc(imports))
      true
    } else {
      false
    }
  }

  @Suppress("DuplicatedCode") // stop bothering me
  private fun usesResByRes(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context,
  ): ResByResAnalysisResult {
    // compile-time
    val compileTimeStyleParentRefs = mutableSetOf<AndroidResSource.StyleParentRef>()
    val compileTimeAttrRefs = mutableSetOf<AndroidResSource.AttrRef>()

    // runtime
    val runtimeStyleParentRefs = mutableSetOf<AndroidResSource.StyleParentRef>()
    val runtimeAttrRefs = mutableSetOf<AndroidResSource.AttrRef>()

    // By exiting at the first discovered usage, we can conclude the dependency is used without being able to report ALL
    // the usages via Reason. But that's ok, this should be faster.
    for ((type, id) in capability.lines) {
      // compile-time
      for ((_, styleParentRefs, attrRefs) in context.project.androidResSource) {
        styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }?.let { compileTimeStyleParentRefs.add(it) }

        attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }?.let { compileTimeAttrRefs.add(it) }
      }

      // runtime
      for ((_, styleParentRefs, attrRefs) in context.project.androidResRuntimeSource) {
        styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }?.let { runtimeStyleParentRefs.add(it) }

        attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }?.let { runtimeAttrRefs.add(it) }
      }
    }

    val allCompileTimeRefs: Set<AndroidResSource.ResRef> = compileTimeStyleParentRefs + compileTimeAttrRefs
    val allRuntimeRefs: Set<AndroidResSource.ResRef> = runtimeStyleParentRefs + runtimeAttrRefs

    val usesForCompileTime = if (allCompileTimeRefs.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes.resRefs(allCompileTimeRefs)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.ResByRes.resRefs(allCompileTimeRefs))
      true
    } else {
      false
    }

    val usesForRuntime = if (allRuntimeRefs.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByResRuntime.resRefs(allRuntimeRefs)
      reportsBuilder.put(primarySourceKind, coordinates, Kind.DEPENDENCY, Reason.ResByResRuntime.resRefs(allRuntimeRefs))
      true
    } else {
      false
    }

    return ResByResAnalysisResult(
      usesForCompileTime = usesForCompileTime,
      usesForRuntime = usesForRuntime,
    )
  }

  private fun usesInlineMember(
    coordinates: Coordinates,
    capability: InlineMemberCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val candidateImports = capability.inlineMembers
      .flatMapToOrderedSet(InlineMemberCapability.InlineMember::candidateImports)

    val imports = context.project.imports.asSequence().filter { import ->
      import.name in candidateImports
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
//      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Inline(imports)
      reportsBuilder.put(imports.getPrimarySourceKind(), coordinates, Kind.DEPENDENCY, Reason.Inline(imports.mapToOrderedSet { it.name }))
      true
    } else {
      false
    }
  }

  private fun usesAnnotationProcessor(
    coordinates: Coordinates,
    capability: AnnotationProcessorCapability,
    context: GraphViewVisitor.Context,
  ): Boolean = AnnotationProcessorDetector(
    coordinates,
    capability.supportedAnnotationTypes,
    kapt,
//    reportBuilder,
    primarySourceKind,
    reportsBuilder,
  ).usesAnnotationProcessor(context)
}

private class AnnotationProcessorDetector(
  private val coordinates: Coordinates,
  private val supportedTypes: Set<String>,
  private val isKaptApplied: Boolean,
//  private val reportBuilder: DependencyTraceReport.Builder,
  private val primarySourceKind: SourceKind,
  private val reportsBuilder: DependencyTraceReports,
) {

  // convert ["lombok.*"] to [lombok.(package) regex]
  private val stars = supportedTypes
    .filter { it.endsWith("*") }
    .map { it.replace(".", "\\.") }
    .map { it.replace("*", JAVA_SUB_PACKAGE) }
    .map { it.toRegex(setOf(RegexOption.IGNORE_CASE)) }

  fun usesAnnotationProcessor(context: GraphViewVisitor.Context): Boolean {
    return (context.project.usedByImport() || context.project.usedByClass()).also {
      if (!it) reason(primarySourceKind, Reason.Unused)
    }
  }

  private fun ProjectVariant.usedByImport(): Boolean {
    val usedImports = mutableSetOf<ClassReference>()
    for (import in imports) {
      if (import.name in supportedTypes || stars.any { it.matches(import.name) }) {
        usedImports.add(import)
      }
    }

    return if (usedImports.isNotEmpty()) {
      reason(usedImports.getPrimarySourceKind(), Reason.AnnotationProcessor.imports(usedImports.mapToOrderedSet { it.name }, isKaptApplied))
      true
    } else {
      false
    }
  }

  private fun ProjectVariant.usedByClass(): Boolean {
    val theUsedClasses = mutableSetOf<ClassReference>()
    for (clazz in usedNonAnnotationClasses) {
      if (clazz.name in supportedTypes || stars.any { it.matches(clazz.name) }) {
        theUsedClasses.add(clazz)
      }
    }

    return if (theUsedClasses.isNotEmpty()) {
      reason(theUsedClasses.getPrimarySourceKind(), Reason.AnnotationProcessor.classes(theUsedClasses.mapToOrderedSet { it.name }, isKaptApplied))
      true
    } else {
      false
    }
  }

  private fun reason(sourceKind: SourceKind, reason: Reason) {
//    reportBuilder[coordinates, Kind.ANNOTATION_PROCESSOR] = reason
    reportsBuilder.put(sourceKind, coordinates, Kind.ANNOTATION_PROCESSOR, reason)
  }

  private fun Set<ClassReference>.getPrimarySourceKind(): SourceKind = getPrimarySourceKind(primarySourceKind)
}

private class ResByResAnalysisResult(
  val usesForCompileTime: Boolean,
  val usesForRuntime: Boolean,
)

// TODO move / test / document
// TODO only do this for KMP projects!
// TODO don't reference source set names by string literals!
// TODO validate this is a valid KmpSourceKind (look at compileClasspathName and runtime)
//  I think it literally isn't, since there are no "common" classpaths. So need to figure that out. Some code later on checks that stuff.
// I think we need to check in order for commonMain, commonTest (any others?)
private fun Set<ClassReference>.getPrimarySourceKind(fallback: SourceKind): SourceKind {
  if (fallback !is KmpSourceKind) return fallback

  val commonClassReference = find { it.kmpCommonSourceSetName == "commonMain" } ?: find { it.kmpCommonSourceSetName == "commonTest" }
  val kmpCommonSourceSetName = commonClassReference?.kmpCommonSourceSetName

  return if (kmpCommonSourceSetName != null) {
    // TODO not sure about this
    val a = fallback.copy(name = kmpCommonSourceSetName)
    a
  } else {
    fallback
  }
}
