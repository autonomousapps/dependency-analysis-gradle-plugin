package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.intermediates.DependencyTraceReport
import com.autonomousapps.model.intermediates.DependencyTraceReport.Kind
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.visitor.GraphViewReader
import com.autonomousapps.visitor.GraphViewVisitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class ComputeUsagesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
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

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ComputeUsagesAction::class.java) {
      graph.set(this@ComputeUsagesTask.graph)
      declarations.set(this@ComputeUsagesTask.declarations)
      dependencies.set(this@ComputeUsagesTask.dependencies)
      syntheticProject.set(this@ComputeUsagesTask.syntheticProject)
      kapt.set(this@ComputeUsagesTask.kapt)
      output.set(this@ComputeUsagesTask.output)
    }
  }

  interface ComputeUsagesParameters : WorkParameters {
    val graph: RegularFileProperty
    val declarations: RegularFileProperty
    val dependencies: DirectoryProperty
    val syntheticProject: RegularFileProperty
    val kapt: Property<Boolean>
    val output: RegularFileProperty
  }

  abstract class ComputeUsagesAction : WorkAction<ComputeUsagesParameters> {

    private val graph = parameters.graph.fromJson<DependencyGraphView>()
    private val declarations = parameters.declarations.fromJsonSet<Declaration>()
    private val project = parameters.syntheticProject.fromJson<ProjectVariant>()
    private val dependencies = project.dependencies(parameters.dependencies.get())

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reader = GraphViewReader(
        project = project,
        dependencies = dependencies,
        graph = graph,
        declarations = declarations
      )
      val visitor = GraphVisitor(project, parameters.kapt.get())
      reader.accept(visitor)

      val report = visitor.report
      output.writeText(report.toJson())
    }
  }
}

private class GraphVisitor(
  project: ProjectVariant,
  private val kapt: Boolean
) : GraphViewVisitor {

  val report: DependencyTraceReport get() = reportBuilder.build()

  private val reportBuilder = DependencyTraceReport.Builder(
    buildType = project.buildType,
    flavor = project.flavor,
    variant = project.variant
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
    var isRuntimeAndroid = false
    var usesResBySource = false
    var usesResByRes = false
    var usesAssets = false
    var usesConstant = false
    var usesInlineMember = false
    var hasServiceLoader = false
    var hasSecurityProvider = false
    var hasNativeLib = false

    dependency.capabilities.values.forEach { capability ->
      @Suppress("UNUSED_VARIABLE") // exhaustive when
      val ignored: Any = when (capability) {
        is AndroidLinterCapability -> {
          isLintJar = capability.isLintJar
          reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Reason.LintJar.of(capability.lintRegistry)
        }
        is AndroidManifestCapability -> isRuntimeAndroid = isRuntimeAndroid(dependencyCoordinates, capability)
        is AndroidAssetCapability -> usesAssets = usesAssets(dependencyCoordinates, capability, context)
        is AndroidResCapability -> {
          usesResBySource = usesResBySource(dependencyCoordinates, capability, context)
          usesResByRes = usesResByRes(dependencyCoordinates, capability, context)
        }
        is AnnotationProcessorCapability -> {
          isAnnotationProcessor = true
          isAnnotationProcessorCandidate = usesAnnotationProcessor(dependencyCoordinates, capability, context)
        }
        is ClassCapability -> {
          if (isAbi(dependencyCoordinates, capability, context)) {
            isApiCandidate = true
          } else if (isImplementation(dependencyCoordinates, capability, context)) {
            isImplCandidate = true
          } else if (isImported(dependencyCoordinates, capability, context)) {
            isImplByImportCandidate = true
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
    // An example are KMP facades that resolve to -jvm artifacts
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

    if (isApiCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.API
    } else if (isCompileOnlyCandidate) {
      // TODO compileOnlyApi? Only relevant for java-library projects
      // compileOnly candidates are not also unused candidates. Some annotations are not detectable by bytecode
      // analysis (SOURCE retention), and possibly not by source parsing either (they could be in the same package), so
      // we don't suggest removing such dependencies.
      isUnusedCandidate = false
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
    } else if (isImplCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
    } else if (isImplByImportCandidate) {
      reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
    }

    if (isUnusedCandidate) {
      // These weren't detected by direct presence in bytecode, but (in some cases) via source analysis. We can say less
      // about them, so we dump them into `implementation` or `runtimeOnly`.
      when {
        usesResBySource -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesResByRes -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesConstant -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        usesInlineMember -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.IMPL
        isLintJar -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
        isRuntimeAndroid -> reportBuilder[dependencyCoordinates, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
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

  private fun isRuntimeAndroid(coordinates: Coordinates, capability: AndroidManifestCapability): Boolean {
    val components = capability.componentMap
    val services = components[AndroidManifestCapability.Component.SERVICE]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.services(it)
    }
    val providers = components[AndroidManifestCapability.Component.PROVIDER]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid.providers(it)
    }
    // If we considered any component to be sufficient, then we'd be super over-aggressive regarding whether an Android
    // library was used.
    return services != null || providers != null
  }

  private fun isAbi(
    coordinates: Coordinates,
    classCapability: ClassCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    val exposedClasses = context.project.exposedClasses.asSequence().filter { exposedClass ->
      classCapability.classes.contains(exposedClass)
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
    classCapability: ClassCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    val implClasses = context.project.implementationClasses.asSequence().filter { implClass ->
      classCapability.classes.contains(implClass)
    }.toSortedSet()

    return if (implClasses.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Impl(implClasses)
      true
    } else {
      false
    }
  }

  private fun isImported(
    coordinates: Coordinates,
    classCapability: ClassCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    val imports = context.project.imports.asSequence().filter { import ->
      classCapability.classes.contains(import)
    }.toSortedSet()

    return if (imports.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Imported(imports)
      true
    } else {
      false
    }
  }

  private fun usesConstant(
    coordinates: Coordinates,
    capability: ConstantCapability,
    context: GraphViewVisitor.Context
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

    val ktFiles = capability.ktFiles
    val candidateImports = capability.constants.asSequence()
      .flatMap { (fqcn, names) ->
        val ktPrefix = ktFiles.find {
          it.fqcn == fqcn
        }?.name?.let { name ->
          fqcn.removeSuffix(name)
        }
        val ktImports = names.mapNotNull { name -> ktPrefix?.let { "$it$name" } }

        ktImports + listOf("$fqcn.*") + optionalStarImport(fqcn) + names.map { name -> "$fqcn.$name" }
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
    context: GraphViewVisitor.Context
  ): Boolean = (capability.assets.isNotEmpty()
    && context.project.usedClassesBySrc.contains("android.content.res.AssetManager")
    ).andIfTrue {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Asset(capability.assets)
    }

  private fun usesResBySource(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context
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

  private fun usesResByRes(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    val styleParentRefs = mutableSetOf<AndroidResSource.StyleParentRef>()
    val attrRefs = mutableSetOf<AndroidResSource.AttrRef>()

    for ((type, id) in capability.lines) {
      for (candidate in context.project.androidResSource) {
        candidate.styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }?.let { styleParentRefs.add(it) }

        candidate.attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }?.let { attrRefs.add(it) }
      }
    }

    var used = if (styleParentRefs.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes.styleParentRefs(styleParentRefs)
      true
    } else {
      false
    }

    used = used || if (attrRefs.isNotEmpty()) {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes.attrRefs(attrRefs)
      true
    } else {
      false
    }

    return used
  }

  private fun usesInlineMember(
    coordinates: Coordinates,
    capability: InlineMemberCapability,
    context: GraphViewVisitor.Context
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
    context: GraphViewVisitor.Context
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
  private val reportBuilder: DependencyTraceReport.Builder
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
    val usedAnnotationClasses = mutableSetOf<String>()
    for (clazz in usedClasses) {
      if (supportedTypes.contains(clazz) || stars.any { it.matches(clazz) }) {
        usedAnnotationClasses.add(clazz)
      }
    }

    return if (usedAnnotationClasses.isNotEmpty()) {
      reason(Reason.AnnotationProcessor.classes(usedAnnotationClasses, isKaptApplied))
      true
    } else {
      false
    }
  }

  private fun reason(reason: Reason) {
    reportBuilder[coordinates, Kind.ANNOTATION_PROCESSOR] = reason
  }
}
