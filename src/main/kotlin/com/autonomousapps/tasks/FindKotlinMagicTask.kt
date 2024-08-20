// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.KotlinMetadataVisitor
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.InlineMemberCapability
import com.autonomousapps.model.KtFile
import com.autonomousapps.model.PhysicalArtifact
import com.autonomousapps.model.PhysicalArtifact.Mode
import com.autonomousapps.model.TypealiasCapability
import com.autonomousapps.model.intermediates.InlineMemberDependency
import com.autonomousapps.model.intermediates.TypealiasDependency
import com.autonomousapps.services.InMemoryCache
import kotlinx.metadata.*
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class FindKotlinMagicTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
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
  abstract val outputInlineMembers: RegularFileProperty

  /** typealiases in this project's dependencies. */
  @get:OutputFile
  abstract val outputTypealiases: RegularFileProperty

  /**
   * Errors analyzing class files.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1035">Issue 1035</a>
   * @see <a href="https://youtrack.jetbrains.com/issue/KT-60870">KT-60870</a>
   */
  @get:OutputFile
  abstract val outputErrors: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(FindKotlinMagicWorkAction::class.java) {
      artifacts.set(this@FindKotlinMagicTask.artifacts)
      inlineUsageReport.set(this@FindKotlinMagicTask.outputInlineMembers)
      typealiasReport.set(this@FindKotlinMagicTask.outputTypealiases)
      errorsReport.set(this@FindKotlinMagicTask.outputErrors)
      inMemoryCacheProvider.set(this@FindKotlinMagicTask.inMemoryCacheProvider)
    }
  }

  interface FindKotlinMagicParameters : WorkParameters {
    val artifacts: RegularFileProperty
    val inlineUsageReport: RegularFileProperty
    val typealiasReport: RegularFileProperty
    val errorsReport: RegularFileProperty
    val inMemoryCacheProvider: Property<InMemoryCache>
  }

  abstract class FindKotlinMagicWorkAction : WorkAction<FindKotlinMagicParameters> {

    private val logger = getLogger<FindKotlinMagicTask>()

    override fun execute() {
      val inlineUsageReportFile = parameters.inlineUsageReport.getAndDelete()
      val typealiasReportFile = parameters.typealiasReport.getAndDelete()
      val errorsReport = parameters.errorsReport.getAndDelete()

      val finder = KotlinMagicFinder(
        inMemoryCache = parameters.inMemoryCacheProvider.get(),
        artifacts = parameters.artifacts.fromJsonList<PhysicalArtifact>(),
        errorsReport = errorsReport,
      )
      val inlineMembers = finder.inlineMembers
      val typealiases = finder.typealiases

      inlineUsageReportFile.bufferWriteJsonSet(inlineMembers)
      typealiasReportFile.bufferWriteJsonSet(typealiases)

      if (finder.didWriteErrors) {
        logger.warn("There were errors during inline member analysis. See ${errorsReport.toPath().toUri()}")
      } else {
        // This file must always exist, even if empty
        errorsReport.writeText("")
      }
    }
  }
}

internal class KotlinMagicFinder(
  private val inMemoryCache: InMemoryCache,
  artifacts: List<PhysicalArtifact>,
  private val errorsReport: File,
) {

  private val logger = getLogger<FindKotlinMagicTask>()
  var didWriteErrors = false

  val inlineMembers: Set<InlineMemberDependency>
  val typealiases: Set<TypealiasDependency>

  init {
    val inlineMembersMut = mutableSetOf<InlineMemberDependency>()
    val typealiasesMut = mutableSetOf<TypealiasDependency>()

    artifacts.asSequence()
      .filter {
        it.isJar() || it.containsClassFiles()
      }.map { artifact ->
        artifact to findKotlinMagic(artifact, artifact.mode)
      }.forEach { (artifact, capabilities) ->
        if (capabilities.inlineMembers.isNotEmpty()) {
          inlineMembersMut += InlineMemberDependency(artifact.coordinates, capabilities.inlineMembers)
        }
        if (capabilities.typealiases.isNotEmpty()) {
          typealiasesMut += TypealiasDependency(artifact.coordinates, capabilities.typealiases)
        }
      }

    inlineMembers = inlineMembersMut
    typealiases = typealiasesMut
  }

  // private fun analyzeDependencies(): Set<InlineMemberDependency> {
  //   val inlineMembers = mutableSetOf<InlineMemberDependency>()
  //   val typealiases = mutableSetOf<TypealiasDependency>()
  //
  //   artifacts.asSequence()
  //     .filter {
  //       it.isJar() || it.containsClassFiles()
  //     }.map { artifact ->
  //       artifact to findKotlinMagic(artifact, artifact.mode)
  //     }.forEach { (artifact, capabilities) ->
  //       if (capabilities.inlineMembers.isNotEmpty()) {
  //         inlineMembers += InlineMemberDependency(artifact.coordinates, capabilities.inlineMembers)
  //       }
  //       if (capabilities.typealiases.isNotEmpty()) {
  //         typealiases += TypealiasDependency(artifact.coordinates, capabilities.typealiases)
  //       }
  //     }
  //
  //   // return artifacts.asSequence()
  //   //   .filter {
  //   //     it.isJar() || it.containsClassFiles()
  //   //   }.map { artifact ->
  //   //     artifact to findKotlinMagic(artifact, artifact.mode)
  //   //   }.map { (artifact, capabilities) ->
  //   //     InlineMemberDependency(artifact.coordinates, inlineMembers)
  //   //   }.toSortedSet()
  // }

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
  private fun findKotlinMagic(artifact: PhysicalArtifact, mode: Mode): KotlinCapabilities {
    val cached = findInCache(artifact)
    if (cached != null) return cached

    fun packageName(fileLike: String): String {
      return if (fileLike.contains('/')) {
        // entry is in a package
        fileLike.substringBeforeLast('/').replace('/', '.')
      } else {
        // entry is in root; no package
        ""
      }
    }

    val inlineMembers = mutableSetOf<InlineMemberCapability.InlineMember>()
    val typealiases = mutableSetOf<TypealiasCapability.Typealias>()

    when (mode) {
      Mode.ZIP -> {
        val zipFile = ZipFile(artifact.file)
        val entries = zipFile.entries().toList()
        // Only look at jars that have actual Kotlin classes in them
        if (entries.none { it.name.endsWith(".kotlin_module") }) {
          return KotlinCapabilities.EMPTY
        }

        entries.asSequenceOfClassFiles()
          .mapNotNull { entry ->
            // TODO an entry with `META-INF/proguard/androidx-annotations.pro`
            val kotlinMagic = readClass(
              zipFile.getInputStream(entry).use { ClassReader(it.readBytes()) },
              entry.toString()
            ) ?: return@mapNotNull null

            entry to kotlinMagic
          }
          .forEach { (entry, kotlinMagic) ->
            if (kotlinMagic.inlineMembers != null) {
              inlineMembers += InlineMemberCapability.InlineMember(
                packageName = packageName(entry.name),
                // Guaranteed to be non-empty
                inlineMembers = kotlinMagic.inlineMembers
              )
            }

            if (kotlinMagic.typealiases != null) {
              typealiases += TypealiasCapability.Typealias(
                packageName = packageName(entry.name),
                typealiases = kotlinMagic.typealiases
              )
            }
          }
      }

      Mode.CLASSES -> {
        if (KtFile.fromDirectory(artifact.file).isEmpty()) {
          return KotlinCapabilities.EMPTY
        }

        artifact.file.walkBottomUp()
          .filter { it.isFile && it.name.endsWith(".class") }
          .mapNotNull { classFile ->
            val kotlinMagic = readClass(
              classFile.inputStream().use { ClassReader(it.readBytes()) },
              classFile.toString()
            ) ?: return@mapNotNull null

            classFile to kotlinMagic
          }
          .forEach { (classFile, kotlinMagic) ->
            if (kotlinMagic.inlineMembers != null) {
              inlineMembers += InlineMemberCapability.InlineMember(
                packageName = packageName(Files.asPackagePath(classFile)),
                // Guaranteed to be non-empty
                inlineMembers = kotlinMagic.inlineMembers
              )
            }

            if (kotlinMagic.typealiases != null) {
              typealiases += TypealiasCapability.Typealias(
                packageName = packageName(Files.asPackagePath(classFile)),
                typealiases = kotlinMagic.typealiases
              )
            }
          }
      }
    }

    val kotlinCapabilities = KotlinCapabilities(inlineMembers, typealiases)

    // cache
    putInCache(artifact, kotlinCapabilities)

    return kotlinCapabilities
  }

  private fun findInCache(artifact: PhysicalArtifact): KotlinCapabilities? {
    return inMemoryCache.kotlinCapabilities(artifact.file.absolutePath)
  }

  private fun putInCache(artifact: PhysicalArtifact, capabilities: KotlinCapabilities) {
    inMemoryCache.inlineMembers(artifact.file.absolutePath, capabilities)
  }

  /** Returned set is either null or non-empty. */
  private fun readClass(classReader: ClassReader, classFile: String): KotlinMagic? {
    val metadataVisitor = KotlinMetadataVisitor(logger)
    classReader.accept(metadataVisitor, 0)

    var inlineMembers: Set<String>? = null
    var typealiases: Set<TypealiasCapability.Typealias.Alias>? = null

    metadataVisitor.builder?.let { header ->
      // Can throw `kotlinx.metadata.InconsistentKotlinMetadataException`, which is unfortunately `internal` to its
      // module. It extends `IllegalArgumentException`, so we catch that. This can happen if we attempt to read a class
      // file compiled by a "very old" version of Kotlin.
      // See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1035
      // See https://youtrack.jetbrains.com/issue/KT-60870
      val metadata = try {
        KotlinClassMetadata.readLenient(header.build())
      } catch (e: IllegalArgumentException) {
        logger.debug("Can't read class file '$classFile'")
        errorsReport.appendText("Can't read class file '$classFile'\n")
        didWriteErrors = true
        return null
      }

      when (metadata) {
        is KotlinClassMetadata.Class -> {
          inlineMembers = inlineMembers(metadata.kmClass)
          typealiases = typealiases(metadata.kmClass)
        }

        is KotlinClassMetadata.FileFacade -> {
          inlineMembers = inlineMembers(metadata.kmPackage)
          typealiases = typealiases(metadata.kmPackage)
        }

        is KotlinClassMetadata.MultiFileClassPart -> {
          inlineMembers = inlineMembers(metadata.kmPackage)
          typealiases = typealiases(metadata.kmPackage)
        }

        is KotlinClassMetadata.SyntheticClass -> logger.debug("Ignoring SyntheticClass $classFile")
        is KotlinClassMetadata.MultiFileClassFacade -> logger.debug("Ignoring MultiFileClassFacade $classFile")
        is KotlinClassMetadata.Unknown -> logger.debug("Ignoring Unknown $classFile")
      }
    } ?: return null

    // It's part of the contract to never return an empty set
    return KotlinMagic(
      inlineMembers = inlineMembers?.ifEmpty { null },
      typealiases = typealiases?.ifEmpty { null },
    )

    // It's part of the contract to never return an empty set
    // return inlineMembers?.ifEmpty { null }
  }

  private class KotlinMagic(
    val inlineMembers: Set<String>?,
    val typealiases: Set<TypealiasCapability.Typealias.Alias>?,
  )

  private fun inlineMembers(kmDeclaration: KmDeclarationContainer): Set<String> {
    fun inlineFunctions(functions: List<KmFunction>): Sequence<String> {
      return functions.asSequence()
        .filter { it.isInline }
        .map { it.name }
    }

    fun inlineProperties(properties: List<KmProperty>): Sequence<String> {
      return properties.asSequence()
        .filter { it.getter.isInline }
        .map { it.name }
    }

    return (inlineFunctions(kmDeclaration.functions) + inlineProperties(kmDeclaration.properties)).toSortedSet()
  }

  private fun typealiases(kmDeclaration: KmDeclarationContainer): Set<TypealiasCapability.Typealias.Alias> {
    fun KmType.name(): String {
      // classifier is variable, so we can't smartcast in the when statement without something like this
      return classifier.run {
        when (this) {
          is KmClassifier.Class -> name
          is KmClassifier.TypeAlias -> name
          is KmClassifier.TypeParameter -> id.toString()
        }
      }
    }

    return kmDeclaration.typeAliases.mapToOrderedSet {
      TypealiasCapability.Typealias.Alias(it.name, it.expandedType.name())
    }
  }
}

internal class KotlinCapabilities(
  val inlineMembers: Set<InlineMemberCapability.InlineMember>,
  val typealiases: Set<TypealiasCapability.Typealias>,
) {
  companion object {
    val EMPTY = KotlinCapabilities(emptySet(), emptySet())
  }
}
