package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.intermediates.DependencyUsageReport
import com.autonomousapps.model.intermediates.DependencyUsageReport.Trace
import com.autonomousapps.model.intermediates.Location
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
    private val locations = parameters.locations.fromJsonSet<Location>()
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
        locations = locations
      )
      val visitor = GraphVisitor(project)
      reader.accept(visitor)

      output.writeText(visitor.report.toJson())
    }

    private fun getDependency(coordinates: Coordinates): Dependency {
      return dependenciesDir.file(coordinates.toFileName()).fromJson()
    }
  }
}

private class GraphVisitor(private val project: ProjectVariant) : GraphViewVisitor {

  val report: DependencyUsageReport
    get() = DependencyUsageReport(
      buildType = project.buildType,
      flavor = project.flavor,
      variant = project.variant,
      annotationProcessorDependencies = annotationProcessingDependencies.toTraces(),
      abiDependencies = apiDependencies.toTraces(),
      implDependencies = implDependencies.toTraces(),
      compileOnlyDependencies = compileOnlyDependencies.toTraces(),
      runtimeOnlyDependencies = runtimeOnlyDependencies.toTraces(),
      compileOnlyApiDependencies = compileOnlyApiDependencies.toTraces(),
      unusedDependencies = unusedDependencies.toTraces(),
    )

  /*
   * App projects don't have APIs. This is handled upstream by ensuring app projects don't attempt ABI analysis.
   * Test variants also don't have APIs. This is not yet handled at all.
   */

  private val annotationProcessingDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val apiDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val implDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val compileOnlyDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val runtimeOnlyDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val compileOnlyApiDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()
  private val unusedDependencies = mutableMapOf<Coordinates, MutableSet<Reason>>()

  override fun visit(dependency: Dependency, context: GraphViewVisitor.Context) {
    var isAnnotationProcessingCandidate = false
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

    // TODO this is a potential concurrency candidate
    dependency.capabilities.values.forEach { capability ->
      @Suppress("UNUSED_VARIABLE") // exhaustive when
      val ignored: Any = when (capability) {
        is AndroidLinterCapability -> isLintJar = capability.isLintJar
        is AndroidManifestCapability -> isRuntimeAndroid = isRuntimeAndroid(capability)
        is AndroidResCapability -> {
          usesResBySource = usesResBySource(capability, context)
          usesResByRes = usesResByRes(capability, context)
        }
        is AnnotationProcessorCapability -> {
          if (usesAnnotationProcessor(capability, context)) {
            isAnnotationProcessingCandidate = true
          } else {
            isUnusedCandidate = true
          }
          // isAnnotationProcessingCandidate = usesAnnotationProcessor(capability, context)
        }
        is ClassCapability -> {
          if (isAbi(capability, context)) {
            isApiCandidate = true
          } else if (isImplementation(capability, context)) {
            isImplCandidate = true
          } else if (isImported(capability, context)) {
            isImplByImportCandidate = true
          } else {
            isUnusedCandidate = true
          }
        }
        is ConstantCapability -> usesConstant = usesConstant(capability, context)
        is InferredCapability -> isCompileOnlyCandidate = capability.isCompileOnlyAnnotations
        is InlineMemberCapability -> usesInlineMember = usesInlineMember(capability, context)
        is ServiceLoaderCapability -> hasServiceLoader = capability.providerClasses.isNotEmpty()
        is NativeLibCapability -> hasNativeLib = capability.fileNames.isNotEmpty()
        is SecurityProviderCapability -> hasSecurityProvider = capability.securityProviders.isNotEmpty()
      }
    }

    if (isAnnotationProcessingCandidate) {
      annotationProcessingDependencies.add(dependency.coordinates, Reason.ANNOTATION_PROCESSOR)
    } else if (isApiCandidate) {
      apiDependencies.add(dependency.coordinates, Reason.ABI)
    } else if (isCompileOnlyCandidate && !isUnusedCandidate) {
      // TODO compileOnlyApi? Only relevant for java-library projects
      compileOnlyDependencies.add(dependency.coordinates, Reason.COMPILE_TIME_ANNOTATIONS)
    } else if (isImplCandidate) {
      implDependencies.add(dependency.coordinates, Reason.IMPL)
    } else if (isImplByImportCandidate) {
      implDependencies.add(dependency.coordinates, Reason.IMPORTED)
    }

    if (isUnusedCandidate) {
      // These weren't detected by direct presence in bytecode, but via source analysis. We can say less about them, so
      // we dump them into `implementation` to be conservative.
      if (usesResBySource) {
        implDependencies.add(dependency.coordinates, Reason.RES_BY_SRC)
      } else if (usesResByRes) {
        implDependencies.add(dependency.coordinates, Reason.RES_BY_RES)
      } else if (usesConstant) {
        implDependencies.add(dependency.coordinates, Reason.CONSTANT)
      } else if (usesInlineMember) {
        implDependencies.add(dependency.coordinates, Reason.INLINE)
      } else if (isLintJar) {
        runtimeOnlyDependencies.add(dependency.coordinates, Reason.LINT_JAR)
      } else if (isRuntimeAndroid) {
        runtimeOnlyDependencies.add(dependency.coordinates, Reason.RUNTIME_ANDROID)
      } else if (hasServiceLoader) {
        runtimeOnlyDependencies.add(dependency.coordinates, Reason.SERVICE_LOADER)
      } else if (hasSecurityProvider) {
        runtimeOnlyDependencies.add(dependency.coordinates, Reason.SECURITY_PROVIDER)
      } else if (hasNativeLib) {
        runtimeOnlyDependencies.add(dependency.coordinates, Reason.NATIVE_LIB)
      } else {
        unusedDependencies.add(dependency.coordinates, Reason.UNUSED)
      }
    }
  }

  private fun MutableMap<Coordinates, MutableSet<Reason>>.toTraces(): Set<Trace> {
    return mapToOrderedSet { (coord, reasons) ->
      Trace(coord, reasons)
    }
  }

  private fun MutableMap<Coordinates, MutableSet<Reason>>.add(coordinates: Coordinates, reason: Reason) {
    merge(coordinates, mutableSetOf(reason)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }

  private fun isRuntimeAndroid(capability: AndroidManifestCapability): Boolean {
    val components = capability.componentMap
    val services = components[AndroidManifestCapability.Component.SERVICE]
    val providers = components[AndroidManifestCapability.Component.PROVIDER]
    // If we considered any component to be sufficient, then we'd be super over-aggressive regarding whether an Android
    // library was used.
    return services != null || providers != null
  }

  private fun isAbi(classCapability: ClassCapability, context: GraphViewVisitor.Context): Boolean {
    return context.project.exposedClasses.any { exposedClass ->
      classCapability.classes.contains(exposedClass)
    }
  }

  private fun isImplementation(classCapability: ClassCapability, context: GraphViewVisitor.Context): Boolean {
    return context.project.implementationClasses.any { implClass ->
      classCapability.classes.contains(implClass)
    }
  }

  private fun isImported(classCapability: ClassCapability, context: GraphViewVisitor.Context): Boolean {
    return context.project.imports.any { import ->
      classCapability.classes.contains(import)
    }
  }

  private fun usesConstant(capability: ConstantCapability, context: GraphViewVisitor.Context): Boolean {
    val ktFiles = capability.ktFiles
    val candidateImports = capability.constants.asSequence()
      .flatMap { (fqcn, names) ->
        val ktPrefix = ktFiles.find {
          it.fqcn == fqcn
        }?.name?.let { name ->
          fqcn.removeSuffix(name)
        }
        val ktImports = names.mapNotNull { name -> ktPrefix?.let { "$it$name" } }

        ktImports + listOf("$fqcn.*") + names.map { name -> "$fqcn.$name" }
      }
      .toSet()

    return context.project.imports.any {
      candidateImports.contains(it)
    }
  }

  private fun usesResBySource(capability: AndroidResCapability, context: GraphViewVisitor.Context): Boolean {
    val projectImports = context.project.imports
    return listOf(capability.rImport, capability.rImport.removeSuffix("R") + "*").any {
      projectImports.contains(it)
    }
  }

  private fun usesResByRes(capability: AndroidResCapability, context: GraphViewVisitor.Context): Boolean {
    return capability.lines.any { (type, id) ->
      context.project.androidResSource.any { candidate ->
        val byStyleParentRef = candidate.styleParentRefs.any { styleParentRef ->
          id == styleParentRef.styleParent
        }
        val byAttrRef by unsafeLazy {
          candidate.attrRefs.any { attrRef ->
            type == attrRef.type && id == attrRef.id
          }
        }

        byStyleParentRef || byAttrRef
      }
    }
  }

  private fun usesInlineMember(capability: InlineMemberCapability, context: GraphViewVisitor.Context): Boolean {
    val candidateImports = capability.inlineMembers.asSequence()
      .flatMap { (pn, names) ->
        listOf("$pn.*") + names.map { name -> "$pn.$name" }
      }
      .toSet()
    return context.project.imports.any {
      candidateImports.contains(it)
    }
  }

  private fun usesAnnotationProcessor(
    capability: AnnotationProcessorCapability,
    context: GraphViewVisitor.Context
  ): Boolean = AnnotationProcessorDetector(capability.supportedAnnotationTypes).usesAnnotationProcessor(context)
}

private class AnnotationProcessorDetector(
  private val supportedTypes: Set<String>
) {

  // convert ["lombok.*"] to [lombok.(package) regex]
  private val stars = supportedTypes
    .filter { it.endsWith("*") }
    .map { it.replace(".", "\\.") }
    .map { it.replace("*", JAVA_SUB_PACKAGE) }
    .map { it.toRegex(setOf(RegexOption.IGNORE_CASE)) }

  fun usesAnnotationProcessor(context: GraphViewVisitor.Context): Boolean {
    return context.project.usedByImport() || context.project.usedByClass()
  }

  private fun ProjectVariant.usedByImport(): Boolean {
    for (import in imports) {
      if (supportedTypes.contains(import) || stars.any { it.matches(import) }) {
        return true
      }
    }
    return false
  }

  private fun ProjectVariant.usedByClass(): Boolean {
    for (clazz in usedClasses) {
      if (supportedTypes.contains(clazz) || stars.any { it.matches(clazz) }) {
        return true
      }
    }
    return false
  }
}
