@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.*
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.instrumentation.InstrumentationBuildService
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class ConstantUsageDetectionTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of constants, from other components, that have been used"
  }

  /**
   * Upstream artifacts.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val artifacts: RegularFileProperty

  /**
   * All the imports in the Java and Kotlin source in this project.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val imports: RegularFileProperty

  /**
   * A [`Set<Dependency>`][Dependency] of dependencies that provide constants that the current project is using.
   */
  @get:OutputFile
  abstract val constantUsageReport: RegularFileProperty

  @get:Internal
  abstract val instrumentationProvider: Property<InstrumentationBuildService>

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(ConstantUsageDetectionWorkAction::class.java) {
      artifacts.set(this@ConstantUsageDetectionTask.artifacts)
      importsFile.set(this@ConstantUsageDetectionTask.imports)
      constantUsageReport.set(this@ConstantUsageDetectionTask.constantUsageReport)
      instrumentationProvider.set(this@ConstantUsageDetectionTask.instrumentationProvider)
    }
  }
}

interface ConstantUsageDetectionParameters : WorkParameters {
  val artifacts: RegularFileProperty
  val importsFile: RegularFileProperty
  val constantUsageReport: RegularFileProperty
  val instrumentationProvider: Property<InstrumentationBuildService>
}

abstract class ConstantUsageDetectionWorkAction : WorkAction<ConstantUsageDetectionParameters> {

  private val logger = getLogger<ConstantUsageDetectionTask>()

  override fun execute() {
    // Output
    val constantUsageReportFile = parameters.constantUsageReport.get().asFile
    constantUsageReportFile.delete()

    // Inputs
    val artifacts = parameters.artifacts.get().asFile.readText().fromJsonList<Artifact>()
    val imports = parameters.importsFile.get().asFile.readText().fromJsonList<Imports>().flatten()

    val usedComponents = JvmConstantDetector(parameters.instrumentationProvider, logger, artifacts, imports).find()

    logger.debug("Constants usage:\n${usedComponents.toPrettyString()}")
    constantUsageReportFile.writeText(usedComponents.toJson())
  }

  // The constant detector doesn't care about source type
  private fun List<Imports>.flatten(): Set<String> = flatMap { it.imports }.toSortedSet()
}

/*
 * TODO@tsr all this stuff below looks very similar to InlineMemberExtractionTask
 */

internal class JvmConstantDetector(
  private val instrumentationProvider: Property<InstrumentationBuildService>,
  private val logger: Logger,
  private val artifacts: List<Artifact>,
  private val actualImports: Set<String>
) {

  fun find(): Set<Dependency> {
    val constantImportCandidates: Set<ComponentWithConstantMembers> = findConstantImportCandidates()
    return findUsedConstantImports(constantImportCandidates)
  }

  // from the upstream bytecode. Therefore "candidates" (not necessarily used)
  private fun findConstantImportCandidates(): Set<ComponentWithConstantMembers> {
    return artifacts
      .map { artifact ->
        artifact to JvmConstantMemberFinder(instrumentationProvider, logger, ZipFile(artifact.file)).find()
      }.filterNot { (_, imports) ->
        imports.isEmpty()
      }.map { (artifact, imports) ->
        ComponentWithConstantMembers(artifact.dependency, imports)
      }.toSortedSet()
  }

  private fun findUsedConstantImports(
    constantImportCandidates: Set<ComponentWithConstantMembers>
  ): Set<Dependency> {
    return actualImports.flatMap { actualImport ->
      findUsedConstantImports(actualImport, constantImportCandidates)
    }.toSortedSet()
  }

  /**
   * [actualImport] is, e.g.,
   * * `com.myapp.BuildConfig.DEBUG`
   * * `com.myapp.BuildConfig.*`
   */
  private fun findUsedConstantImports(
    actualImport: String, constantImportCandidates: Set<ComponentWithConstantMembers>
  ): List<Dependency> {
    // TODO@tsr it's a little disturbing there can be multiple matches. An issue with this naive algorithm.
    // TODO@tsr I need to be more intelligent in source parsing. Look at actual identifiers being used and associate those with their star-imports
    return constantImportCandidates.filter {
      it.imports.contains(actualImport)
    }.map {
      it.dependency
    }
  }
}

/**
 * Parses bytecode looking for constant declarations.
 */
private class JvmConstantMemberFinder(
  instrumentationProvider: Property<InstrumentationBuildService>,
  private val logger: Logger,
  private val zipFile: ZipFile
) {

  private val instrumentation = instrumentationProvider.get()

  /**
   * Returns either an empty list, if there are no constants, or a list of import candidates. E.g.:
   * ```
   * [
   *   "com.myapp.BuildConfig.*",
   *   "com.myapp.BuildConfig.DEBUG"
   * ]
   * ```
   * An import statement with either of those would import the `com.myapp.BuildConfig.DEBUG` constant, contributed by
   * the "com.myapp" module.
   */
  fun find(): Set<String> {
    val alreadyFoundConstantMembers: Set<String>? = instrumentation.constantMembers[zipFile.name]
    if (alreadyFoundConstantMembers != null) {
      return alreadyFoundConstantMembers
    }

    val entries = zipFile.entries().toList()

    return entries
      .filter { it.name.endsWith(".class") }
      .flatMap { entry ->
        val classReader = zipFile.getInputStream(entry).use { ClassReader(it.readBytes()) }
        val constantVisitor = ConstantVisitor(logger)
        classReader.accept(constantVisitor, 0)

        val fqcn = constantVisitor.className
          .replace("/", ".")
          .replace("$", ".")
        val constantMembers = constantVisitor.classes

        if (constantMembers.isNotEmpty()) {
          listOf(
            // import com.myapp.BuildConfig -> BuildConfig.DEBUG
            fqcn,
            // import com.myapp.BuildConfig.* -> DEBUG
            "$fqcn.*"
          ) +
            // import com.myapp.* -> /* Kotlin file with top-level const val declarations */
            optionalStarImport(fqcn) +
            constantMembers.map { name -> "$fqcn.$name" }
        } else {
          emptyList()
        }
      }.toSortedSet().also {
        instrumentation.constantMembers.putIfAbsent(zipFile.name, it)
      }
  }

  private fun optionalStarImport(fqcn: String): List<String> {
    return if (fqcn.contains(".")) {
      // "fqcn" is not in a package, and so contains no dots
      listOf("${fqcn.substring(0, fqcn.lastIndexOf("."))}.*")
    } else {
      // a star import makes no sense in this context
      emptyList()
    }
  }
}

/*
 * TODO@tsr some thoughts on an improved algo:
 * Need a data structure that includes the following import patterns from providers:
 * 1. com.myapp.MyClass                // Import of class containing constant thing -> MyClass.CONSTANT_THING
 * 2. com.myapp.MyClass.CONSTANT_THING // Direct import of constant thing -> CONSTANT_THING
 * 3. com.myapp.MyClass.*              // Star-import of all constant things in MyClass -> CONSTANT_THING_1, CONSTANT_THING_2
 * 4. com.myapp.*                      // Kotlin top-level declarations in com.myapp package -> CONSTANT_THING
 *
 * 3 and 4 (mostly 4) are problematic because they results in non-uniquely identifiable component providers of
 * constants.
 *
 * If, on the consumer side, I see one of those import patterns, I could also look for `SimpleIdentifier`s and associate
 * those with constant things provided by the providers. My data structure would need the addition of simple identifiers
 * for each class/package.
 */
