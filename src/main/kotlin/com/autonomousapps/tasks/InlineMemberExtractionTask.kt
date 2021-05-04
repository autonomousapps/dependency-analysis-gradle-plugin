@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import kotlinx.metadata.Flag
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.KotlinClassMetadata
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

/**
 * Algorithm:
 * 1. Parses the bytecode of all dependencies looking for inline members (functions or properties), and producing a
 *    report that associates these dependencies (see [Dependency]) with a set of imports
 *    ([ComponentWithInlineMembers.imports]) that would indicate use of an inline member. It is a best-guess heuristic.
 *    (So, `inline fun SpannableStringBuilder.bold()` gets associated with `androidx.core.text.bold` in the core-ktx
 *    module.)
 * 2. Parse all Kotlin source looking for imports that might be associated with an inline function
 * 3. Connect 1 and 2.
 */
@CacheableTask
abstract class InlineMemberExtractionTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of dependencies that contribute used inline members"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val artifacts: RegularFileProperty

  /**
   * All the imports in the Kotlin source in this project.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val imports: RegularFileProperty

  @get:OutputFile
  abstract val inlineUsageReport: RegularFileProperty

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(InlineMemberExtractionWorkAction::class.java) {
      artifacts.set(this@InlineMemberExtractionTask.artifacts)
      imports.set(this@InlineMemberExtractionTask.imports)
      inlineUsageReport.set(this@InlineMemberExtractionTask.inlineUsageReport)
      inMemoryCacheProvider.set(this@InlineMemberExtractionTask.inMemoryCacheProvider)
    }
  }
}

interface InlineMemberExtractionParameters : WorkParameters {
  val artifacts: RegularFileProperty
  val imports: RegularFileProperty
  val inlineUsageReport: RegularFileProperty
  val inMemoryCacheProvider: Property<InMemoryCache>
}

abstract class InlineMemberExtractionWorkAction : WorkAction<InlineMemberExtractionParameters> {

  private val logger = getLogger<InlineMemberExtractionTask>()

  override fun execute() {
    // Outputs
    val inlineUsageReportFile = parameters.inlineUsageReport.getAndDelete()

    // Inputs
    val artifacts = parameters.artifacts.fromJsonList<Artifact>()
    val imports = parameters.imports.fromJsonList<Imports>().kotlinImports()

    // In principle, there may not be any Kotlin source, although a current implementation detail is
    // that "imports" will never be null, only empty. Making this null-safe seems harmless enough,
    // however.
    val usedComponents = imports?.let {
      InlineDependenciesFinder(parameters.inMemoryCacheProvider, logger, artifacts, it).find()
    } ?: emptySet()

    logger.debug("Inline usage:\n${usedComponents.toPrettyString()}")
    inlineUsageReportFile.writeText(usedComponents.toJson())
  }

  private fun List<Imports>.kotlinImports(): Set<String>? = find {
    it.sourceType == SourceType.KOTLIN
  }?.imports
}

internal class InlineDependenciesFinder(
  private val inMemoryCacheProvider: Property<InMemoryCache>,
  private val logger: Logger,
  private val artifacts: List<Artifact>,
  private val actualImports: Set<String>
) {

  /**
   * Returns a set of [Dependency]s that contribute used inline members in the current project (as indicated by
   * [actualImports]).
   */
  fun find(): Set<Dependency> {
    val inlineImportsCandidates: Set<ComponentWithInlineMembers> = findInlineImportCandidates()
    // This is not needed except as a manual diagnostic
    //inlineMembersReportFile.writeText(inlineImports.toPrettyString())
    return findUsedInlineImports(inlineImportsCandidates)
  }

  // from the upstream bytecode. Therefore "candidates" (not necessarily used)
  private fun findInlineImportCandidates(): Set<ComponentWithInlineMembers> {
    return artifacts
      .filter {
        it.file.name.endsWith(".jar")
      }.map { artifact ->
        artifact to InlineMemberFinder(inMemoryCacheProvider, logger, ZipFile(artifact.file)).find().toSortedSet()
      }.filterNot { (_, imports) ->
        imports.isEmpty()
      }.mapToOrderedSet { (artifact, imports) ->
        ComponentWithInlineMembers(artifact.dependency, imports)
      }
  }

  private fun findUsedInlineImports(
    inlineImportCandidates: Set<ComponentWithInlineMembers>
  ): Set<Dependency> {
    return actualImports.flatMapToOrderedSet { actualImport ->
      findUsedInlineImports(actualImport, inlineImportCandidates)
    }
  }

  /**
   * [actualImport] is, e.g.,
   * * `com.myapp.BuildConfig.DEBUG`
   * * `com.myapp.BuildConfig.*`
   */
  private fun findUsedInlineImports(
    actualImport: String, constantImportCandidates: Set<ComponentWithInlineMembers>
  ): List<Dependency> {
    // TODO it's a little disturbing there can be multiple matches. An issue with this naive algorithm.
    // TODO I need to be more intelligent in source parsing. Look at actual identifiers being used and associate those with their star-imports
    return constantImportCandidates.filter {
      it.imports.contains(actualImport)
    }.map {
      it.dependency
    }
  }
}

/**
 * Use to find inline members (functions or properties).
 */
internal class InlineMemberFinder(
  private val inMemoryCacheProvider: Property<out InMemoryCache>,
  private val logger: Logger,
  private val zipFile: ZipFile
) {

  /**
   * Returns either an empty list, if there are no inline members, or a list of import candidates. E.g.:
   * ```
   * [
   *   "kotlin.jdk7.*",
   *   "kotlin.jdk7.use"
   * ]
   * ```
   * An import statement with either of those would import the `kotlin.jdk7.use()` inline function, contributed by the
   * "org.jetbrains.kotlin:kotlin-stdlib-jdk7" module.
   */
  fun find(): List<String> {
    val inMemoryCache = inMemoryCacheProvider.get()
    val alreadyFoundInlineMembers: List<String>? = inMemoryCache.inlineMember(zipFile.name)
    if (alreadyFoundInlineMembers != null) {
      return alreadyFoundInlineMembers
    }

    val entries = zipFile.entries().toList()
    // Only look at jars that have actual Kotlin classes in them
    if (entries.none { it.name.endsWith(".kotlin_module") }) {
      return emptyList()
    }

    return entries.filterToSetOfClassFiles()
      .flatMap { entry ->
        // TODO an entry with `META-INF/proguard/androidx-annotations.pro`
        val classReader = zipFile.getInputStream(entry).use { ClassReader(it.readBytes()) }
        val metadataVisitor = KotlinMetadataVisitor(logger)
        classReader.accept(metadataVisitor, 0)

        val inlineMembers = metadataVisitor.builder?.let { header ->
          when (val metadata = KotlinClassMetadata.read(header.build())) {
            is KotlinClassMetadata.Class -> inlineMembers(metadata.toKmClass())
            is KotlinClassMetadata.FileFacade -> inlineMembers(metadata.toKmPackage())
            is KotlinClassMetadata.MultiFileClassPart -> inlineMembers(metadata.toKmPackage())
            is KotlinClassMetadata.SyntheticClass -> {
              logger.debug("Ignoring SyntheticClass $entry")
              emptyList()
            }
            is KotlinClassMetadata.MultiFileClassFacade -> {
              logger.debug("Ignoring MultiFileClassFacade $entry")
              emptyList()
            }
            is KotlinClassMetadata.Unknown -> {
              logger.debug("Ignoring Unknown $entry")
              emptyList()
            }
            null -> {
              logger.debug("Ignoring null $entry")
              emptyList()
            }
          }
        } ?: emptyList()

        if (inlineMembers.isNotEmpty()) {
          if (entry.name.contains("/")) {
            // entry is in a package
            val pn = entry.name.substring(0, entry.name.lastIndexOf("/")).replace("/", ".")
            listOf("$pn.*") + inlineMembers.map { name -> "$pn.$name" }
          } else {
            // entry is in root; no package
            inlineMembers
          }
        } else {
          emptyList()
        }
      }.also {
        inMemoryCache.inlineMembers(zipFile.name, it)
      }
  }

  private fun inlineMembers(kmDeclaration: KmDeclarationContainer): List<String> {
    return inlineFunctions(kmDeclaration.functions) + inlineProperties(kmDeclaration.properties)
  }

  private fun inlineFunctions(functions: List<KmFunction>): List<String> {
    return functions
      .filter { Flag.Function.IS_INLINE(it.flags) }
      .map { it.name }
  }

  private fun inlineProperties(properties: List<KmProperty>): List<String> {
    return properties
      .filter { Flag.PropertyAccessor.IS_INLINE(it.flags) }
      .map { it.name }
  }
}
