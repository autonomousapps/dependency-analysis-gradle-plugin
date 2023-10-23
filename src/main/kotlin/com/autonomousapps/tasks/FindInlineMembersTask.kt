@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.KotlinMetadataVisitor
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.InlineMemberCapability
import com.autonomousapps.model.PhysicalArtifact
import com.autonomousapps.model.intermediates.InlineMemberDependency
import com.autonomousapps.services.InMemoryCache
import kotlinx.metadata.Flag
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class FindInlineMembersTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of dependencies that contribute used inline members"
  }

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  /** Not used by the task action, but necessary for correct input-output tracking, for reasons I do not recall. */
  @get:Classpath
  abstract val compileClasspath: ConfigurableFileCollection

  /** [PhysicalArtifact]s used to compile this project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val artifacts: RegularFileProperty

  /** Inline members in this project's dependencies. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(FindInlineMembersWorkAction::class.java) {
      artifacts.set(this@FindInlineMembersTask.artifacts)
      inlineUsageReport.set(this@FindInlineMembersTask.output)
      inMemoryCacheProvider.set(this@FindInlineMembersTask.inMemoryCacheProvider)
    }
  }

  interface FindInlineMembersParameters : WorkParameters {
    val artifacts: RegularFileProperty
    val inlineUsageReport: RegularFileProperty
    val inMemoryCacheProvider: Property<InMemoryCache>
  }

  abstract class FindInlineMembersWorkAction : WorkAction<FindInlineMembersParameters> {

    private val logger = getLogger<FindInlineMembersTask>()

    override fun execute() {
      val inlineUsageReportFile = parameters.inlineUsageReport.getAndDelete()

      val artifacts = parameters.artifacts.fromJsonList<PhysicalArtifact>()

      val inlineMembers = InlineMembersFinder(
        inMemoryCache = parameters.inMemoryCacheProvider.get(),
        artifacts = artifacts
      ).find()

      logger.debug("Inline usage:\n${inlineMembers.toPrettyString()}")
      inlineUsageReportFile.bufferWriteJsonSet(inlineMembers)
    }
  }
}

internal class InlineMembersFinder(
  private val inMemoryCache: InMemoryCache,
  private val artifacts: List<PhysicalArtifact>,
) {

  private val logger = getLogger<FindInlineMembersTask>()

  fun find(): Set<InlineMemberDependency> = artifacts.asSequence()
    .filter {
      it.file.name.endsWith(".jar")
    }.map { artifact ->
      artifact to findInlineMembers(artifact)
    }.filterNot { (_, inlineMembers) ->
      inlineMembers.isEmpty()
    }.map { (artifact, inlineMembers) ->
      InlineMemberDependency(artifact.coordinates, inlineMembers)
    }.toSortedSet()

  /**
   * Returns either an empty set, if there are no inline members, or a set of [InlineMemberCapability.InlineMember]s
   * (import candidates). E.g.:
   * ```
   * [
   *   "kotlin.jdk7.*",
   *   "kotlin.jdk7.use"
   * ]
   * ```
   * An import statement with either of those would import the `kotlin.jdk7.use()` inline function, contributed by the
   * "org.jetbrains.kotlin:kotlin-stdlib-jdk7" module.
   */
  private fun findInlineMembers(artifact: PhysicalArtifact): Set<InlineMemberCapability.InlineMember> {
    val alreadyFoundInlineMembers = inMemoryCache.inlineMember(artifact.file.absolutePath)
    if (alreadyFoundInlineMembers != null) {
      return alreadyFoundInlineMembers
    }

    val zipFile = ZipFile(artifact.file)
    val entries = zipFile.entries().toList()
    // Only look at jars that have actual Kotlin classes in them
    if (entries.none { it.name.endsWith(".kotlin_module") }) {
      return emptySet()
    }

    return entries.asSequenceOfClassFiles()
      .mapNotNull { entry ->
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
              emptySet()
            }

            is KotlinClassMetadata.MultiFileClassFacade -> {
              logger.debug("Ignoring MultiFileClassFacade $entry")
              emptySet()
            }

            is KotlinClassMetadata.Unknown -> {
              logger.debug("Ignoring Unknown $entry")
              emptySet()
            }

            null -> {
              logger.debug("Ignoring null $entry")
              emptySet()
            }
          }
        } ?: emptySet()

        // return early if no members found
        if (inlineMembers.isEmpty()) return@mapNotNull null

        val pn = if (entry.name.contains('/')) {
          // entry is in a package
          entry.name.substringBeforeLast('/').replace('/', '.')
        } else {
          // entry is in root; no package
          ""
        }

        // return non-empty members
        InlineMemberCapability.InlineMember(
          packageName = pn,
          // Guaranteed to be non-empty
          inlineMembers = inlineMembers
        )
      }.toSortedSet()
      .also {
        inMemoryCache.inlineMembers(artifact.file.absolutePath, it)
      }
  }

  private fun inlineMembers(kmDeclaration: KmDeclarationContainer): Set<String> {
    return (inlineFunctions(kmDeclaration.functions) + inlineProperties(kmDeclaration.properties)).toSortedSet()
  }

  private fun inlineFunctions(functions: List<KmFunction>): Sequence<String> {
    return functions.asSequence()
      .filter { Flag.Function.IS_INLINE(it.flags) }
      .map { it.name }
  }

  private fun inlineProperties(properties: List<KmProperty>): Sequence<String> {
    return properties.asSequence()
      .filter { Flag.PropertyAccessor.IS_INLINE(it.flags) }
      .map { it.name }
  }
}
