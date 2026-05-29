// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.KotlinMetadataVisitor
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.PhysicalArtifact.Mode
import com.autonomousapps.model.internal.TypealiasCapability
import com.autonomousapps.model.internal.intermediates.producer.InlineMemberDependency
import com.autonomousapps.model.internal.intermediates.producer.TypealiasDependency
import com.autonomousapps.services.InMemoryCache
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
import kotlin.metadata.*
import kotlin.metadata.jvm.KotlinClassMetadata

@CacheableTask
public abstract class FindKotlinMagicTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Produces a report of dependencies that contribute used inline members"
  }

  @get:Internal
  public abstract val inMemoryCacheProvider: Property<InMemoryCache>

  /** Not used by the task action, but necessary for correct input-output tracking, for reasons I do not recall. */
  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  /** [PhysicalArtifact]s used to compile this project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val artifacts: RegularFileProperty

  /** Inline members in this project's dependencies. */
  @get:OutputFile
  public abstract val outputInlineMembers: RegularFileProperty

  /** typealiases in this project's dependencies. */
  @get:OutputFile
  public abstract val outputTypealiases: RegularFileProperty

  /**
   * Errors analyzing class files.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1035">Issue 1035</a>
   * @see <a href="https://youtrack.jetbrains.com/issue/KT-60870">KT-60870</a>
   */
  @get:OutputFile
  public abstract val outputErrors: RegularFileProperty

  @TaskAction
  public fun action() {
    // Pass the shared cache content to the work action, which requires serializable data only
    val cache = inMemoryCacheProvider.get()
    val seed = artifacts.fromJsonList<PhysicalArtifact>()
      .mapNotNull { artifact ->
        val key = artifact.file.absolutePath
        cache.kotlinCapabilities(key)?.let { key to it }
      }
      .toMap()

    val seedFile = File(temporaryDir, "kotlin-magic-cache-seed.json").apply { bufferWriteJsonMap(seed) }
    val newEntriesFile = File(temporaryDir, "kotlin-magic-cache-new.json")

    workerExecutor.noIsolation().submit(Action::class.java) {
      it.artifacts.set(artifacts)
      it.inlineUsageReport.set(outputInlineMembers)
      it.typealiasReport.set(outputTypealiases)
      it.errorsReport.set(outputErrors)
      it.cacheSeed.set(seedFile)
      it.newCacheEntries.set(newEntriesFile)
    }

    // Block so we can merge the worker's results back into the shared cache.
    workerExecutor.await()
    newEntriesFile.fromJsonMap<String, KotlinCapabilities>().forEach { (key, capabilities) ->
      cache.inlineMembers(key, capabilities)
    }
  }

  public interface Parameters : WorkParameters {
    public val artifacts: RegularFileProperty
    public val inlineUsageReport: RegularFileProperty
    public val typealiasReport: RegularFileProperty
    public val errorsReport: RegularFileProperty

    /** [`Map<String, KotlinCapabilities>`][KotlinCapabilities] of already-cached results, keyed by artifact path. */
    public val cacheSeed: RegularFileProperty

    /** [`Map<String, KotlinCapabilities>`][KotlinCapabilities] of cache misses, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<FindKotlinMagicTask>()

    override fun execute() {
      val inlineUsageReportFile = parameters.inlineUsageReport.getAndDelete()
      val typealiasReportFile = parameters.typealiasReport.getAndDelete()
      val errorsReport = parameters.errorsReport.getAndDelete()

      val finder = KotlinMagicFinder(
        seedCache = parameters.cacheSeed.fromJsonMap(),
        artifacts = parameters.artifacts.fromJsonList<PhysicalArtifact>(),
        errorsReport = errorsReport,
      )
      val inlineMembers = finder.inlineMembers
      val typealiases = finder.typealiases

      inlineUsageReportFile.bufferWriteJsonSet(inlineMembers)
      typealiasReportFile.bufferWriteJsonSet(typealiases)

      parameters.newCacheEntries.getAndDelete().bufferWriteJsonMap(finder.newEntries)

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
  private val seedCache: Map<String, KotlinCapabilities>,
  artifacts: List<PhysicalArtifact>,
  private val errorsReport: File,
) {

  private val logger = getLogger<FindKotlinMagicTask>()
  var didWriteErrors = false

  val inlineMembers: Set<InlineMemberDependency>
  val typealiases: Set<TypealiasDependency>

  /** [KotlinCapabilities] computed during this run (cache misses), keyed by artifact path, to merge into the cache. */
  val newEntries: MutableMap<String, KotlinCapabilities> = LinkedHashMap()

  init {
    val inlineMembersMut = mutableSetOf<InlineMemberDependency>()
    val typealiasesMut = mutableSetOf<TypealiasDependency>()

    artifacts.asSequence()
      .filter {
        it.isJar() || it.containsClassFiles()
      }.map { artifact ->
        val key = artifact.file.absolutePath
        val capabilities = seedCache[key] ?: findKotlinMagic(artifact, artifact.mode).also { newEntries[key] = it }
        artifact to capabilities
      }.forEach { (artifact, capabilities) ->
        if (capabilities.inlineMembers.isNotEmpty()) {
          inlineMembersMut += InlineMemberDependency.newInstance(artifact.coordinates, capabilities.inlineMembers)
        }
        if (capabilities.typealiases.isNotEmpty()) {
          typealiasesMut += TypealiasDependency.newInstance(artifact.coordinates, capabilities.typealiases)
        }
      }

    inlineMembers = inlineMembersMut
    typealiases = typealiasesMut
  }

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
   *
   * TODO(tsr): docs for the TypeAliasCapability portion of this.
   */
  private fun findKotlinMagic(artifact: PhysicalArtifact, mode: Mode): KotlinCapabilities {
    fun packageName(fileLike: String): String {
      return if (fileLike.contains('/')) {
        // entry is in a package
        fileLike.substringBeforeLast('/').replace('/', '.')
      } else {
        // entry is in root; no package
        ""
      }
    }

    // com/foo/BarKt.class -> com.foo.BarKt
    fun className(entryName: String): String {
      return entryName.replace('/', '.').substringBeforeLast(".class")
    }

    val inlineMembers = mutableSetOf<InlineMemberCapability.InlineMember>()
    val typealiases = mutableSetOf<TypealiasCapability.Typealias>()

    when (mode) {
      Mode.ZIP -> {
        ZipFile(artifact.file).use { zipFile ->
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
                inlineMembers += InlineMemberCapability.InlineMember.newInstance(
                  className = className(entry.name),
                  packageName = packageName(entry.name),
                  // Guaranteed to be non-empty
                  inlineMembers = kotlinMagic.inlineMembers
                )
              }

              if (kotlinMagic.typealiases != null) {
                typealiases += TypealiasCapability.Typealias.newInstance(
                  packageName = packageName(entry.name),
                  alternatePackageName = kotlinMagic.packageName,
                  typealiases = kotlinMagic.typealiases
                )
              }
            }
        }
      }

      Mode.CLASSES -> {
        if (KtFile.fromDirectory(artifact.file).isEmpty()) {
          return KotlinCapabilities.EMPTY
        }

        artifact.file.asSequenceOfClassFiles()
          .mapNotNull { classFile ->
            val kotlinMagic = readClass(
              classFile.inputStream().use { ClassReader(it.readBytes()) },
              classFile.toString()
            ) ?: return@mapNotNull null

            classFile to kotlinMagic
          }
          .forEach { (classFile, kotlinMagic) ->
            if (kotlinMagic.inlineMembers != null) {
              val packageName = packageName(Files.asPackagePath(classFile))
              val className = packageName + classFile.name.substringBeforeLast(".class")

              inlineMembers += InlineMemberCapability.InlineMember.newInstance(
                className = className,
                packageName = packageName,
                // Guaranteed to be non-empty
                inlineMembers = kotlinMagic.inlineMembers
              )
            }

            if (kotlinMagic.typealiases != null) {
              typealiases += TypealiasCapability.Typealias.newInstance(
                packageName = packageName(Files.asPackagePath(classFile)),
                alternatePackageName = kotlinMagic.packageName,
                typealiases = kotlinMagic.typealiases
              )
            }
          }
      }
    }

    return KotlinCapabilities(inlineMembers, typealiases)
  }

  /** Returned set is either null or non-empty. */
  private fun readClass(classReader: ClassReader, classFile: String): KotlinMagic? {
    val metadataVisitor = KotlinMetadataVisitor(logger)
    classReader.accept(metadataVisitor, 0)

    var packageName: String = ""
    var inlineMembers: Set<String>? = null
    var typealiases: Set<TypealiasCapability.Typealias.Alias>? = null

    metadataVisitor.builder?.let { header ->
      // Can throw `kotlinx.metadata.InconsistentKotlinMetadataException`, which is unfortunately `internal` to its
      // module. It extends `IllegalArgumentException`, so we catch that. This can happen if we attempt to read a class
      // file compiled by a "very old" version of Kotlin.
      // See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1035
      // See https://youtrack.jetbrains.com/issue/KT-60870
      val kotlinClassMetadata = try {
        val metadata = header.build()
        packageName = metadata.packageName // can be empty

        KotlinClassMetadata.readLenient(metadata)
      } catch (_: IllegalArgumentException) {
        logger.debug("Can't read class file '$classFile'")
        errorsReport.appendText("Can't read class file '$classFile'\n")
        didWriteErrors = true
        return null
      }

      when (kotlinClassMetadata) {
        is KotlinClassMetadata.Class -> {
          inlineMembers = inlineMembers(kotlinClassMetadata.kmClass)
          typealiases = typealiases(kotlinClassMetadata.kmClass)
        }

        is KotlinClassMetadata.FileFacade -> {
          inlineMembers = inlineMembers(kotlinClassMetadata.kmPackage)
          typealiases = typealiases(kotlinClassMetadata.kmPackage)
        }

        is KotlinClassMetadata.MultiFileClassPart -> {
          inlineMembers = inlineMembers(kotlinClassMetadata.kmPackage)
          typealiases = typealiases(kotlinClassMetadata.kmPackage)
        }

        is KotlinClassMetadata.SyntheticClass -> logger.debug("Ignoring SyntheticClass $classFile")
        is KotlinClassMetadata.MultiFileClassFacade -> logger.debug("Ignoring MultiFileClassFacade $classFile")
        is KotlinClassMetadata.Unknown -> logger.debug("Ignoring Unknown $classFile")
      }
    } ?: return null

    // It's part of the contract to never return an empty set
    return KotlinMagic(
      packageName = packageName,
      inlineMembers = inlineMembers?.ifEmpty { null },
      typealiases = typealiases?.ifEmpty { null },
    )
  }

  private class KotlinMagic(
    val packageName: String,
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
