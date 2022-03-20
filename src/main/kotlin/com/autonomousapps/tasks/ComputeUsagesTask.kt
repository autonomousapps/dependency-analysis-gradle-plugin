package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.intermediates.Bucket
import com.autonomousapps.model.intermediates.Declaration
import com.autonomousapps.model.intermediates.DependencyTraceReport
import com.autonomousapps.model.intermediates.DependencyTraceReport.Kind
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.visitor.GraphViewReader
import com.autonomousapps.visitor.GraphViewVisitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
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
  abstract val locations: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  abstract val dependencies: DirectoryProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val syntheticProject: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ComputeUsagesAction::class.java) {
      graph.set(this@ComputeUsagesTask.graph)
      locations.set(this@ComputeUsagesTask.locations)
      dependencies.set(this@ComputeUsagesTask.dependencies)
      syntheticProject.set(this@ComputeUsagesTask.syntheticProject)
      output.set(this@ComputeUsagesTask.output)
    }
  }

  interface ComputeUsagesParameters : WorkParameters {
    val graph: RegularFileProperty
    val locations: RegularFileProperty
    val dependencies: DirectoryProperty
    val syntheticProject: RegularFileProperty
    val output: RegularFileProperty
  }

  abstract class ComputeUsagesAction : WorkAction<ComputeUsagesParameters> {

    private val dependenciesDir = parameters.dependencies.get()
    private val graph = parameters.graph.fromJson<DependencyGraphView>()
    private val declarations = parameters.locations.fromJsonSet<Declaration>()
    private val project = parameters.syntheticProject.fromJson<ProjectVariant>()

    private val dependencies = project.classpath.asSequence()
      .plus(project.annotationProcessors)
      .map(::getDependency)
      .toSet()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reader = GraphViewReader(
        project = project,
        dependencies = dependencies,
        graph = graph,
        declarations = declarations
      )
      val visitor = GraphVisitor(project)
      reader.accept(visitor)

      val report = visitor.report
      output.writeText(report.toJson())
    }

    private fun getDependency(coordinates: Coordinates): Dependency {
      val file = dependenciesDir.file(coordinates.toFileName())
      return if (file.asFile.exists()) {
        file.fromJson()
      } else {
        error("No file for ${coordinates.gav()}")
      }
    }
  }
}

private class GraphVisitor(project: ProjectVariant) : GraphViewVisitor {

  val report: DependencyTraceReport get() = reportBuilder.build()

  private val reportBuilder = DependencyTraceReport.Builder(
    buildType = project.buildType,
    flavor = project.flavor,
    variant = project.variant
  )

  override fun visit(dependency: Dependency, context: GraphViewVisitor.Context) {
    val coord = dependency.coordinates
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
          reportBuilder[coord, Kind.DEPENDENCY] = Reason.LintJar("Is an Android linter")
        }
        is AndroidManifestCapability -> isRuntimeAndroid = isRuntimeAndroid(coord, capability)
        is AndroidResCapability -> {
          usesResBySource = usesResBySource(coord, capability, context)
          usesResByRes = usesResByRes(coord, capability, context)
        }
        is AnnotationProcessorCapability -> {
          isAnnotationProcessor = true
          isAnnotationProcessorCandidate = usesAnnotationProcessor(coord, capability, context)
        }
        is ClassCapability -> {
          if (isAbi(coord, capability, context)) {
            isApiCandidate = true
          } else if (isImplementation(coord, capability, context)) {
            isImplCandidate = true
          } else if (isImported(coord, capability, context)) {
            isImplByImportCandidate = true
          } else {
            isUnusedCandidate = true
          }
        }
        is ConstantCapability -> usesConstant = usesConstant(coord, capability, context)
        is InferredCapability -> {
          isCompileOnlyCandidate = capability.isCompileOnlyAnnotations
          reportBuilder[coord, Kind.DEPENDENCY] = Reason.CompileTimeAnnotations("Provides compile-time annotations")
        }
        is InlineMemberCapability -> usesInlineMember = usesInlineMember(coord, capability, context)
        is ServiceLoaderCapability -> {
          val providers = capability.providerClasses
          hasServiceLoader = providers.isNotEmpty()
          reportBuilder[coord, Kind.DEPENDENCY] = Reason.ServiceLoader("Provides service loaders: $providers")
        }
        is NativeLibCapability -> {
          val fileNames = capability.fileNames
          hasNativeLib = fileNames.isNotEmpty()
          reportBuilder[coord, Kind.DEPENDENCY] = Reason.NativeLib("Provides native binaries: $fileNames")
        }
        is SecurityProviderCapability -> {
          val providers = capability.securityProviders
          hasSecurityProvider = providers.isNotEmpty()
          reportBuilder[coord, Kind.DEPENDENCY] = Reason.SecurityProvider("Provides security providers: $providers")
        }
      }
    }

    // this is not mutually exclusive with other buckets. E.g., Lombok is both an annotation processor and a "normal"
    // dependency. See LombokSpec.
    if (isAnnotationProcessorCandidate) {
      reportBuilder[coord, Kind.ANNOTATION_PROCESSOR] = Bucket.ANNOTATION_PROCESSOR
    } else if (isAnnotationProcessor) {
      // unused annotation processor
      reportBuilder[coord, Kind.ANNOTATION_PROCESSOR] = Bucket.NONE
    }

    if (isApiCandidate) {
      reportBuilder[coord, Kind.DEPENDENCY] = Bucket.API
    } else if (isCompileOnlyCandidate) {
      // TODO compileOnlyApi? Only relevant for java-library projects
      // compileOnly candidates are not also unused candidates. Some annotations are not detectable by bytecode
      // analysis (SOURCE retention), and possibly not by source parsing either (they could be in the same package), so
      // we don't suggest removing such dependencies.
      isUnusedCandidate = false
      reportBuilder[coord, Kind.DEPENDENCY] = Bucket.COMPILE_ONLY
    } else if (isImplCandidate) {
      reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
    } else if (isImplByImportCandidate) {
      reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
    }

    if (isUnusedCandidate) {
      // These weren't detected by direct presence in bytecode, but via source analysis. We can say less about them, so
      // we dump them into `implementation` to be conservative.
      if (usesResBySource) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
      } else if (usesResByRes) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
      } else if (usesConstant) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
      } else if (usesInlineMember) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.IMPL
      } else if (isLintJar) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      } else if (isRuntimeAndroid) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      } else if (hasServiceLoader) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      } else if (hasSecurityProvider) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      } else if (hasNativeLib) {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.RUNTIME_ONLY
      } else {
        reportBuilder[coord, Kind.DEPENDENCY] = Bucket.NONE
        reportBuilder[coord, Kind.DEPENDENCY] = Reason.Unused
      }
    }
  }

  private fun isRuntimeAndroid(coordinates: Coordinates, capability: AndroidManifestCapability): Boolean {
    val components = capability.componentMap
    val services = components[AndroidManifestCapability.Component.SERVICE]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid("Provides Android Services: $it")
    }
    val providers = components[AndroidManifestCapability.Component.PROVIDER]?.also {
      reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.RuntimeAndroid("Provides Android Providers: $it")
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
    return context.project.exposedClasses.any { exposedClass ->
      classCapability.classes.contains(exposedClass).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Abi("Exposes class $exposedClass")
      }
    }
  }

  private fun isImplementation(
    coordinates: Coordinates,
    classCapability: ClassCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    return context.project.implementationClasses.any { implClass ->
      classCapability.classes.contains(implClass).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Impl("Uses class $implClass")
      }
    }
  }

  private fun isImported(
    coordinates: Coordinates,
    classCapability: ClassCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    return context.project.imports.any { import ->
      classCapability.classes.contains(import).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Imported("Declares import $import")
      }
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
      .toSet()

    return context.project.imports.any { import ->
      candidateImports.contains(import).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Constant("Imports constant $import")
      }
    }
  }

  private fun usesResBySource(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    val projectImports = context.project.imports
    return listOf(capability.rImport, capability.rImport.removeSuffix("R") + "*").any { import ->
      projectImports.contains(import).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResBySrc("Imports res $import")
      }
    }
  }

  private fun usesResByRes(
    coordinates: Coordinates,
    capability: AndroidResCapability,
    context: GraphViewVisitor.Context
  ): Boolean {
    return capability.lines.any { (type, id) ->
      context.project.androidResSource.any { candidate ->
        val styleParentRef = candidate.styleParentRefs.find { styleParentRef ->
          id == styleParentRef.styleParent
        }

        if (styleParentRef != null) {
          reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes("Uses res $styleParentRef")
          return true
        }

        val attrRef = candidate.attrRefs.find { attrRef ->
          type == attrRef.type && id == attrRef.id
        }

        if (attrRef != null) {
          reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.ResByRes("Uses res $attrRef")
          return true
        }

        false
      }
    }
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

    return context.project.imports.any { import ->
      candidateImports.contains(import).also {
        if (it) reportBuilder[coordinates, Kind.DEPENDENCY] = Reason.Inline("Imports inline member $import")
      }
    }
  }

  private fun usesAnnotationProcessor(
    coordinates: Coordinates,
    capability: AnnotationProcessorCapability,
    context: GraphViewVisitor.Context
  ): Boolean = AnnotationProcessorDetector(
    coordinates,
    capability.supportedAnnotationTypes,
    reportBuilder
  ).usesAnnotationProcessor(context)
}

private class AnnotationProcessorDetector(
  private val coordinates: Coordinates,
  private val supportedTypes: Set<String>,
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
    for (import in imports) {
      if (supportedTypes.contains(import) || stars.any { it.matches(import) }) {
        reason(Reason.AnnotationProcessor("Imports annotation $import"))
        return true
      }
    }
    return false
  }

  private fun ProjectVariant.usedByClass(): Boolean {
    for (clazz in usedClasses) {
      if (supportedTypes.contains(clazz) || stars.any { it.matches(clazz) }) {
        reason(Reason.AnnotationProcessor("Uses annotation $clazz"))
        return true
      }
    }
    return false
  }

  private fun reason(reason: Reason) {
    reportBuilder[coordinates, Kind.ANNOTATION_PROCESSOR] = reason
  }
}
