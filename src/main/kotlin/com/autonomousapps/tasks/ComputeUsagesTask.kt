// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.graph.Graphs.parents
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.graph.supers.SuperNode
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.declaration.internal.Bucket
import com.autonomousapps.model.declaration.internal.Declaration
import com.autonomousapps.model.internal.*
import com.autonomousapps.model.internal.intermediates.DependencyTraceReport
import com.autonomousapps.model.internal.intermediates.DependencyTraceReport.Kind
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.visitor.GraphViewReader
import com.autonomousapps.visitor.GraphViewVisitor
import com.google.common.graph.Graphs
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
abstract class ComputeUsagesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Computes actual dependency usage"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val graph: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  abstract val dependencies: DirectoryProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val syntheticProject: RegularFileProperty

  @get:Input
  abstract val kapt: Property<Boolean>

  @get:Input
  abstract val checkSuperClasses: Property<Boolean>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val duplicateClassesReports: ListProperty<RegularFile>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ComputeUsagesAction::class.java) {
      graph.set(this@ComputeUsagesTask.graph)
      declarations.set(this@ComputeUsagesTask.declarations)
      dependencies.set(this@ComputeUsagesTask.dependencies)
      syntheticProject.set(this@ComputeUsagesTask.syntheticProject)
      kapt.set(this@ComputeUsagesTask.kapt)
      checkSuperClasses.set(this@ComputeUsagesTask.checkSuperClasses)
      duplicateClassesReports.set(this@ComputeUsagesTask.duplicateClassesReports)
      output.set(this@ComputeUsagesTask.output)
    }
  }

  interface ComputeUsagesParameters : WorkParameters {
    val graph: RegularFileProperty
    val declarations: RegularFileProperty
    val dependencies: DirectoryProperty
    val syntheticProject: RegularFileProperty
    val kapt: Property<Boolean>
    val checkSuperClasses: Property<Boolean>
    val duplicateClassesReports: ListProperty<RegularFile>
    val output: RegularFileProperty
  }

  abstract class ComputeUsagesAction : WorkAction<ComputeUsagesParameters> {

    private val graph = parameters.graph.fromJson<DependencyGraphView>()
    private val declarations = parameters.declarations.fromJsonSet<Declaration>()
    private val project = parameters.syntheticProject.fromJson<ProjectVariant>()
    private val dependencies = project.dependencies(parameters.dependencies.get())
    private val duplicateClasses = parameters.duplicateClassesReports.get().asSequence()
      .map { it.fromJsonSet<DuplicateClass>() }
      .flatten()
      .toSortedSet()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reader = GraphViewReader(
        project = project,
        dependencies = dependencies,
        graph = graph,
        declarations = declarations,
        duplicateClasses = duplicateClasses,
      )
      val visitor = GraphVisitor(
        project = project,
        kapt = parameters.kapt.get(),
        checkSuperClasses = parameters.checkSuperClasses.get(),
      )
      reader.accept(visitor)

      val report = visitor.report
      output.bufferWriteJson(report)
    }
  }
}

private class GraphVisitor(
  project: ProjectVariant,
  private val kapt: Boolean,
  private val checkSuperClasses: Boolean,
) : GraphViewVisitor {

  val report: DependencyTraceReport get() = reportBuilder.build()

  private val reportBuilder = DependencyTraceReport.Builder(
    buildType = project.buildType,
    flavor = project.flavor,
    sourceKind = project.sourceKind,
  )

  override fun visit(dependency: Dependency, context: GraphViewVisitor.Context) {
    val dependencyCoordinates = dependency.coordinates

    var isAnnotationProcessor = false
    var isAnnotationProcessorCandidate = false
    var isApiCandidate = false
    var isImplCandidate = false
    var isImplByImportCandidate = false
    var isUnusedCandidate = false
    var isLintJar = false
    var isCompileOnlyCandidate = false
    var isRequiredAnnotationCandidate = false
    var isCompileOnlyAnnotationCandidate = false
    var isRuntimeAndroid = false
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
      @Suppress("UNUSED_VARIABLE", "unused") // exhaustive when
      val ignored: Any = when (capability) {
        is AndroidLinterCapability -> {
          isLintJar = capability.isLintJar
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.LintJar.of(capability.lintRegistry)
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

        is BinaryClassCapability -> {
          // TODO(tsr) re-evaluate once we have minified intermediates
          // checkBinaryCompatibility(dependencyCoordinates, capability, context)

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
          } else if (usesAnnotation(dependencyCoordinates, capability, context)) {
            isRequiredAnnotationCandidate = true
          } else if (isForMissingSuperclass(dependencyCoordinates, capability, context)) {
            isImplCandidate = true
          } else if (usesInvisibleAnnotation(dependencyCoordinates, capability, context)) {
            isCompileOnlyAnnotationCandidate = true
          } else {
            isUnusedCandidate = true
          }
        }

        is ConstantCapability -> usesConstant = usesConstant(dependencyCoordinates, capability, context)
        is InferredCapability -> {
          if (capability.isCompileOnlyAnnotations) {
            reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.CompileTimeAnnotations()
          }
          isCompileOnlyCandidate = capability.isCompileOnlyAnnotations
        }

        is InlineMemberCapability -> usesInlineMember = usesInlineMember(dependencyCoordinates, capability, context)

        is TypealiasCapability -> {
          if (isImplementation(dependencyCoordinates, capability, context)) {
            isImplCandidate = true
            isUnusedCandidate = false
          }

          // for exhaustive when
          Unit
        }

        is ServiceLoaderCapability -> {
          val providers = capability.providerClasses
          hasServiceLoader = providers.isNotEmpty()
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.ServiceLoader(providers)
        }

        is NativeLibCapability -> {
          val fileNames = capability.fileNames
          hasNativeLib = fileNames.isNotEmpty()
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.NativeLib(fileNames)
        }

        is SecurityProviderCapability -> {
          val providers = capability.securityProviders
          hasSecurityProvider = providers.isNotEmpty()
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.SecurityProvider(providers)
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
      reportBuilder[dependencyCoordinates, Kind.ANNOTATION_PROCESSOR] = Bucket.ANNOTATION_PROCESSOR
    } else if (isAnnotationProcessor) {
      // unused annotation processor
      reportBuilder[dependencyCoordinates, Kind.ANNOTATION_PROCESSOR] = Bucket.NONE
    }

    /*
     * The order below is critically important.
     */

    if (isApiCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.API
    } else if (isImplCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
    } else if (isCompileOnlyCandidate) {
      // TODO compileOnlyApi? Only relevant for java-library projects
      // compileOnly candidates are not also unused candidates. Some annotations are not detectable by bytecode
      // analysis (SOURCE retention), and possibly not by source parsing either (they could be in the same package), so
      // we don't suggest removing such dependencies.
      isUnusedCandidate = false
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
    } else if (isImplByImportCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
    } else if (isRequiredAnnotationCandidate) {
      // We detected an annotation, but it's a RUNTIME annotation, so we can't suggest it be moved to compileOnly.
      // Don't suggest removing it!
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
    } else if (isCompileOnlyAnnotationCandidate) {
      isUnusedCandidate = false
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
    } else if (noRealCapabilities(dependency)) {
      isUnusedCandidate = true
    }

    if (isUnusedCandidate) {
      // These weren't detected by direct presence in bytecode, but (in some cases) via source analysis. We can say less
      // about them, so we dump them into `implementation` or `runtimeOnly`.
      when {
        usesResBySource -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesResByResCompileTime -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesResByResRuntime -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY // TODO(tsr): is this correct?
        usesConstant -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesInlineMember -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        isLintJar -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        isRuntimeAndroid -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        usesTestInstrumentationRunner -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        usesAssets -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasServiceLoader -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasSecurityProvider -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        hasNativeLib -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        else -> {
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.NONE
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.Unused
        }
      }
    }
  }

  /**
   * If this [dependency] has no capabilities, or just a single capability that meets these requirements:
   * 1. The only capability is a [NativeLibCapability]
   * 2. The only capability is an [InferredCapability] where [InferredCapability.isCompileOnlyAnnotations] is false
   *    (that is, it's not a compile-only candidate).
   */
  private fun noRealCapabilities(dependency: Dependency): Boolean {
    if (dependency.capabilities.isEmpty()) return true

    // TODO(tsr): this doesn't match the kdoc. Is this wrong or is the kdoc wrong? I think this is wrong, but no tests
    //  are failing...
    val single = dependency.capabilities.values.singleOrNull { it is InferredCapability || it is NativeLibCapability }

    return (single as? InferredCapability)?.isCompileOnlyAnnotations == false
      || single is NativeLibCapability
  }

  private fun isRuntimeAndroid(coordinates: Coordinates, capability: AndroidManifestCapability): Boolean {
    val components = capability.componentMap
    val activities = components[AndroidManifestCapability.Component.ACTIVITY]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.activities(it)
    }
    val providers = components[AndroidManifestCapability.Component.PROVIDER]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.providers(it)
    }
    val receivers = components[AndroidManifestCapability.Component.RECEIVER]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.receivers(it)
    }
    val services = components[AndroidManifestCapability.Component.SERVICE]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.services(it)
    }

    return activities != null || providers != null || receivers != null || services != null
  }

  private fun isAbi(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val exposedClasses = context.project.exposedClasses.asSequence().filter { exposedClass ->
      capability.classes.contains(exposedClass)
    }.toSortedSet()

    return if (exposedClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Abi(exposedClasses)
      true
    } else {
      false
    }
  }

  private fun isImplementation(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val implClasses = context.project.implementationClasses.asSequence().filter { implClass ->
      capability.classes.contains(implClass)
    }.toSortedSet()

    return if (implClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Impl(implClasses)
      true
    } else {
      false
    }
  }

  private fun isForMissingSuperclass(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    if (!checkSuperClasses) return false

    val superGraph = context.superGraph
    val externalSupers = context.project.externalSupers

    // collect all the dependencies associated with external supers
    // nb: we start by iterating over `supergraph.nodes()`, and then filtering, as that is _far more efficient_
    // then iterating over `externalSupers` and then calling `supergraph.nodes()` repeatedly: I have observed graphs
    // with hundreds of thousands of nodes. This is why we use Guava directly here rather than going through our own
    // Graphs wrapper. There's a yet-to-be-published update to the wrapper that does this for us.
    val requiredExternalClasses = superGraph.nodes().asSequence()
      .filter { superNode -> superNode.className in externalSupers }
      .flatMap { superNode -> Graphs.reachableNodes(superGraph, superNode) }
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
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ImplSuper(requiredExternalClasses)
      true
    } else {
      false
    }
  }

  private fun usesAnnotation(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val annoClasses = context.project.usedAnnotationClassesBySrc.asSequence().filter { annoClass ->
      capability.classes.contains(annoClass)
    }.toSortedSet()

    return if (annoClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Annotation(annoClasses)
      true
    } else {
      false
    }
  }

  private fun usesInvisibleAnnotation(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val annoClasses = context.project.usedInvisibleAnnotationClassesBySrc.asSequence().filter { annoClass ->
      capability.classes.contains(annoClass)
    }.toSortedSet()

    return if (annoClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.InvisibleAnnotation(annoClasses)
      true
    } else {
      false
    }
  }

  // private fun checkBinaryCompatibility(
  //   coordinates: Coordinates,
  //   binaryClassCapability: BinaryClassCapability,
  //   context: GraphViewVisitor.Context,
  // ) {
  //   // Can't be incompatible if the code compiles in the context of no duplication
  //   if (context.duplicateClasses.isEmpty()) return
  //
  //   // TODO(tsr): special handling for @Composable
  //   val memberAccessOwners = context.project.memberAccesses.mapToSet { it.owner }
  //   val relevantDuplicates = context.duplicateClasses
  //     .filter { duplicate -> coordinates in duplicate.dependencies && duplicate.className in memberAccessOwners }
  //     .filter { duplicate -> duplicate.classpathName == DuplicateClass.COMPILE_CLASSPATH_NAME }
  //
  //   // Can't be incompatible if the code compiles in the context of no relevant duplication
  //   if (relevantDuplicates.isEmpty()) return
  //
  //   val relevantDuplicateClassNames = relevantDuplicates.mapToOrderedSet { it.className }
  //   val relevantMemberAccesses = context.project.memberAccesses
  //     .filterToOrderedSet { access -> access.owner in relevantDuplicateClassNames }
  //
  //   val partitionResult = relevantMemberAccesses.mapToSet { access ->
  //     binaryClassCapability.findMatchingClasses(access)
  //   }.reduce()
  //   val matchingBinaryClasses = partitionResult.matchingClasses
  //   val nonMatchingBinaryClasses = partitionResult.nonMatchingClasses
  //
  //   // There must be a compatible BinaryClass.<field|method> for each MemberAccess for the usage to be binary-compatible
  //   val isBinaryCompatible = relevantMemberAccesses.all { access ->
  //     when (access) {
  //       is MemberAccess.Field -> {
  //         matchingBinaryClasses.any { bin ->
  //           bin.effectivelyPublicFields.any { field ->
  //             field.matches(access)
  //           }
  //         }
  //       }
  //
  //       is MemberAccess.Method -> {
  //         matchingBinaryClasses.any { bin ->
  //           bin.effectivelyPublicMethods.any { method ->
  //             method.matches(access)
  //           }
  //         }
  //       }
  //     }
  //   }
  //
  //   if (!isBinaryCompatible) {
  //     reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.BinaryIncompatible(
  //       relevantMemberAccesses, nonMatchingBinaryClasses
  //     )
  //   }
  // }
  //
  // // TODO: I think this could be more efficient
  // private fun Set<BinaryClassCapability.PartitionResult>.reduce(): BinaryClassCapability.PartitionResult {
  //   val matches = sortedSetOf<BinaryClass>()
  //   val nonMatches = sortedSetOf<BinaryClass>()
  //
  //   forEach { result ->
  //     matches.addAll(result.matchingClasses)
  //     nonMatches.addAll(result.nonMatchingClasses)
  //   }
  //
  //   return BinaryClassCapability.PartitionResult(
  //     matchingClasses = matches.reduce(),
  //     nonMatchingClasses = nonMatches.reduce(),
  //   )
  // }
  //
  // private fun Set<BinaryClass>.reduce(): Set<BinaryClass> {
  //   val builders = mutableMapOf<String, BinaryClass.Builder>()
  //
  //   forEach { bin ->
  //     builders.merge(
  //       bin.className,
  //       BinaryClass.Builder(
  //         className = bin.className,
  //         superClassName = bin.superClassName,
  //         interfaces = bin.interfaces.toSortedSet(),
  //         effectivelyPublicFields = bin.effectivelyPublicFields.toSortedSet(),
  //         effectivelyPublicMethods = bin.effectivelyPublicMethods.toSortedSet(),
  //       )
  //     ) { acc, inc ->
  //       acc.apply {
  //         effectivelyPublicFields.addAll(inc.effectivelyPublicFields)
  //         effectivelyPublicMethods.addAll(inc.effectivelyPublicMethods)
  //       }
  //     }
  //   }
  //
  //   return builders.values.mapToOrderedSet { it.build() }
  // }

  private fun isImplementation(
    coordinates: Coordinates,
    typealiasCapability: TypealiasCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val usedClasses = context.project.usedClassesBySrc.asSequence().filter { usedClass ->
      typealiasCapability.typealiases.any { ta ->
        ta.typealiases.map { "${ta.packageName}.${it.name}" }.contains(usedClass)
      }
    }.toSortedSet()

    return if (usedClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Typealias(usedClasses)
      true
    } else {
      false
    }
  }

  private fun isImported(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val imports = context.project.imports.asSequence().filter { import ->
      capability.classes.contains(import)
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Imported(imports)
      true
    } else {
      false
    }
  }

  private fun containsAndroidTestInstrumentationRunner(
    coordinates: Coordinates,
    capability: BinaryClassCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val testInstrumentationRunner = context.project.testInstrumentationRunner ?: return false

    return if (capability.classes.contains(testInstrumentationRunner)) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.TestInstrumentationRunner(testInstrumentationRunner)
      true
    } else {
      false
    }
  }

  private fun usesConstant(
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
      .flatMap { (fqcn, names) ->
        val ktPrefix = ktFiles.find {
          it.fqcn == fqcn
        }?.name?.let { name ->
          fqcn.removeSuffix(name)
        }
        val ktImports = names.mapNotNull { name -> ktPrefix?.let { "$it$name" } }

        ktImports +
          listOf("$fqcn.*") +
          optionalStarImport(fqcn) +
          names.map { name -> "$fqcn.$name" } +
          optionalCompanionImport(names, fqcn)
      }
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/687
      .map { it.replace('$', '.') }
      .toSet()

    val imports = context.project.imports.asSequence().filter { import ->
      candidateImports.contains(import)
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Constant(imports)
      true
    } else {
      false
    }
  }

  /**
   * Returns `true` if `capability.assets` is not empty and if the project uses `android.content.res.AssetManager`.
   */
  private fun usesAssets(
    coordinates: Coordinates,
    capability: AndroidAssetCapability,
    context: GraphViewVisitor.Context,
  ): Boolean = (capability.assets.isNotEmpty()
    && context.project.usedNonAnnotationClassesBySrc.contains("android.content.res.AssetManager")
    ).andIfTrue {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Asset(capability.assets)
    }

  private fun usesResBySource(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context,
  ): Boolean {
    val projectImports = context.project.imports
    val imports = listOf(capability.rImport, capability.rImport.removeSuffix("R") + "*").asSequence()
      .filter { import -> projectImports.contains(import) }
      .toSortedSet()

    return if (imports.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResBySrc(imports)
      true
    } else {
      false
    }
  }

  // TODO(tsr): move elsewhere
  internal class ResByResAnalysisResult(
    val usesForCompileTime: Boolean,
    val usesForRuntime: Boolean,
  )

  private fun usesResByRes(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context,
  ): ResByResAnalysisResult {
    // TODO(tsr): simplify duplication?
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
      for (candidate in context.project.androidResSource) {
        candidate.styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }?.let { compileTimeStyleParentRefs.add(it) }

        candidate.attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }?.let { compileTimeAttrRefs.add(it) }
      }

      // runtime
      for (candidate in context.project.androidResRuntimeSource) {
        candidate.styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }?.let { runtimeStyleParentRefs.add(it) }

        candidate.attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }?.let { runtimeAttrRefs.add(it) }
      }
    }

    val allCompileTimeRefs: Set<AndroidResSource.ResRef> = compileTimeStyleParentRefs + compileTimeAttrRefs
    val allRuntimeRefs: Set<AndroidResSource.ResRef> = runtimeStyleParentRefs + runtimeAttrRefs

    val usesForCompileTime = if (allCompileTimeRefs.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes.resRefs(allCompileTimeRefs)
      true
    } else {
      false
    }

    // TODO(tsr): fix reason
    val usesForRuntime = if (allRuntimeRefs.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes.resRefs(allRuntimeRefs)
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
    val candidateImports = capability.inlineMembers.asSequence()
      .flatMap { (pn, names) ->
        listOf("$pn.*") + names.map { name -> "$pn.$name" }
      }
      .toSet()

    val imports = context.project.imports.asSequence().filter { import ->
      candidateImports.contains(import)
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Inline(imports)
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
    reportBuilder
  ).usesAnnotationProcessor(context)
}

private class AnnotationProcessorDetector(
  private val coordinates: Coordinates,
  private val supportedTypes: Set<String>,
  private val isKaptApplied: Boolean,
  private val reportBuilder: DependencyTraceReport.Builder,
) {

  // convert ["lombok.*"] to [lombok.(package) regex]
  private val stars = supportedTypes
    .filter { it.endsWith("*") }
    .map { it.replace(".", "\\.") }
    .map { it.replace("*", JAVA_SUB_PACKAGE) }
    .map { it.toRegex(setOf(RegexOption.IGNORE_CASE)) }

  fun usesAnnotationProcessor(context: GraphViewVisitor.Context): Boolean {
    return (context.project.usedByImport() || context.project.usedByClass()).also {
      if (!it) reason(Reason.Unused)
    }
  }

  private fun ProjectVariant.usedByImport(): Boolean {
    val usedImports = mutableSetOf<String>()
    for (import in imports) {
      if (supportedTypes.contains(import) || stars.any { it.matches(import) }) {
        usedImports.add(import)
      }
    }

    return if (usedImports.isNotEmpty()) {
      reason(Reason.AnnotationProcessor.imports(usedImports, isKaptApplied))
      true
    } else {
      false
    }
  }

  private fun ProjectVariant.usedByClass(): Boolean {
    val theUsedClasses = mutableSetOf<String>()
    for (clazz in (usedNonAnnotationClasses + usedAnnotationClassesBySrc)) {
      if (supportedTypes.contains(clazz) || stars.any { it.matches(clazz) }) {
        theUsedClasses.add(clazz)
      }
    }

    return if (theUsedClasses.isNotEmpty()) {
      reason(Reason.AnnotationProcessor.classes(theUsedClasses, isKaptApplied))
      true
    } else {
      false
    }
  }

  private fun reason(reason: Reason) {
    reportBuilder[coordinates, Kind.ANNOTATION_PROCESSOR] = reason
  }
}
