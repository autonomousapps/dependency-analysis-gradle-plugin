// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.KotlinMetadataVisitor
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.Files
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.TypealiasCapability
import com.autonomousapps.tasks.FindKotlinMagicTask
import com.autonomousapps.tasks.KotlinCapabilities
import java.io.File
import java.util.zip.ZipFile
import kotlin.metadata.*
import kotlin.metadata.jvm.KotlinClassMetadata

internal class KotlinMagicFinder(
  artifacts: List<PhysicalArtifact>,
  private val errorsReport: File,
) {

  private val logger = getLogger<FindKotlinMagicTask>()
  var didWriteErrors = false

  private val _newEntries = linkedMapOf<String, KotlinCapabilities>()

  /** [KotlinCapabilities] computed during this run (cache misses), keyed by artifact path, to merge into the cache. */
  val newEntries: Map<String, KotlinCapabilities> get() = _newEntries

  init {
    artifacts.asSequence()
      .forEach { artifact ->
        val key = artifact.cacheKey()
        val capabilities = findKotlinMagic(artifact, artifact.mode)

        // The point of this class
        _newEntries[key] = capabilities
      }
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
  private fun findKotlinMagic(artifact: PhysicalArtifact, mode: PhysicalArtifact.Mode): KotlinCapabilities {
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
      PhysicalArtifact.Mode.ZIP -> {
        ZipFile(artifact.jarFile()).use { zipFile ->
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

      PhysicalArtifact.Mode.CLASSES -> {
        if (KtFile.fromDirectories(artifact.files).isEmpty()) {
          return KotlinCapabilities.EMPTY
        }

        artifact.classFiles()
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
